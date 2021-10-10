package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import me.jameshunt.dhiffiechat.Encryption_keyQueries
import me.jameshunt.dhiffiechat.HandledException
import me.jameshunt.dhiffiechat.crypto.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.crypto.SecretKey

class MessageService(
    private val identityManager: IdentityManager,
    private val okHttpClient: OkHttpClient,
    private val api: LambdaApi,
    private val userService: UserService,
    private val fileLocationUtil: FileLocationUtil,
    private val encryptionKeyQueries: Encryption_keyQueries
) {

    fun getMessageSummaries(): Single<List<LambdaApi.MessageSummary>> {
        return api.getMessageSummaries()
    }

    fun sendMessage(
        recipientUserId: String,
        text: String?,
        file: File,
        mediaType: MediaType
    ): Single<Unit> {
        val ephemeral = api.getEphemeralPublicKey(
            body = LambdaApi.EphemeralPublicKeyRequest(recipientUserId)
        )

        return userService
            .getUserPublicKey(recipientUserId)
            .zipWith(ephemeral)
            .map { (otherUsersPublicKey, ephemeral) ->
                val ephemeralCameFromCorrectUser = RSACrypto.canVerify(
                    ephemeral.publicKey.toBase64String(),
                    ephemeral.signature,
                    otherUsersPublicKey
                )
                if (ephemeralCameFromCorrectUser) {
                    ephemeral.publicKey
                } else {
                    throw HandledException.InvalidSignature
                }
            }
            .flatMap { publicKey ->
                val sendingKeyPair = DHCrypto.genDHKeyPair()
                val secret = DHCrypto.agreeSecretKey(sendingKeyPair.private, publicKey)

                val output = fileLocationUtil.outgoingEncryptedFile()

                AESCrypto.encrypt(file = file, output = output, key = secret)

                val encryptedText = text
                    ?.let { AESCrypto.encrypt(text.toByteArray(), secret) }
                    ?.toBase64String()

                val body = LambdaApi.SendMessage(
                    recipientUserId = recipientUserId,
                    text = encryptedText,
                    s3Key = output.toS3Key(),
                    mediaType = mediaType,
                    ephemeralPublicKey = publicKey,
                    signedSendingPublicKey = LambdaApi.SignedKey(
                        publicKey = sendingKeyPair.public,
                        signature = RSACrypto.sign(
                            sendingKeyPair.public.toBase64String(),
                            identityManager.getIdentity().private
                        )
                    )
                )

                api.sendMessage(body = body)
                    .flatMap { response -> upload(response.uploadUrl, output) }
                    .doAfterSuccess {
                        file.delete()
                        output.delete()
                    }
            }
    }

    fun decryptMessage(message: LambdaApi.Message): Single<Pair<LambdaApi.Message, File>> {
        return getSharedSecretForDecryption(message)
            .flatMap { sharedSecret ->
                val decryptedMessage = decryptMessageText(message, sharedSecret)
                getDecryptedFile(message, sharedSecret).map { decryptedMessage to it }
            }
            .doOnSuccess {
                val ephemeralPublicKey = message.ephemeralPublicKey.toBase64String()
                encryptionKeyQueries.deleteEphemeral(publicKey = ephemeralPublicKey)
            }
    }

    private fun decryptMessageText(
        message: LambdaApi.Message,
        sharedSecret: SecretKey
    ): LambdaApi.Message {
        val decryptedText = message.text
            ?.base64ToByteArray()
            ?.let { AESCrypto.decrypt(it, sharedSecret) }
            ?.toString(Charsets.UTF_8)

        return message.copy(text = decryptedText)
    }

    private fun getDecryptedFile(
        message: LambdaApi.Message,
        sharedSecret: SecretKey
    ): Single<File> {
        return api
            .consumeMessage(body = LambdaApi.ConsumeMessage(message.messageCreatedAt))
            .flatMap { download(it.s3Url) }
            .observeOn(Schedulers.computation())
            .map {
                AESCrypto.decrypt(
                    inputStream = it,
                    output = fileLocationUtil.incomingDecryptedFile(),
                    key = sharedSecret
                )
            }
            .map { fileLocationUtil.incomingDecryptedFile() }
    }

    private fun getSharedSecretForDecryption(message: LambdaApi.Message): Single<SecretKey> {
        val privateEphemeral = encryptionKeyQueries
            .selectPrivate(publicKey = message.ephemeralPublicKey.toBase64String())
            .executeAsOne()
            .toDHPrivateKey()

        return userService.getUserPublicKey(userId = message.from)
            .map { otherUsersPublicKey ->
                val sendingEphemeral = message.sendingPublicKey
                val ephemeralCameFromCorrectUser = RSACrypto.canVerify(
                    base64 = sendingEphemeral.toBase64String(),
                    signatureBase64 = message.sendingPublicKeySignature,
                    publicKey = otherUsersPublicKey
                )

                if (ephemeralCameFromCorrectUser) {
                    DHCrypto.agreeSecretKey(prkSelf = privateEphemeral, pbkPeer = sendingEphemeral)
                } else {
                    throw HandledException.InvalidSignature
                }
            }
    }

    private fun upload(url: URL, file: File): Single<Unit> {
        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        return Single.create<Unit> { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.onSuccess(Unit)
                    } else {
                        continuation.onError(HttpException(response))
                    }
                }
            })
        }.subscribeOn(Schedulers.io())
    }

    private fun download(url: URL): Single<InputStream> {
        val request = Request.Builder().url(url).get().build()

        return Single.create<InputStream> { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.onError(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.onSuccess(response.body!!.byteStream())
                    } else {
                        continuation.onError(HttpException(response))
                    }
                }
            })
        }.subscribeOn(Schedulers.io())
    }

    class HttpException(val okHttpResponse: Response) : RuntimeException(
        "${okHttpResponse.message} ${okHttpResponse.request.url}"
    )
}

enum class MediaType {
    Image,
    Video
}
