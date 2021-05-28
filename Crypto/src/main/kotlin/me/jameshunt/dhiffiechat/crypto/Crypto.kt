package me.jameshunt.dhiffiechat.crypto

import java.io.*
import java.math.BigInteger
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.CipherOutputStream


// TODO: secure private key
const val serverPrivate = "MIIBZwIBADCCARsGCSqGSIb3DQEDATCCAQwCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoCAgIABEMCQQCPu0nb03BX15VXPWTCLlgsiKKJc3IXJKrzM/eLdvh1S8dg02liqxk7f2qoHT4gOsvfYdBB38NOORWesa1pWyCz"
const val serverPublic = "MIIBpzCCARsGCSqGSIb3DQEDATCCAQwCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoCAgIAA4GFAAKBgQDJLVn3e5VaJQkr728oJWpPPHJijsLA7sqw5hkBlobp1LnklJ/Y3VjeaYGXx58KCx8vrfM4FbTpIayLEAobAE7ZhIvsOSArVm+92LA7KdKMXIgakmqkj4HSV3P+ptcwi4eWfhhGiAV5Uz0wN2RoDyqA89oD2GHSSSqqvNZZFQCWzQ=="

object DHCrypto {

    private const val AES_KEY_SIZE = 128
    private val p = BigInteger("178011905478542266528237562450159990145232156369120674273274450314442865788737020770612695252123463079567156784778466449970650770920727857050009668388144034129745221171818506047231150039301079959358067395348717066319802262019714966524135060945913707594956514672855690606794135837542707371727429551343320695239")
    private val g = BigInteger("174068207532402095185811980123523436538604490794561350978495831040599953488455823147851597408940950725307797094915759492368300574252438761037084473467180148876118103083043754985190983472601550494691329488083395492313850000361646482644608492304078721818959999056496097769368017749273708962006689187956744210730")
    private val dhParameterSpec = DHParameterSpec(p, g)

    private val kpg: KeyPairGenerator by lazy {
        KeyPairGenerator.getInstance("DH").also { it.initialize(dhParameterSpec) }
    }

    @Throws(Exception::class)
    fun agreeSecretKey(
        prkSelf: PrivateKey,
        pbkPeer: PublicKey
    ): SecretKey {
        // instantiates and inits a KeyAgreement
        val ka: KeyAgreement = KeyAgreement.getInstance("DH")
        ka.init(prkSelf, dhParameterSpec)
        // Computes the KeyAgreement
        ka.doPhase(pbkPeer, true)
        // Generates the shared secret
        val secret: ByteArray = ka.generateSecret()

        // === Generates an AES key ===

        // you should really use a Key Derivation Function instead, but this is
        // rather safe
        val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")
        val bkey: ByteArray = Arrays.copyOf(
            sha256.digest(secret), AES_KEY_SIZE / java.lang.Byte.SIZE
        )
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
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun encrypt(input: ByteArray, key: SecretKey, iv: IvParameterSpec): ByteArray {
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(input)
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun encrypt(file: File, output: File, key: SecretKey, iv: IvParameterSpec) {
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)

        val cipherStream = CipherOutputStream(output.outputStream(), cipher)
        file.inputStream().use { inStream ->
            cipherStream.use { outStream -> inStream.copyTo(outStream) }
        }
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun decrypt(inputStream: InputStream, output: File, key: SecretKey, iv: IvParameterSpec) {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)

        CipherInputStream(inputStream, cipher).use {
            it.copyTo(output.outputStream())
        }
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun decrypt(cipherInput: ByteArray, key: SecretKey, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(cipherInput)
    }
}

fun String.toPublicKey(): PublicKey {
    val bytes = this.base64ToByteArray()
    return KeyFactory.getInstance("DH").generatePublic(X509EncodedKeySpec(bytes))
}

fun String.toPrivateKey(): PrivateKey {
    val bytes = this.base64ToByteArray()
    return KeyFactory.getInstance("DH").generatePrivate(PKCS8EncodedKeySpec(bytes))
}

fun String.toIv(): IvParameterSpec = IvParameterSpec(this.base64ToByteArray())

fun IvParameterSpec.toBase64String(): String = iv.toBase64String()
fun PublicKey.toBase64String(): String = encoded.toBase64String()
fun PrivateKey.toBase64String(): String = encoded.toBase64String()

fun String.base64ToByteArray(): ByteArray = Base64.getDecoder().decode(this)
