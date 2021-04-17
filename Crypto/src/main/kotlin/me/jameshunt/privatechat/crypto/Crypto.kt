package me.jameshunt.privatechat.crypto

import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DHCrypto {

    private const val AES_KEY_SIZE = 128
    private val kpg: KeyPairGenerator by lazy {
        KeyPairGenerator.getInstance("DH").also { it.initialize(1024) }
    }

    @Throws(Exception::class)
    fun agreeSecretKey(
        prkSelf: PrivateKey?,
        pbkPeer: PublicKey?
    ): SecretKey {
        // instantiates and inits a KeyAgreement
        val ka: KeyAgreement = KeyAgreement.getInstance("DH")
        ka.init(prkSelf)
        // Computes the KeyAgreement
        ka.doPhase(pbkPeer, true)
        // Generates the shared secret
        val secret: ByteArray = ka.generateSecret()

        // === Generates an AES key ===

        // you should really use a Key Derivation Function instead, but this is
        // rather safe
        val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")
        val bkey: ByteArray = Arrays.copyOf(
            sha256.digest(secret), AES_KEY_SIZE / java.lang.Byte.SIZE)
        return SecretKeySpec(bkey, "AES")
    }

    fun genDHKeyPair(): KeyPair {
        return kpg.genKeyPair()
    }
}

object AESCrypto {
    fun generateIv(): IvParameterSpec {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }

    @Throws(
        NoSuchPaddingException::class, NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class,
        InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class
    )
    fun encrypt(input: String, key: SecretKey, iv: IvParameterSpec): String {
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText: ByteArray = cipher.doFinal(input.toByteArray())
        return Base64.getEncoder().encodeToString(cipherText)
    }

    @Throws(
        NoSuchPaddingException::class, NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class,
        InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class
    )
    fun decrypt(cipherText: String, key: SecretKey, iv: IvParameterSpec): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText))
        return String(plainText)
    }
}

fun String.toPublicKey(): PublicKey {
    val bytes = Base64.getDecoder().decode(this)
    return KeyFactory.getInstance("DH").generatePublic(X509EncodedKeySpec(bytes))
}

fun String.toPrivateKey(): PrivateKey {
    val bytes = Base64.getDecoder().decode(this)
    return KeyFactory.getInstance("DH").generatePrivate(PKCS8EncodedKeySpec(bytes))
}

fun String.toIv(): IvParameterSpec = IvParameterSpec(Base64.getDecoder().decode(this))