package me.jameshunt.dhiffiechat

import com.amazonaws.services.s3.AmazonS3

class ServerCredentials(private val s3: AmazonS3) {
    val firebaseJson by lazy {
        s3
            .getObject(configBucket, "firebaseConfig.json")
            .objectContent
            .readBytes()
            .toString(Charsets.UTF_8)
    }
}