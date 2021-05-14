package me.jameshunt.dhiffiechat.crypto

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.PublicKey
import java.util.*

fun PublicKey.toUserId(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.encoded)
    .toBase64String()

fun ByteArray.toBase64String(): String {
    return Base64.getEncoder().encodeToString(this)
}


fun File.toS3Key(): String {
    val length = this.length().coerceAtMost(100_000).toInt()
    val bytes = ByteArray(length)

    FileInputStream(this).use {
        it.read(bytes, 0, length)
    }

    return MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .toBase64String()
        .replace("/", "_") // don't make folders
}
