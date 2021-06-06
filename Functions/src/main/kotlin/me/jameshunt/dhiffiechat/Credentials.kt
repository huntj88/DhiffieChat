package me.jameshunt.dhiffiechat

import me.jameshunt.dhiffiechat.crypto.DHCrypto.genDHKeyPair
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toPrivateKey
import java.security.PrivateKey


object Credentials {
    val firebaseJson by lazy {
        Singletons.s3
            .getObject("dhiffiechat-config-bucket", "firebaseConfig.json")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
    }

    val serverPrivateKey = getS3PrivateKey()

    private fun getS3PrivateKey(): PrivateKey {
        generateKeyPairIfMissing()
        return Singletons.s3
            .getObject("dhiffiechat-config-bucket", "serverKeyPair.private")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
            .toPrivateKey()
    }

    private fun generateKeyPairIfMissing() {
        val exists = Singletons.s3.doesObjectExist("dhiffiechat-config-bucket", "serverKeyPair.private")

        if (!exists) {
            val dh = genDHKeyPair()
            Singletons.s3.putObject(
                "dhiffiechat-config-bucket",
                "serverKeyPair.private",
                dh.private.toBase64String()
            )
            Singletons.s3.putObject(
                "dhiffiechat-config-bucket",
                "serverKeyPair.public",
                dh.public.toBase64String()
            )
        }
    }
}