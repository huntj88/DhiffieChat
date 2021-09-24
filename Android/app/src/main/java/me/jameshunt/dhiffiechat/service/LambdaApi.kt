package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import me.jameshunt.dhiffiechat.BuildConfig
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.net.URL
import java.security.PublicKey
import java.time.Instant

data class ResponseMessage(val message: String)

interface LambdaApi {

    @POST("PerformRequest")
    fun initSingleEndpoint(@Query("type") type: String = "Init"): Single<ResponseMessage>

    data class GetUserPublicKey(val userId: String)
    data class GetUserPublicKeyResponse(val publicKey: String)

    @POST("PerformRequest")
    fun getUserPublicKey(
        @Query("type") type: String = "GetUserPublicKey",
        @Body body: GetUserPublicKey
    ): Single<GetUserPublicKeyResponse>

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
        val mediaType: MediaType,
        val signedS3Url: URL?
    )

    @POST("PerformRequest")
    fun getMessageSummaries(
        @Query("type") type: String = "GetMessageSummaries"
    ): Single<List<MessageSummary>>


    data class UserRelationships(
        val sentRequests: List<String>,
        val receivedRequests: List<String>,
        val friends: List<String>,
    )

    @POST("PerformRequest")
    fun getUserRelationships(
        @Query("type") type: String = "GetUserRelationships"
    ): Single<UserRelationships>

    /**
     * idempotent above
     *
     * mutating below
     */
    data class CreateIdentity(
        val publicKey: PublicKey,
        val encryptedToken: String,
        val fcmToken: String
    )

    @POST("PerformRequest")
    fun createIdentity(
        @Query("type") type: String = "CreateIdentity",
        @Body body: CreateIdentity
    ): Single<ResponseMessage>

    data class ScanQR(val scannedUserId: String)

    @POST("PerformRequest")
    fun scanQR(
        @Query("type") type: String = "ScanQR",
        @Body body: ScanQR
    ): Single<ResponseMessage>


    data class SendMessage(
        val recipientUserId: String,
        val text: String?,
        val s3Key: String,
        val mediaType: MediaType
    )
    data class SendMessageResponse(val uploadUrl: URL)

    @POST("PerformRequest")
    fun sendMessage(
        @Query("type") type: String = "SendMessage",
        @Body body: SendMessage
    ): Single<SendMessageResponse>

    data class ConsumeMessage(
        val fileKey: String,
        val timeSent: Instant
    )
    data class ConsumeMessageResponse(val s3Url: URL)

    @POST("PerformRequest")
    fun consumeMessage(
        @Query("type") type: String = "ConsumeMessage",
        @Body body: ConsumeMessage
    ): Single<ConsumeMessageResponse>

    data class RemainingEphemeralReceiveKeysResponse(val remainingKeys: Int)
    @POST("PerformRequest")
    fun remainingEphemeralReceiveKeys(
        @Query("type") type: String = "RemainingEphemeralReceiveKeys"
    ): Single<RemainingEphemeralReceiveKeysResponse>

    data class UploadReceiveKeys(val newKeys: List<PublicKey>)
    @POST("PerformRequest")
    fun uploadEphemeralReceiveKeys(
        @Query("type") type: String = "UploadEphemeralReceiveKeys",
        @Body body: UploadReceiveKeys
    ): Single<ResponseMessage>
}

class HeaderInterceptor(
    private val identityManager: IdentityManager,
    private val authManager: AuthManager
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // do not bother with Auth for CreateIdentity request or wrong base url
        val skipAuth = BuildConfig.BASE_URL !in request.url.toString() ||
            request.url.toString().contains("CreateIdentity")

        return when (skipAuth) {
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
            "userServerEncryptedToken" to userToServerCredentials.encryptedToken
        )
    }
}
