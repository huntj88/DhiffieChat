package me.jameshunt.dhiffiechat

import com.amazonaws.services.s3.AmazonS3
import me.jameshunt.dhiffiechat.crypto.DHCrypto.genDHKeyPair
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toPrivateKey
import java.security.PrivateKey

class Credentials(private val s3: AmazonS3) {
    val firebaseJson by lazy {
        Singletons.s3
            .getObject(configBucket, "firebaseConfig.json")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
    }

    val serverPrivateKey = getS3PrivateKey()

    private fun getS3PrivateKey(): PrivateKey {
        generateKeyPairIfMissing()
        return s3
            .getObject(configBucket, "serverKeyPair.private")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
            .toPrivateKey()
    }

    private fun generateKeyPairIfMissing() {
        if (s3.doesObjectExist(configBucket, "serverKeyPair.private")) {
            return
        }

        val dh = genDHKeyPair()
        s3.putObject(
            configBucket,
            "serverKeyPair.private",
            dh.private.toBase64String()
        )
        s3.putObject(
            configBucket,
            "serverKeyPair.public",
            dh.public.toBase64String()
        )
    }
}