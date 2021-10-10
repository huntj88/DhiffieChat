package me.jameshunt.dhiffiechat.message

import com.amazonaws.services.dynamodbv2.document.Item
import com.fasterxml.jackson.annotation.JsonIgnore
import me.jameshunt.dhiffiechat.format
import java.net.URL
import java.time.Instant

data class Message(
    val to: String,
    val from: String,
    val messageCreatedAt: Instant,
    val text: String?,
    val fileKey: String,
    val mediaType: String,
    val uploadFinished: Boolean,
    val signedS3Url: URL?,
    val signedS3UrlExpiration: Instant?,
    val ephemeralPublicKey: String,
    val sendingPublicKey: String,
    val sendingPublicKeySignature: String,
    @JsonIgnore
    val expiresAt: Instant // used for amazon dynamoDB automatic item expiration
)

fun Item.toMessage(): Message {
    return Message(
        to = this.getString("to"),
        from = this.getString("from"),
        messageCreatedAt = Instant.parse(this.getString("messageCreatedAt")),
        text = this.getString("text"),
        fileKey = this.getString("fileKey"),
        mediaType = this.getString("mediaType"),
        uploadFinished = this.getBoolean("uploadFinished"),
        signedS3Url = this.getString("signedS3Url")?.let { URL(it) },
        signedS3UrlExpiration = this.getString("signedS3UrlExpiration")?.let { Instant.parse(it) },
        ephemeralPublicKey = this.getString("ephemeralPublicKey"),
        sendingPublicKey = this.getString("sendingPublicKey"),
        sendingPublicKeySignature = this.getString("sendingPublicKeySignature"),
        expiresAt = Instant.ofEpochSecond(this.getLong("expiresAt"))
    )
}

fun Message.toItem(): Item {
    val map = mapOf(
        "to" to to,
        "from" to from,
        "messageCreatedAt" to messageCreatedAt.format(),
        "text" to text,
        "fileKey" to fileKey,
        "mediaType" to mediaType,
        "uploadFinished" to uploadFinished,
        "signedS3Url" to signedS3Url?.toString(),
        "signedS3UrlExpiration" to signedS3UrlExpiration?.format(),
        "ephemeralPublicKey" to ephemeralPublicKey,
        "sendingPublicKey" to sendingPublicKey,
        "sendingPublicKeySignature" to sendingPublicKeySignature,
        "expiresAt" to expiresAt.epochSecond
    )

    return Item.fromMap(map)
}