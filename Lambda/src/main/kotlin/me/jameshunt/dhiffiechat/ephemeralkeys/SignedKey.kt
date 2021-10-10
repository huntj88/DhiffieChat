package me.jameshunt.dhiffiechat.ephemeralkeys

data class SignedKey(
    val publicKey: String,
    val signature: String
)