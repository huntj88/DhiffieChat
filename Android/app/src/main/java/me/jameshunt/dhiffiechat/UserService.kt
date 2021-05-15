package me.jameshunt.dhiffiechat

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.RequestType.*
import me.jameshunt.dhiffiechat.crypto.toBase64String

class UserService(
    private val aliasQueries: AliasQueries,
    private val networkHelper: NetworkHelper,
    private val api: SingleEndpointApi,
    private val authManager: AuthManager,
    private val identityManager: IdentityManager
) {

    fun getFriends(): Flow<List<Alias>> {
        return aliasQueries.getAliases().asFlow().mapToList().map { aliases ->
            val friends = getRelationships().friends
            aliases.filter { it.userId in friends }
        }
    }

    suspend fun getRelationships(): GetUserRelationships.Response {
        return api.getUserRelationships()
    }

    suspend fun addFriend(userId: String, alias: String) {
        aliasQueries.addAlias(userId, alias)
        api.scanQR(ScanQR(scannedUserId = userId))
    }

    suspend fun getMessageSummaries(): List<GetMessageSummaries.MessageSummary> {
        return api.getMessageSummaries()
    }

    suspend fun createIdentity() {
        val userToServerCredentials =
            authManager.userToServerAuth(serverPublicKey = networkHelper.getServerPublicKey())

        api.createIdentity(
            CreateIdentity(
                publicKey = identityManager.getIdentity().public.toBase64String(),
                iv = userToServerCredentials.iv.toBase64String(),
                encryptedToken = userToServerCredentials.encryptedToken
            )
        )
    }
}
