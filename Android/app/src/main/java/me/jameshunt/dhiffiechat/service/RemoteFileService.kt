package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL

class RemoteFileService(private val okHttpClient: OkHttpClient) {

    class HttpException(val okHttpResponse: Response) : RuntimeException(
        "${okHttpResponse.message} ${okHttpResponse.request.url}"
    )

    fun upload(url: URL, file: File): Single<Unit> {
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

    fun download(url: URL): Single<InputStream> {
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
}