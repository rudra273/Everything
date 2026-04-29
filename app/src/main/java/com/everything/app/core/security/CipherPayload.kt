package com.everything.app.core.security

data class CipherPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray,
)
