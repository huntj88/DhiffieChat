package me.jameshunt.privatechat

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.jameshunt.privatechat.PrivateChatApi.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET


object Network {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://a94ckbr7c8.execute-api.us-east-1.amazonaws.com/test/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: PrivateChatApi = retrofit.create(PrivateChatApi::class.java)

    fun getServerPublicKey(onResult: (String) -> Unit) {
        api.getServerPublicKey().enqueue(object: Callback<ServerPublicKey> {
            override fun onResponse(call: Call<ServerPublicKey>, response: Response<ServerPublicKey>) {
                onResult(response.body()!!.publicKey)
            }

            override fun onFailure(call: Call<ServerPublicKey>, t: Throwable) {
                t.printStackTrace()
                TODO("Not yet implemented")
            }
        })
    }
}

interface PrivateChatApi {
    data class ServerPublicKey(val publicKey: String)

    @GET("ServerPublicKey")
    fun getServerPublicKey(): Call<ServerPublicKey>
}