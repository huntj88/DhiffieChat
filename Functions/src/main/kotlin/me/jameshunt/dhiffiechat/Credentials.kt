package me.jameshunt.dhiffiechat

import me.jameshunt.dhiffiechat.crypto.DHCrypto.genDHKeyPair
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toPrivateKey
import java.security.PrivateKey

class Credentials {
    val firebaseJson by lazy {
        Singletons.s3
            .getObject(configBucketName, "firebaseConfig.json")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
    }

    val serverPrivateKey = getS3PrivateKey()

    private fun getS3PrivateKey(): PrivateKey {
        generateKeyPairIfMissing()
        return Singletons.s3
            .getObject(configBucketName, "serverKeyPair.private")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
            .toPrivateKey()
    }

    private fun generateKeyPairIfMissing() {
        if (Singletons.s3.doesObjectExist(configBucketName, "serverKeyPair.private")) {
            return
        }

        val dh = genDHKeyPair()
        Singletons.s3.putObject(
            configBucketName,
            "serverKeyPair.private",
            dh.private.toBase64String()
        )
        Singletons.s3.putObject(
            configBucketName,
            "serverKeyPair.public",
            dh.public.toBase64String()
        )
    }
}