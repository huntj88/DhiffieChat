package me.jameshunt.privatechat

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.DHCrypto
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.spec.IvParameterSpec

import java.security.MessageDigest


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

        setContent {
            MainUI(identity = identityManager.getIdentity().hashedIdentity)
        }
        val clientHeaders = authManager.getAuthHeaders(serverPublicKey = Server.keyPair.public)
        Log.d("encrypted token", clientHeaders.encryptedToken)
        Log.d("crypto Test", Server.decrypt(clientHeaders))
        Log.d("crypto hash", identityManager.getIdentity().hashedIdentity)
    }
}

object Server {
    val keyPair = DHCrypto.genDHKeyPair()

    fun decrypt(clientHeaders: AuthManager.AuthHeaders): String {
        val sharedSecretKey = DHCrypto.agreeSecretKey(keyPair.private, getPublicKey(clientHeaders.hashedIdentity))
        return AESCrypto.decrypt(clientHeaders.encryptedToken, sharedSecretKey, clientHeaders.iv)
    }

    private fun getPublicKey(hashedIdentity: String): PublicKey {
        TODO()
    }
}


class AuthManager(private val identityManager: IdentityManager) {
    private data class Token(
        val type: String = "Authentication",
        val expires: Instant
    ) {
        fun toSerialized(): String {
            // TODO
            return toString()
        }
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
    private val decoder = Base64.getDecoder()

    private var cached: Identity? = null

    fun getIdentity(): Identity = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): Identity? {
        cached?.let { return it }

        val privateBase64 = sharedPreferences.getString("private", null) ?: return null
        val publicBase64 = sharedPreferences.getString("public", null) ?: return null

        Log.d("public base64", publicBase64)
        val instance = KeyFactory.getInstance("DH")
        return Identity(
            privateKey = instance.generatePrivate(PKCS8EncodedKeySpec(decoder.decode(privateBase64))),
            publicKey = instance.generatePublic(X509EncodedKeySpec(decoder.decode(publicBase64)))
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