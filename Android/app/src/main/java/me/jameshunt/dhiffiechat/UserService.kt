package me.jameshunt.dhiffiechat

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.LambdaApi.*
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

    suspend fun createIdentity() {
        val userToServerCredentials = authManager.userToServerAuth()

        api.createIdentity(
            body = CreateIdentity(
                publicKey = identityManager.getIdentity().public,
                iv = userToServerCredentials.iv,
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
}
