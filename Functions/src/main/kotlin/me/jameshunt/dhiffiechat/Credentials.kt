package me.jameshunt.dhiffiechat

import me.jameshunt.dhiffiechat.crypto.DHCrypto.genDHKeyPair
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toPrivateKey


object Credentials {
    val firebaseJson by lazy {
        Singletons.s3
            .getObject("config-bucket-z00001", "firebaseConfig.json")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
    }

    val serverPrivateKey by lazy {
        val exists = Singletons.s3.doesObjectExist("config-bucket-z00001", "serverKeyPair.private")

        if (!exists) {
            val dh = genDHKeyPair()
            Singletons.s3.putObject(
                "config-bucket-z00001",
                "serverKeyPair.private",
                dh.private.toBase64String()
            )
            Singletons.s3.putObject(
                "config-bucket-z00001",
                "serverKeyPair.public",
                dh.public.toBase64String()
            )
        }

        Singletons.s3
            .getObject("config-bucket-z00001", "serverKeyPair.private")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
            .toPrivateKey()
    }
}