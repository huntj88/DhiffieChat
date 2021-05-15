package me.jameshunt.dhiffiechat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import me.jameshunt.dhiffiechat.RequestType.*
import me.jameshunt.dhiffiechat.crypto.*
import okhttp3.*
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import java.io.IOException
import java.net.URL
import java.security.PublicKey
import java.time.Instant
import javax.crypto.spec.IvParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class RequestType {
    data class GetUserPublicKey(val userId: String) : RequestType() {
        data class Response(val publicKey: String)
    }

    object GetMessageSummaries : RequestType() {
        data class MessageSummary(
            val from: String,
            val count: Int,
            val next: Message
        )

        data class Message(
            val to: String,
            val from: String,
            val messageCreatedAt: Instant,
            val text: String?,
            val fileKey: String,
            val iv: String,
            val mediaType: MediaType,
            val signedS3Url: URL?
        )
    }

    object GetUserRelationships : RequestType() {
        data class Response(
            val sentRequests: List<String>,
            val receivedRequests: List<String>,
            val friends: List<String>,
        )
    }

    /**
     * idempotent above
     *
     * mutating below
     */
    data class CreateIdentity(
        val publicKey: String,
        val iv: String,
        val encryptedToken: String
    ) : RequestType()

    data class ScanQR(val scannedUserId: String) : RequestType()

    data class SendMessage(
        val recipientUserId: String,
        val s3Key: String,
        val userUserIv: IvParameterSpec,
        val mediaType: MediaType
    ) : RequestType() {
        data class Response(val uploadUrl: URL)
    }

    data class ConsumeMessage(
        val fileKey: String,
        val timeSent: String
    ) : RequestType() {
        data class Response(val s3Url: URL)
    }
}

class SingleEndpointApi(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val networkHelper: NetworkHelper
) {
    private val toMediaType = "application/json".toMediaType()

    suspend fun createIdentity(requestType: CreateIdentity) {
        val request = Request.Builder()
            .defaultUrl<CreateIdentity>()
            .handleRequestBody(moshi.adapter(CreateIdentity::class.java), requestType).build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        it.resume(Unit)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    suspend fun getUserPublicKey(requestType: GetUserPublicKey): GetUserPublicKey.Response {
        val request = Request.Builder()
            .defaultUrl<GetUserPublicKey>()
            .handleRequestBody(moshi.adapter(GetUserPublicKey::class.java), requestType)
            .headers(networkHelper.standardHeaders())
            .build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val value = moshi
                            .adapter(GetUserPublicKey.Response::class.java)
                            .fromJson(response.body!!.source())!!
                        it.resume(value)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    suspend fun sendMessage(requestType: SendMessage): SendMessage.Response {
        val request = Request.Builder()
            .defaultUrl<SendMessage>()
            .handleRequestBody(moshi.adapter(SendMessage::class.java), requestType)
            .headers(networkHelper.standardHeaders())
            .build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val value = moshi
                            .adapter(SendMessage.Response::class.java)
                            .fromJson(response.body!!.source())!!
                        it.resume(value)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    suspend fun consumeMessage(requestType: ConsumeMessage): ConsumeMessage.Response {
        val request = Request.Builder()
            .defaultUrl<ConsumeMessage>()
            .handleRequestBody(moshi.adapter(ConsumeMessage::class.java), requestType)
            .headers(networkHelper.standardHeaders())
            .build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val value = moshi
                            .adapter(ConsumeMessage.Response::class.java)
                            .fromJson(response.body!!.source())!!
                        it.resume(value)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    suspend fun scanQR(requestType: ScanQR) {
        val request = Request.Builder()
            .defaultUrl<GetUserRelationships>()
            .handleRequestBody(moshi.adapter(ScanQR::class.java), requestType)
            .headers(networkHelper.standardHeaders())
            .build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        it.resume(Unit)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    suspend fun getUserRelationships(): GetUserRelationships.Response {
        val request = Request.Builder()
            .defaultUrl<GetUserRelationships>()
            .emptyBody()
            .headers(networkHelper.standardHeaders())
            .build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val value = moshi
                            .adapter(GetUserRelationships.Response::class.java)
                            .fromJson(response.body!!.source())!!
                        it.resume(value)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    suspend fun getMessageSummaries(): List<GetMessageSummaries.MessageSummary> {
        val request = Request.Builder()
            .defaultUrl<GetMessageSummaries>()
            .emptyBody()
            .headers(networkHelper.standardHeaders())
            .build()

        return suspendCoroutine {
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val newParameterizedType = Types.newParameterizedType(
                            MutableList::class.java,
                            GetMessageSummaries.MessageSummary::class.java
                        )

                        val value = moshi
                            .adapter<List<GetMessageSummaries.MessageSummary>>(newParameterizedType)
                            .fromJson(response.body!!.source())!!
                        it.resume(value)
                    } else {
                        it.resumeWithException(Exception(response.toString()))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    it.resumeWithException(e)
                }
            })
        }
    }

    private inline fun <reified T> Request.Builder.defaultUrl(): Request.Builder = this
        .url("https://lbedr5sli7.execute-api.us-east-1.amazonaws.com/stage/PerformRequest?type=${T::class.java.simpleName}")

    private fun <T> Request.Builder.handleRequestBody(
        adapter: JsonAdapter<T>,
        request: T
    ): Request.Builder = this
        .post(object : RequestBody() {
            override fun contentType(): okhttp3.MediaType = toMediaType
            override fun writeTo(sink: BufferedSink) {
                adapter.toJson(sink, request)
            }
        })

    private fun Request.Builder.emptyBody(): Request.Builder = this
        .post(object : RequestBody() {
            override fun contentType(): okhttp3.MediaType? = null
            override fun writeTo(sink: BufferedSink) {
                sink.write(byteArrayOf())
            }
        })
}

class NetworkHelper(
    private val identityManager: IdentityManager,
    private val authManager: AuthManager
) {
    fun standardHeaders(vararg additionalHeaders: Map<String, String>): Headers {
        val toUserId = identityManager.getIdentity().toUserId()

        val formatted = arrayOf(*additionalHeaders, userToServerHeaders())
            .map { it.entries }
            .flatten()
            .flatMap { listOf(it.key, it.value) }
            .toTypedArray()

        return Headers.headersOf("userId", toUserId, *formatted)
    }

    private fun userToServerHeaders(): Map<String, String> {
        val userToServerCredentials = authManager.userToServerAuth(getServerPublicKey())
        return mapOf(
            "userServerIv" to userToServerCredentials.iv.toBase64String(),
            "userServerEncryptedToken" to userToServerCredentials.encryptedToken
        )
    }

    fun getServerPublicKey(): PublicKey {
        return serverPublic.toPublicKey()
    }
}

fun ByteArray.toBitmap(): Bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)