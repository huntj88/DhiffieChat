package me.jameshunt.dhiffiechat

import me.jameshunt.dhiffiechat.crypto.toBase64String
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.net.URL
import java.security.PublicKey
import java.time.Instant
import javax.crypto.spec.IvParameterSpec


class LauncherService(private val api: LambdaApi, private val prefManager: PrefManager) {
    suspend fun init() {
        api.initSingleEndpoint()
    }

    fun isUserProfileSetup(): Boolean = prefManager.isUserProfileSetup()
}

data class ResponseMessage(val message: String)

interface LambdaApi {

    @POST("PerformRequest")
    suspend fun initSingleEndpoint(@Query("type") type: String = "Init"): ResponseMessage

    data class GetUserPublicKey(val userId: String)
    data class GetUserPublicKeyResponse(val publicKey: String)

    @POST("PerformRequest")
    suspend fun getUserPublicKey(
        @Query("type") type: String = "GetUserPublicKey",
        @Body body: GetUserPublicKey
    ): GetUserPublicKeyResponse

    data class MessageSummary(
        val from: String,
        val count: Int,
        val mostRecentCreatedAt: Instant?,
        val next: Message?
    )

    data class Message(
        val to: String,
        val from: String,
        val messageCreatedAt: Instant,
        val text: String?,
        val fileKey: String,
        val iv: IvParameterSpec,
        val mediaType: MediaType,
        val signedS3Url: URL?
    )

    @POST("PerformRequest")
    suspend fun getMessageSummaries(
        @Query("type") type: String = "GetMessageSummaries"
    ): List<MessageSummary>


    data class UserRelationships(
        val sentRequests: List<String>,
        val receivedRequests: List<String>,
        val friends: List<String>,
    )

    @POST("PerformRequest")
    suspend fun getUserRelationships(
        @Query("type") type: String = "GetUserRelationships"
    ): UserRelationships

    /**
     * idempotent above
     *
     * mutating below
     */
    data class CreateIdentity(
        val publicKey: PublicKey,
        val iv: IvParameterSpec,
        val encryptedToken: String,
        val fcmToken: String
    )

    @POST("PerformRequest")
    suspend fun createIdentity(
        @Query("type") type: String = "CreateIdentity",
        @Body body: CreateIdentity
    ): ResponseMessage

    data class ScanQR(val scannedUserId: String)

    @POST("PerformRequest")
    suspend fun scanQR(
        @Query("type") type: String = "ScanQR",
        @Body body: ScanQR
    ): ResponseMessage


    data class SendMessage(
        val recipientUserId: String,
        val s3Key: String,
        val userUserIv: IvParameterSpec,
        val mediaType: MediaType
    )
    data class SendMessageResponse(val uploadUrl: URL)

    @POST("PerformRequest")
    suspend fun sendMessage(
        @Query("type") type: String = "SendMessage",
        @Body body: SendMessage
    ): SendMessageResponse

    data class ConsumeMessage(
        val fileKey: String,
        val timeSent: Instant
    )
    data class ConsumeMessageResponse(val s3Url: URL)

    @POST("PerformRequest")
    suspend fun consumeMessage(
        @Query("type") type: String = "ConsumeMessage",
        @Body body: ConsumeMessage
    ): ConsumeMessageResponse
}

class HeaderInterceptor(
    private val identityManager: IdentityManager,
    private val authManager: AuthManager
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // do not bother with Auth for CreateIdentity request
        return when (request.url.toString().contains("CreateIdentity")) {
            true -> chain.proceed(request)
            false -> {
                val headers = request.headers.newBuilder()
                    .addAll(standardHeaders())
                    .build()

                chain.proceed(request.newBuilder().headers(headers).build())
            }
        }
    }

    private fun standardHeaders(): Headers {
        val toUserId = identityManager.getIdentity().toUserId()

        val formatted = userToServerHeaders()
            .entries
            .flatMap { listOf(it.key, it.value) }
            .toTypedArray()

        return Headers.headersOf("userId", toUserId, *formatted)
    }

    private fun userToServerHeaders(): Map<String, String> {
        val userToServerCredentials = authManager.userToServerAuth()
        return mapOf(
            "userServerIv" to userToServerCredentials.iv.toBase64String(),
            "userServerEncryptedToken" to userToServerCredentials.encryptedToken
        )
    }
}
