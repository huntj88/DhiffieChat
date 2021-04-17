package me.jameshunt.privatechat

import android.util.Log
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.jameshunt.privatechat.PrivateChatApi.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.Instant
import java.time.format.DateTimeFormatter


val moshi: Moshi = Moshi.Builder()
    .add(object {
        @ToJson
        fun toJson(instant: Instant): String {
            return DateTimeFormatter.ISO_INSTANT.format(instant)
        }

        @FromJson
        fun fromJson(string: String): Instant {
            return Instant.parse(string)
        }
    })
    .addLast(KotlinJsonAdapterFactory())
    .build()

object Network {
    private val okhttp = OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okhttp)
        .baseUrl("https://5070w7s6vb.execute-api.us-east-1.amazonaws.com/test/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: PrivateChatApi = retrofit.create(PrivateChatApi::class.java)

    fun getServerPublicKey(onResult: (String) -> Unit) {
        api.getServerPublicKey().enqueue(object: Callback<ServerPublicKey> {
            override fun onResponse(call: Call<ServerPublicKey>, response: Response<ServerPublicKey>) {
                onResult(response.body()!!.publicKey)
            }

            override fun onFailure(call: Call<ServerPublicKey>, t: Throwable) {
                throw t
            }
        })
    }

    fun createIdentity(createIdentity: CreateIdentity, onResult: () -> Unit) {
        Log.d("create identity", createIdentity.toString())

//        return
        api.createIdentity(createIdentity).enqueue(object: Callback<ResponseMessage> {
            override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                Log.d("response", response.raw().body!!.toString())
                onResult()
            }

            override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                throw t
            }
        })
    }
}

interface PrivateChatApi {
    data class ServerPublicKey(val publicKey: String)

    @GET("ServerPublicKey")
    fun getServerPublicKey(): Call<ServerPublicKey>

    data class CreateIdentity(
        val publicKey: String,
        val iv: String,
        val encryptedToken: String
    )
    data class ResponseMessage(val message: String)

    @POST("CreateIdentity")
    fun createIdentity(@Body identity: CreateIdentity): Call<ResponseMessage>
}