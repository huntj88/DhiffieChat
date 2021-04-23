package me.jameshunt.privatechat.crypto

import java.math.BigInteger
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.*
import javax.crypto.spec.DHParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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
        prkSelf: PrivateKey?,
        pbkPeer: PublicKey?
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
    fun encrypt(input: ByteArray, key: SecretKey, iv: IvParameterSpec): ByteArray {
        val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText: ByteArray = cipher.doFinal(input)
        return Base64.getEncoder().encode(cipherText)
    }

    @Throws(
        NoSuchPaddingException::class, NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class,
        InvalidKeyException::class, BadPaddingException::class, IllegalBlockSizeException::class
    )
    fun decrypt(cipherInput: ByteArray, key: SecretKey, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(Base64.getDecoder().decode(cipherInput))
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

fun IvParameterSpec.toBase64String(): String = Base64.getEncoder().encodeToString(iv)
fun PublicKey.toBase64String(): String = Base64.getEncoder().encodeToString(encoded)
fun PrivateKey.toBase64String(): String = Base64.getEncoder().encodeToString(encoded)

fun PublicKey.toHashedIdentity(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.encoded)
    .let { Base64.getEncoder().encodeToString(it) }