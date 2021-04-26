package me.jameshunt.privatechat

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


object DI {
    fun setLifecycleComponents(mainActivity: MainActivity) {
        LifeCycleAwareComponents.sharedPreferences = mainActivity.getSharedPreferences(
            "prefs",
            AppCompatActivity.MODE_PRIVATE
        )
    }

    private object LifeCycleAwareComponents {
        lateinit var sharedPreferences: SharedPreferences
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(object {
            @ToJson
            fun toJson(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
            @FromJson
            fun fromJson(string: String): Instant = Instant.parse(string)
        })
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okhttp = OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okhttp)
        .baseUrl("https://dt0ccztxg7.execute-api.us-east-1.amazonaws.com/test/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: PrivateChatApi = retrofit.create(PrivateChatApi::class.java)
    val identityManager = IdentityManager { LifeCycleAwareComponents.sharedPreferences }
    private val authManager = AuthManager(identityManager, moshi)

    val privateChatService = PrivateChatService(api, authManager)
}
