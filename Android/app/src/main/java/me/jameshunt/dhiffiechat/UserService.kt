package me.jameshunt.dhiffiechat

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.jameshunt.dhiffiechat.LambdaApi.*

class UserService(
    private val aliasQueries: AliasQueries,
    private val api: LambdaApi,
    private val authManager: AuthManager,
    private val identityManager: IdentityManager
) {

    fun getFriends(): Flow<List<Alias>> {
        return aliasQueries.getAliases().asFlow().mapToList().map { aliases ->
            val friends = getRelationships().friends
            aliases.filter { it.userId in friends }
        }
    }

    suspend fun getRelationships(): GetUserRelationshipsResponse {
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
                encryptedToken = userToServerCredentials.encryptedToken
            )
        )
    }
}
