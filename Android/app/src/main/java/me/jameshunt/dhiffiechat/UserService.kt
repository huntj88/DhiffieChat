package me.jameshunt.dhiffiechat

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.crypto.toBase64String

class UserService(
    private val aliasQueries: AliasQueries,
    private val networkHelper: NetworkHelper,
    private val api: DhiffieChatApi,
    private val authManager: AuthManager,
    private val identityManager: IdentityManager
) {

    fun getFriends(): Flow<List<Alias>> {
        return aliasQueries.getAliases().asFlow().mapToList().map { aliases ->
            val friends = api.getUserRelationships(networkHelper.standardHeaders()).friends
            aliases.filter { it.userId in friends }
        }
    }

    suspend fun getRelationships(): DhiffieChatApi.Relationships {
        return api.getUserRelationships(networkHelper.standardHeaders())
    }

    suspend fun addFriend(userId: String, alias: String) {
        aliasQueries.addAlias(userId, alias)
        api.scanQR(
            headers = networkHelper.standardHeaders(),
            qr = DhiffieChatApi.QR(scannedUserId = userId)
        )
    }

    suspend fun getMessageSummaries(): List<DhiffieChatApi.MessageFromUserSummary> {
        return api.getMessageSummaries(networkHelper.standardHeaders())
    }

    suspend fun createIdentity(): DhiffieChatApi.ResponseMessage {
        val userToServerCredentials = authManager.userToServerAuth(serverPublicKey = networkHelper.getServerPublicKey())

        return api.createIdentity(
            DhiffieChatApi.CreateIdentity(
                publicKey = identityManager.getIdentity().public.toBase64String(),
                iv = userToServerCredentials.iv.toBase64String(),
                encryptedToken = userToServerCredentials.encryptedToken
            )
        )
    }
}
