package me.jameshunt.dhiffiechat

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RelationshipService(
    private val aliasQueries: AliasQueries,
    private val dhiffieChatService: DhiffieChatService
) {

    fun getFriends(): Flow<List<Alias>> {
        return aliasQueries.getAliases().asFlow().mapToList().map { aliases ->
            val friends = dhiffieChatService.getUserRelationships().friends
            aliases.filter { it.userId in friends }
        }
    }

    suspend fun addFriend(userId: String, alias: String) {
        aliasQueries.addAlias(userId, alias)
        dhiffieChatService.scanQR(userId)
    }
}
