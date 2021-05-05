package me.jameshunt.dhiffiechat.crypto

import java.security.MessageDigest
import java.security.PublicKey
import java.util.*

fun ByteArray.toS3Key(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this)
    .let { Base64.getEncoder().encodeToString(it) }
    .replace("/", "_") // don't make folders

fun PublicKey.toUserId(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.encoded)
    .let { Base64.getEncoder().encodeToString(it) }