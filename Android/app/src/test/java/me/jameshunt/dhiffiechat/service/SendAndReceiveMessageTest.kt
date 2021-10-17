package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import me.jameshunt.dhiffiechat.HandledException
import me.jameshunt.dhiffiechat.crypto.DHCrypto
import me.jameshunt.dhiffiechat.crypto.RSACrypto
import me.jameshunt.dhiffiechat.crypto.toBase64String
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.time.Instant
import java.util.*


/**
 * used for testing send and receive, without an integration test with server
 */
class SharedResources {
    private val testId: String = UUID.randomUUID().toString()
    val user1KeyPair = RSACrypto.generateKeyPair()
    val user2KeyPair = RSACrypto.generateKeyPair()

    val user2Ephemeral = DHCrypto.genDHKeyPair()

    val fileToTest = File("src/test/res/test.png")
    val inputFile = File("src/test/res/generated/$testId/duplicateOfTest.png").let {
        fileToTest.copyTo(it)
    }
    val outGoingEncryptedFile = File("src/test/res/generated/$testId/outGoing")
    val incomingDecryptedFile = File("src/test/res/generated/$testId/incoming")

    var user1SignedEphemeral: LambdaApi.SignedKey? = null
    var encryptedText: String? = null
    val uploadedEncryptedFile = File("src/test/res/generated/$testId/uploadedEncrypted")
}

/**
 * dependencies and mocks for sender of message
 * is possible to use the default implementation or extend to fit the test
 */

class DefaultSenderService(
    private val sharedResources: SharedResources,
    identityManager: IdentityManager = testIdentityManager(sharedResources),
    remoteFileService: RemoteFileService = testRemoteFileService(sharedResources),
    lambdaApi: LambdaApi = testLambdaApi(sharedResources),
    ephemeralKeyService: EphemeralKeyService = testEphemeralKeyService()
) {
    companion object {
        fun testIdentityManager(sharedResources: SharedResources): IdentityManager = object : IdentityManager {
            override fun getIdentity(): KeyPair {
                return sharedResources.user1KeyPair
            }
        }

        fun testRemoteFileService(sharedResources: SharedResources): RemoteFileService = object : RemoteFileService {
            override fun upload(url: URL, file: File): Single<Unit> {
                return Single.just(Unit)
                    .doOnSuccess { file.copyTo(sharedResources.uploadedEncryptedFile) }
                    .doOnSuccess { assertTrue(true) }
            }

            override fun download(url: URL): Single<InputStream> = notNeededForServerMock()
        }

        fun testLambdaApi(sharedResources: SharedResources): LambdaApi = object : TestLambdaApi() {
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
                // Images/video are uploaded separately after a signed upload url
                // for s3 is returned in the response
                val sendMessageResponse = LambdaApi.SendMessageResponse(
                    URL("https://someS3url.thatIsSigned.com")
                )
                return Single.just(sendMessageResponse)
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

        fun testEphemeralKeyService(): EphemeralKeyService = object : EphemeralKeyService {
            override fun getMatchingPrivateKey(ephemeralPublicKey: PublicKey): PrivateKey =
                notNeededForMock("only used when receiving message")

            override fun deleteKeyPair(ephemeralPublicKey: PublicKey): Unit =
                notNeededForMock("only used when receiving message")
        }
    }

    val messageService: MessageService = MessageService(
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

class DefaultReceiverService(private val sharedResources: SharedResources) {
    private val identityManager = object : IdentityManager {
        override fun getIdentity(): KeyPair {
            return sharedResources.user2KeyPair
        }
    }

    private val remoteFileService = object : RemoteFileService {
        override fun upload(url: URL, file: File): Single<Unit> {
            notNeededForServerMock("only used when sending message")
        }

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
            val s3Url = URL("https://someS3url.thatIsSigned.com")
            return Single.just(LambdaApi.ConsumeMessageResponse(s3Url))
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

class SendAndReceiveMessageTest {

    @Test
    fun testInvalidSenderSignature() {
        val sharedResources = SharedResources()
        val user1MessageService = DefaultSenderService(sharedResources)
        val user2MessageService = DefaultReceiverService(sharedResources)

        val testStringToEncryptAndDecrypt = "testStringToEncryptAndDecrypt"

        user1MessageService.messageService
            .sendMessage(
                recipientUserId = sharedResources.user2KeyPair.toUserId(),
                text = testStringToEncryptAndDecrypt,
                file = sharedResources.inputFile,
                mediaType = MediaType.Image
            )
            .flatMap {
                // Use signature from unexpected user instead to test checking for identity
                val randomRSAKeyPair = RSACrypto.generateKeyPair()
                val signatureFromWrongUser = RSACrypto.sign(
                    sharedResources.user1SignedEphemeral!!.publicKey.toBase64String(),
                    randomRSAKeyPair.private
                )

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
                        sendingPublicKeySignature = signatureFromWrongUser
                    )
                )
            }
            .test()
            .await()
            .assertError(HandledException.InvalidSignature)
            .assertNoValues()
            .assertNotComplete()
    }

    @Test
    fun testInvalidReceiverSignature() {
        val sharedResources = SharedResources()
        val user1MessageService = DefaultSenderService(
            sharedResources = sharedResources,
            lambdaApi = object : TestLambdaApi() {
                override fun getUserPublicKey(
                    type: String,
                    body: LambdaApi.GetUserPublicKey
                ): Single<LambdaApi.GetUserPublicKeyResponse> {
                    // default Test Implementation
                    return DefaultSenderService.testLambdaApi(sharedResources).getUserPublicKey(body = body)
                }

                override fun getEphemeralPublicKey(
                    type: String,
                    body: LambdaApi.EphemeralPublicKeyRequest
                ): Single<LambdaApi.SignedKey> {
                    val public = sharedResources.user2Ephemeral.public

                    // Use signature from unexpected user instead to test checking for identity
                    val randomRSAKeyPair = RSACrypto.generateKeyPair()
                    val otherUserEphemeralSigned = LambdaApi.SignedKey(
                        publicKey = public,
                        signature = RSACrypto.sign(
                            public.toBase64String(),
                            randomRSAKeyPair.private
                        )
                    )
                    return Single.just(otherUserEphemeralSigned)
                }
            }
        )

        val testStringToEncryptAndDecrypt = "testStringToEncryptAndDecrypt"

        user1MessageService.messageService
            .sendMessage(
                recipientUserId = sharedResources.user2KeyPair.toUserId(),
                text = testStringToEncryptAndDecrypt,
                file = sharedResources.inputFile,
                mediaType = MediaType.Image
            )
            .test()
            .await()
            .assertError(HandledException.InvalidSignature)
            .assertNoValues()
            .assertNotComplete()
    }

    @Test
    fun testValidMessage() {
        val sharedResources = SharedResources()
        val user1MessageService = DefaultSenderService(sharedResources)
        val user2MessageService = DefaultReceiverService(sharedResources)

        val testStringToEncryptAndDecrypt = "testStringToEncryptAndDecrypt"

        user1MessageService.messageService
            .sendMessage(
                recipientUserId = sharedResources.user2KeyPair.toUserId(),
                text = testStringToEncryptAndDecrypt,
                file = sharedResources.inputFile,
                mediaType = MediaType.Image
            )
            .flatMap {
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
            }
            .test()
            .await()
            .assertNoErrors()
            .assertComplete()
            .values().first().let { (message, file) ->
                assertEquals(message.text, testStringToEncryptAndDecrypt)
                val isSame = file.readBytes().contentEquals(sharedResources.fileToTest.readBytes())
                assertTrue(isSame)
            }
    }
}

