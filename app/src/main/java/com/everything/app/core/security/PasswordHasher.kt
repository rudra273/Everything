package com.everything.app.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordHasher {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)

    fun hash(secret: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret, salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(secret: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val actual = hash(secret, salt)
        return MessageDigest.isEqual(actual, expectedHash)
    }

    private companion object {
        const val ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PBKDF2_ITERATIONS = 310_000
        const val HASH_BITS = 256
        const val SALT_BYTES = 32
        val secureRandom = SecureRandom()
    }
}
