package com.rudra.everything.core.security

data class CipherPayload(
    val ciphertext: ByteArray,
    val iv: ByteArray,
)
