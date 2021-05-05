package me.jameshunt.dhiffiechat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URL
import kotlin.coroutines.suspendCoroutine

class S3Service(private val okHttpClient: OkHttpClient) {

    suspend fun uploadToS3(byteArray: ByteArray, url: URL) {
        val request = Request.Builder()
            .url(url)
            .put(byteArray.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        suspendCoroutine<Unit> { continuation ->
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        TODO()
                    }
                }
            })
        }
    }
}