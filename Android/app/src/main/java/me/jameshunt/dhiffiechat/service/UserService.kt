package me.jameshunt.dhiffiechat.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.sqldelight.runtime.rx3.asObservable
import com.squareup.sqldelight.runtime.rx3.mapToList
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import me.jameshunt.dhiffiechat.Alias
import me.jameshunt.dhiffiechat.AliasQueries
import me.jameshunt.dhiffiechat.crypto.toRSAPublicKey
import me.jameshunt.dhiffiechat.crypto.toUserId
import java.security.PublicKey
import java.util.*

class UserService(
    private val aliasQueries: AliasQueries,
    private val api: LambdaApi,
    private val serverAuthManager: ServerAuthManager,
    private val identityManager: IdentityManager,
    private val prefManager: PrefManager
) {

    fun getAlias(): Flowable<Optional<Alias>> {
        val userId = identityManager.getIdentity().toUserId()
        return aliasQueries.getAliases().asObservable().mapToList().map { aliases ->
            Optional.ofNullable(aliases.firstOrNull { it.userId == userId })
        }.toFlowable(BackpressureStrategy.LATEST)
    }

    fun setAlias(alias: String) {
        val userId = identityManager.getIdentity().toUserId()
        aliasQueries.addAlias(userId, alias)
        prefManager.userProfileConfigured()
    }

    fun getFriends(): Observable<List<Alias>> {
        return aliasQueries.getAliases().asObservable().mapToList().flatMapSingle { aliases ->
            getRelationships().map { it.friends }.map { friends ->
                aliases.filter { it.userId in friends }
            }
        }
    }

    fun getRelationships(): Single<LambdaApi.UserRelationships> {
        return api.getUserRelationships()
    }

    fun addFriend(userId: String, alias: String): Single<Unit> {
        aliasQueries.addAlias(userId, alias)
        return api.scanQR(body = LambdaApi.ScanQR(scannedUserId = userId)).map { Unit }
    }

    fun createIdentity(): Single<Unit> {
        val userToServerCredentials = serverAuthManager.userToServerAuth()

        return getFcmToken().flatMap { fcmToken ->
            api.createIdentity(
                body = LambdaApi.CreateIdentity(
                    publicKey = identityManager.getIdentity().public,
                    credentials = userToServerCredentials,
                    fcmToken = fcmToken
                )
            )
        }.map { Unit }
    }

    private fun getFcmToken(): Single<String> = Single.create<String> { cont ->
        val tokenHandle = FirebaseMessaging.getInstance().token
        try {
            cont.onSuccess(tokenHandle.result)
        } catch (e: IllegalStateException) {
            tokenHandle.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("UserService", "Fetching FCM registration token failed", task.exception)
                    cont.onError(task.exception!!)
                }

                // Get new FCM registration token
                val token = task.result
                cont.onSuccess(token)
            }
        }
    }

    fun getUserPublicKey(userId: String): Single<PublicKey> {
        fun validate(publicKey: PublicKey): Single<PublicKey> {
            val userIdFromPublic = publicKey.toUserId()
            return getFriends()
                .firstOrError()
                .map { it.map { it.userId } }
                .map { friends ->
                    // maintain a list of your friends locally to validate against (MiTM), if not in list then abort.
                    val isLocalFriend = friends.contains(userIdFromPublic)

                    // verify that public key given matches whats expected
                    val isValid = isLocalFriend && userId == userIdFromPublic
                    if (!isValid) {
                        throw IllegalStateException("Incorrect public key given for user: $userId")
                    }
                }
                .map { publicKey }
        }

        return api
            .getUserPublicKey(body = LambdaApi.GetUserPublicKey(userId = userId))
            .map { it.publicKey.toRSAPublicKey() }
            .flatMap { validate(it) }
    }

    fun isUserProfileSetup(): Boolean {
        return prefManager.isUserProfileSetup()
    }
}
