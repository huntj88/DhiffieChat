package me.jameshunt.dhiffiechat

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.ref.WeakReference
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


class DI(application: DhiffieChatApp) {

    fun setLifecycleComponents(mainActivity: MainActivity) {
        LifeCycleAwareComponents.sharedPreferences = WeakReference(
            mainActivity.getSharedPreferences(
                "prefs",
                AppCompatActivity.MODE_PRIVATE
            )
        )
    }

    private object LifeCycleAwareComponents {
        lateinit var sharedPreferences: WeakReference<SharedPreferences>
    }

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
        .baseUrl("https://lcbv8emtt0.execute-api.us-east-1.amazonaws.com/stage/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val driver: SqlDriver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = application,
        name = "dhiffiechat.db"
    )
    private val database = Database(driver)

    val identityManager = IdentityManager { LifeCycleAwareComponents.sharedPreferences.get()!! }

    private val authManager = AuthManager(identityManager, moshi)
    private val api: DhiffieChatApi = retrofit.create(DhiffieChatApi::class.java)
    private val networkHelper = NetworkHelper(identityManager, authManager)
    val relationshipService = UserService(database.aliasQueries, networkHelper, api, authManager, identityManager)
    val s3Service = S3Service(okhttp, networkHelper, authManager, api, relationshipService)
    val userService = UserService(database.aliasQueries, networkHelper, api, authManager, identityManager)
}
