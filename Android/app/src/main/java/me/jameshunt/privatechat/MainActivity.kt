package me.jameshunt.privatechat

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
//import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.DHCrypto
import me.jameshunt.privatechat.crypto.toPrivateKey
import me.jameshunt.privatechat.crypto.toPublicKey
import java.security.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.spec.IvParameterSpec


// todo: first message send userId encrypted with aes

// for server auth, sign current Time with private key. if

class MainActivity : AppCompatActivity() {

    private val identityManager by lazy {
        IdentityManager(
            getSharedPreferences(
                "prefs",
                MODE_PRIVATE
            )
        )
    }
    private val authManager by lazy { AuthManager(identityManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Network.getServerPublicKey {
            Log.d("server public", it)
            val clientHeaders = authManager.getAuthHeaders(serverPublicKey = it.toPublicKey())

            val createIdentity = PrivateChatApi.CreateIdentity(
                publicKey = Base64.getEncoder().encodeToString(identityManager.getIdentity().publicKey.encoded),
                iv = Base64.getEncoder().encodeToString(clientHeaders.iv.iv),
                encryptedToken = clientHeaders.encryptedToken
            )
            Network.createIdentity(createIdentity) {
                Log.d("create Identity", "success")
            }
        }

//        setContent {
//            MainUI(identity = identityManager.getIdentity().hashedIdentity)
//        }
        Log.d("crypto hash", identityManager.getIdentity().hashedIdentity)
    }
}

class AuthManager(private val identityManager: IdentityManager) {
    private data class Token(
        val type: String = "Authentication",
        val expires: Instant
    ) {
        fun toSerialized(): String = moshi.adapter(Token::class.java).toJson(this)
    }

    data class AuthHeaders(
        val hashedIdentity: String,
        val iv: IvParameterSpec,
        val encryptedToken: String
    )

    // TODO: caching, return null after expiration
    fun getAuthHeaders(serverPublicKey: PublicKey): AuthHeaders {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().privateKey,
            pbkPeer = serverPublicKey
        )
        val iv = AESCrypto.generateIv()
        val token = Token(expires = Instant.now().plus(5L, ChronoUnit.MINUTES))
        val encryptedToken = AESCrypto.encrypt(input = token.toSerialized(), sharedSecretKey, iv)

        return AuthHeaders(
            hashedIdentity = identityManager.getIdentity().hashedIdentity,
            iv = iv,
            encryptedToken = encryptedToken
        )
    }
}

class IdentityManager(private val sharedPreferences: SharedPreferences) {
    private val encoder = Base64.getEncoder()

    private var cached: Identity? = null

    fun getIdentity(): Identity = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): Identity? {
        cached?.let { return it }

        val privateBase64 = sharedPreferences.getString("private", null) ?: return null
        val publicBase64 = sharedPreferences.getString("public", null) ?: return null

        Log.d("public base64", publicBase64)
        Log.d("private base64", privateBase64)
        return Identity(
            privateKey = privateBase64.toPrivateKey(),
            publicKey = publicBase64.toPublicKey()
        ).also { cached = it }
    }

    private fun new(): Identity = DHCrypto.genDHKeyPair().let {
        Identity(
            privateKey = it.private,
            publicKey = it.public
        )
    }

    @SuppressLint("ApplySharedPref")
    private fun save(identity: Identity) {
        cached = identity
        val privateKey = encoder.encodeToString(identity.privateKey.encoded)
        val publicKey = encoder.encodeToString(identity.publicKey.encoded)

        sharedPreferences.edit()
            .putString("private", privateKey)
            .putString("public", publicKey)
            .apply()
    }
}

data class Identity(
    val privateKey: PrivateKey,
    val publicKey: PublicKey
) {
    val hashedIdentity: String
        get() = MessageDigest
            .getInstance("SHA-256")
            .digest(publicKey.encoded)
            .let { Base64.getEncoder().encodeToString(it) }
}