package me.jameshunt.dhiffiechat.service

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.Alias
import me.jameshunt.dhiffiechat.AliasQueries
import me.jameshunt.dhiffiechat.crypto.AESCrypto
import me.jameshunt.dhiffiechat.crypto.base64ToByteArray
import me.jameshunt.dhiffiechat.crypto.toPublicKey
import me.jameshunt.dhiffiechat.crypto.toUserId
import me.jameshunt.dhiffiechat.service.LambdaApi.*
import java.security.PublicKey
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class UserService(
    private val aliasQueries: AliasQueries,
    private val api: LambdaApi,
    private val authManager: AuthManager,
    private val identityManager: IdentityManager,
    private val prefManager: PrefManager
) {

    fun getAlias(): Alias? {
        val userId = identityManager.getIdentity().toUserId()
        return aliasQueries.getAliases().executeAsList().firstOrNull { it.userId == userId }
    }

    fun setAlias(alias: String) {
        val userId = identityManager.getIdentity().toUserId()
        aliasQueries.addAlias(userId, alias)
        prefManager.userProfileConfigured()
    }

    fun getFriends(): Flow<List<Alias>> {
        return aliasQueries.getAliases().asFlow().mapToList().map { aliases ->
            val friends = getRelationships().friends
            aliases.filter { it.userId in friends }
        }
    }

    suspend fun getRelationships(): UserRelationships {
        return api.getUserRelationships()
    }

    suspend fun addFriend(userId: String, alias: String) {
        aliasQueries.addAlias(userId, alias)
        api.scanQR(body = ScanQR(scannedUserId = userId))
    }

    suspend fun getMessageSummaries(): List<MessageSummary> {
        return api.getMessageSummaries()
    }

    suspend fun decryptMessageText(message: Message): Message {
        val otherUserPublicKey = getUserPublicKey(userId = message.from)
        val sharedSecret = authManager.userToUserMessage(otherUserPublicKey).sharedSecret

        val decryptedText = message.text
            ?.base64ToByteArray()
            ?.let { AESCrypto.decrypt(it, sharedSecret) }
            ?.toString(Charsets.UTF_8)

        return message.copy(text = decryptedText)
    }

    suspend fun createIdentity() {
        val userToServerCredentials = authManager.userToServerAuth()

        api.createIdentity(
            body = CreateIdentity(
                publicKey = identityManager.getIdentity().public,
                encryptedToken = userToServerCredentials.encryptedToken,
                fcmToken = getFcmToken()
            )
        )
    }

    private suspend fun getFcmToken(): String = suspendCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("UserService", "Fetching FCM registration token failed", task.exception)
                cont.resumeWithException(task.exception!!)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            cont.resume(token)
        })
    }

    suspend fun getUserPublicKey(userId: String): PublicKey {
        return api
            .getUserPublicKey(body = LambdaApi.GetUserPublicKey(userId = userId))
            .publicKey
            .toPublicKey()
            .also { publicKey ->
                val userIdFromPublic = publicKey.toUserId()

                // maintain a list of your friends locally to validate against (MiTM), if not in list then abort.
                val friends = getFriends().firstOrNull()?.map { it.userId }
                val isLocalFriend = friends?.contains(userIdFromPublic) ?: false

                // verify that public key given matches whats expected
                val isValid = isLocalFriend && userId == userIdFromPublic
                if (!isValid) {
                    throw IllegalStateException("Incorrect public key given for user: $userId")
                }
            }
    }

    fun isUserProfileSetup(): Boolean {
        return prefManager.isUserProfileSetup()
    }
}
