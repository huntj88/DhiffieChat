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
            Log.d("crypto Test", Server.decrypt(clientHeaders, identityManager.getIdentity()))
        }

//        setContent {
//            MainUI(identity = identityManager.getIdentity().hashedIdentity)
//        }
        Log.d("crypto hash", identityManager.getIdentity().hashedIdentity)
    }
}

object Server {
    val keyPair = getServerKeyPair()

    private fun getServerKeyPair(): KeyPair {
        val public = "MIIBojCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGAVjlmBmEKonUv2b0vpbfIImRilwzF/eNwaU8FLtXKF0T5+fYe42izXEYuq/FNABfkFZKbghBtJPHYX0wDS3EvgoDfSUsBKtNJXYQepfirc8bwNCh4FnxC2Fjs0azcxSeYcE9lnG/xilWk8luipN3OACz4ZpOHaRKr0f5vXk1Xxl8="
        val private = "MIIBpAIBADCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoEgYMCgYB9T0DJNCD7JPxcKcx7MEo2m8TtH3JUQofTw79jnJFzyYwYT7EFigzJS8cQ4OqSAC+NP9MOrJp9Mk3ZJ8KbCOL8hwHQwFzoV+bkOfMJ6vbPt1colQ/rTOaP0EDmINTMdhVpMPqlZ7PdoAeT7IcQ8NzT1cWNjomDPAAV3Qwq84pjDQ=="
        return KeyPair(public.toPublicKey(), private.toPrivateKey())
    }

    fun decrypt(clientHeaders: AuthManager.AuthHeaders, clientTestOnlyParamIdentity: Identity): String {
//        val sharedSecretKey = DHCrypto.agreeSecretKey(keyPair.private, getPublicKey(clientHeaders.hashedIdentity))
        val sharedSecretKey = DHCrypto.agreeSecretKey(keyPair.private, clientTestOnlyParamIdentity.publicKey)
        return AESCrypto.decrypt(clientHeaders.encryptedToken, sharedSecretKey, clientHeaders.iv)
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