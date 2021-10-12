package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.blockingSubscribeBy
import me.jameshunt.dhiffiechat.crypto.DHCrypto
import me.jameshunt.dhiffiechat.crypto.RSACrypto
import me.jameshunt.dhiffiechat.crypto.toBase64String
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.time.Instant


/**
 * used for testing send and receive, without an integration test with server
 */
class SharedResources {
    val user1KeyPair = RSACrypto.generateKeyPair()
    val user2KeyPair = RSACrypto.generateKeyPair()

    val user2Ephemeral = DHCrypto.genDHKeyPair()

    val fileToTest = File("src/test/res/test.png")
    val inputFile = File("src/test/res/duplicateOfTest.png").let {
        fileToTest.copyTo(it)
    }
    val outGoingEncryptedFile = File("src/test/res/outGoing")
    val uploadedEncryptedFile = File("src/test/res/uploadedEncrypted")
    val incomingDecryptedFile = File("src/test/res/incoming")

    var user1SignedEphemeral: LambdaApi.SignedKey? = null
    var encryptedText: String? = null
}

/**
 * dependencies and mocks for sender of message
 */

class User1MessageService(private val sharedResources: SharedResources) {
    private val identityManager = object : IdentityManager {
        override fun getIdentity(): KeyPair {
            return sharedResources.user1KeyPair
        }
    }

    private val remoteFileService = object : RemoteFileService {
        override fun upload(url: URL, file: File): Single<Unit> {
            return Single.just(Unit)
                .doOnSuccess { file.copyTo(sharedResources.uploadedEncryptedFile) }
                .doOnSuccess { assertTrue(true) }
        }

        override fun download(url: URL): Single<InputStream> = TODO("Not yet implemented")
    }

    private val lambdaApi = object : TestLambdaApi() {
        override fun getUserPublicKey(
            type: String,
            body: LambdaApi.GetUserPublicKey
        ): Single<LambdaApi.GetUserPublicKeyResponse> {
            assertEquals(body.userId, sharedResources.user2KeyPair.toUserId())
            return Single.just(
                LambdaApi.GetUserPublicKeyResponse(
                    sharedResources.user2KeyPair.public.toBase64String()
                )
            )
        }

        override fun sendMessage(
            type: String,
            body: LambdaApi.SendMessage
        ): Single<LambdaApi.SendMessageResponse> {
            sharedResources.encryptedText = body.text
            sharedResources.user1SignedEphemeral = body.signedSendingPublicKey
            return Single.just(LambdaApi.SendMessageResponse(URL("https://place.toUploadFile.com")))
        }

        override fun getEphemeralPublicKey(
            type: String,
            body: LambdaApi.EphemeralPublicKeyRequest
        ): Single<LambdaApi.SignedKey> {
            val public = sharedResources.user2Ephemeral.public
            val otherUserEphemeralSigned = LambdaApi.SignedKey(
                publicKey = public,
                signature = RSACrypto.sign(
                    public.toBase64String(),
                    sharedResources.user2KeyPair.private
                )
            )
            return Single.just(otherUserEphemeralSigned)
        }
    }

    private val ephemeralKeyService = object : EphemeralKeyService {
        override fun getMatchingPrivateKey(ephemeralPublicKey: PublicKey): PrivateKey = TODO()
        override fun deleteKeyPair(ephemeralPublicKey: PublicKey): Unit = TODO()
    }

    val messageService = MessageService(
        identityManager = identityManager,
        remoteFileService = remoteFileService,
        api = lambdaApi,
        outgoingEncryptedFile = sharedResources.outGoingEncryptedFile,
        incomingDecryptedFile = sharedResources.incomingDecryptedFile,
        ephemeralKeyService = ephemeralKeyService
    )
}

/**
 * dependencies and mocks for receiver of message
 */

class User2MessageService(private val sharedResources: SharedResources) {
    private val identityManager = object : IdentityManager {
        override fun getIdentity(): KeyPair {
            return sharedResources.user2KeyPair
        }
    }

    private val remoteFileService = object : RemoteFileService {
        override fun upload(url: URL, file: File): Single<Unit> = TODO()
        override fun download(url: URL): Single<InputStream> = Single.just(
            sharedResources.uploadedEncryptedFile.inputStream()
        )
    }

    private val lambdaApi = object : TestLambdaApi() {
        override fun getUserPublicKey(
            type: String,
            body: LambdaApi.GetUserPublicKey
        ): Single<LambdaApi.GetUserPublicKeyResponse> {
            assertEquals(body.userId, sharedResources.user1KeyPair.toUserId())

            return Single.just(
                LambdaApi.GetUserPublicKeyResponse(
                    sharedResources.user1KeyPair.public.toBase64String()
                )
            )
        }

        override fun consumeMessage(
            type: String,
            body: LambdaApi.ConsumeMessage
        ): Single<LambdaApi.ConsumeMessageResponse> {
            return Single.just(LambdaApi.ConsumeMessageResponse(URL("https://place.toUploadFile.com")))
        }
    }

    private val ephemeralKeyService = object : EphemeralKeyService {
        override fun getMatchingPrivateKey(ephemeralPublicKey: PublicKey): PrivateKey {
            return sharedResources.user2Ephemeral.private
        }

        override fun deleteKeyPair(ephemeralPublicKey: PublicKey) {
            assertTrue(ephemeralPublicKey == sharedResources.user2Ephemeral.public)
        }
    }

    val messageService = MessageService(
        identityManager = identityManager,
        remoteFileService = remoteFileService,
        api = lambdaApi,
        outgoingEncryptedFile = sharedResources.outGoingEncryptedFile,
        incomingDecryptedFile = sharedResources.incomingDecryptedFile,
        ephemeralKeyService = ephemeralKeyService
    )
}

class MessageServiceTest {
    private val sharedResources = SharedResources()
    private val user1MessageService = User1MessageService(sharedResources)
    private val user2MessageService = User2MessageService(sharedResources)

    @Test
    fun testSendAndReceiveEncryptionButMockingServer() {
        val testStringToEncryptAndDecrypt = "testStringToEncryptAndDecrypt"

        user1MessageService.messageService.sendMessage(
            recipientUserId = sharedResources.user2KeyPair.toUserId(),
            text = testStringToEncryptAndDecrypt,
            file = sharedResources.inputFile,
            mediaType = MediaType.Image
        ).flatMap {
            user2MessageService.messageService.decryptMessage(
                LambdaApi.Message(
                    to = sharedResources.user2KeyPair.toUserId(),
                    from = sharedResources.user1KeyPair.toUserId(),
                    messageCreatedAt = Instant.now(),
                    text = sharedResources.encryptedText,
                    mediaType = MediaType.Image,
                    fileKey = "",
                    signedS3Url = null,
                    ephemeralPublicKey = sharedResources.user2Ephemeral.public,
                    sendingPublicKey = sharedResources.user1SignedEphemeral!!.publicKey,
                    sendingPublicKeySignature = sharedResources.user1SignedEphemeral!!.signature
                )
            )
        }.blockingSubscribeBy(
            onSuccess = { (message, file) ->
                println(message)
                assertEquals(message.text, testStringToEncryptAndDecrypt)
                val isSame = file.readBytes().contentEquals(sharedResources.fileToTest.readBytes())
                assertTrue(isSame)
                sharedResources.incomingDecryptedFile.delete()
                sharedResources.uploadedEncryptedFile.delete()
            },
            onError = {
                throw it
            }
        )
    }
}

