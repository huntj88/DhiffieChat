package me.jameshunt.dhiffiechat.service

import com.squareup.moshi.Moshi
import me.jameshunt.dhiffiechat.crypto.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class ServerAuthManager(
    private val identityManager: IdentityManager,
    private val moshi: Moshi
) {
    private data class Token(
        val userId: String,
        val type: String = "Authentication",
        val expires: Instant
    )

    data class ServerCredentials(
        val token: String,
        val signature: String
    )

    private fun Token.toSerialized(): String = moshi
        .adapter(Token::class.java)
        .toJson(this)
        .toByteArray()
        .toBase64String()

    fun userToServerAuth(): ServerCredentials {
        val token = Token(
            userId = identityManager.getIdentity().toUserId(),
            expires = Instant.now().plus(1L, ChronoUnit.MINUTES)
        ).toSerialized()

        return ServerCredentials(
            token = token,
            signature =  RSACrypto.sign(
                privateKey = identityManager.getIdentity().private,
                base64 = token
            )
        )
    }
}
