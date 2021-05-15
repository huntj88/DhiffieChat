package me.jameshunt.dhiffiechat

import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toIv
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.crypto.spec.IvParameterSpec


class DI(application: DhiffieChatApp) {
    private val fileLocationUtil = FileLocationUtil(application)

    private val sharedPreferences = application.getSharedPreferences(
        "prefs",
        AppCompatActivity.MODE_PRIVATE
    )

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
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okhttp = OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val driver: SqlDriver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = application,
        name = "dhiffiechat.db"
    )

    private val database = Database(driver)

    private val identityManager = IdentityManager(sharedPreferences)
    private val authManager = AuthManager(identityManager, moshi)
    private val networkHelper = NetworkHelper(identityManager, authManager)
    private val api = SingleEndpointApi(okhttp, moshi, networkHelper)
    private val userService = UserService(
        database.aliasQueries, networkHelper, api, authManager, identityManager
    )
    private val s3Service = S3Service(
        okhttp, authManager, api, userService, fileLocationUtil
    )

    private val injectableComponents = mutableMapOf<String, Any>()

    init {
        register(identityManager, s3Service, userService, fileLocationUtil)
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
        return constructor.newInstance(*args.toTypedArray()) as T
    }
}
