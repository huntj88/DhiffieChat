package me.jameshunt.dhiffiechat

import android.content.Context
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toIv
import me.jameshunt.dhiffiechat.crypto.toPublicKey
import me.jameshunt.dhiffiechat.service.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URL
import java.security.PublicKey
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.crypto.spec.IvParameterSpec


class DI(application: DhiffieChatApp) {
    private val fileLocationUtil = FileLocationUtil(application)
    private val prefManager = PrefManager(
        prefs = application.getSharedPreferences("dhiffieChat", Context.MODE_PRIVATE)
    )

    private val dbQueryManager = DBQueryManager(application, prefManager)

    private val moshi: Moshi = Moshi.Builder()
        .add(object {
            @ToJson
            fun toJson(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)
            @FromJson
            fun fromJson(string: String): Instant = Instant.parse(string)
        })
        .add(object {
            @ToJson
            fun toJson(url: URL): String = url.toString()
            @FromJson
            fun fromJson(string: String): URL = URL(string)
        })
        .add(object {
            @ToJson
            fun toJson(iv: IvParameterSpec): String = iv.toBase64String()
            @FromJson
            fun fromJson(base64: String): IvParameterSpec = base64.toIv()
        })
        .add(object {
            @ToJson
            fun toJson(publicKey: PublicKey): String = publicKey.toBase64String()
            @FromJson
            fun fromJson(base64: String): PublicKey = base64.toPublicKey()
        })
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val identityManager = IdentityManager(dbQueryManager.getEncryptionKeyQueries())
    private val authManager = AuthManager(identityManager, moshi)
    private val headerInterceptor = HeaderInterceptor(identityManager, authManager)

    private val okhttp = OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .addInterceptor(headerInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okhttp)
        .baseUrl("https://nfg3h6fz41.execute-api.us-east-1.amazonaws.com/stage/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(LambdaApi::class.java)
    private val launcherService = LauncherService(api)
    private val userService = UserService(
        dbQueryManager.getAliasQueries(), api, authManager, identityManager, prefManager
    )
    private val s3Service = S3Service(okhttp, authManager, api, userService, fileLocationUtil)

    private val injectableComponents = mutableMapOf<String, Any>()

    init {
        register(moshi, s3Service, userService, fileLocationUtil, launcherService)
    }

    private fun register(vararg entry: Any) {
        entry.forEach {
            injectableComponents[it::class.java.canonicalName!!] = it
        }
    }

    fun <T> createInjected(classToInject: Class<T>): T {
        val constructor = classToInject.constructors.first()
        val args = constructor.parameters.map {
            val canonicalName = it.type.canonicalName!!
            injectableComponents[canonicalName] ?: throw IllegalStateException(
                """
                $canonicalName has not been registered, 
                and cannot be injected into ${classToInject.canonicalName!!}
                """.trimIndent()
            )
        }

        @Suppress("UNCHECKED_CAST")
        return constructor.newInstance(*args.toTypedArray()) as T
    }
}
