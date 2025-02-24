package com.messege.alarmbot.kakao

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object KakaoDecrypt {
    private val keyCache = mutableMapOf<String, ByteArray>()

    private val dict1 = listOf(
        "adrp.ldrsh.ldnp", "ldpsw", "umax", "stnp.rsubhn", "sqdmlsl", "uqrshl.csel", "sqshlu", "umin.usubl.umlsl", "cbnz.adds", "tbnz",
        "usubl2", "stxr", "sbfx", "strh", "stxrb.adcs", "stxrh", "ands.urhadd", "subs", "sbcs", "fnmadd.ldxrb.saddl",
        "stur", "ldrsb", "strb", "prfm", "ubfiz", "ldrsw.madd.msub.sturb.ldursb", "ldrb", "b.eq", "ldur.sbfiz", "extr",
        "fmadd", "uqadd", "sshr.uzp1.sttrb", "umlsl2", "rsubhn2.ldrh.uqsub", "uqshl", "uabd", "ursra", "usubw", "uaddl2",
        "b.gt", "b.lt", "sqshl", "bics", "smin.ubfx", "smlsl2", "uabdl2", "zip2.ssubw2", "ccmp", "sqdmlal",
        "b.al", "smax.ldurh.uhsub", "fcvtxn2", "b.pl"
    )
    private val dict2 = listOf(
        "saddl", "urhadd", "ubfiz.sqdmlsl.tbnz.stnp", "smin", "strh", "ccmp", "usubl", "umlsl", "uzp1", "sbfx",
        "b.eq", "zip2.prfm.strb", "msub", "b.pl", "csel", "stxrh.ldxrb", "uqrshl.ldrh", "cbnz", "ursra", "sshr.ubfx.ldur.ldnp",
        "fcvtxn2", "usubl2", "uaddl2", "b.al", "ssubw2", "umax", "b.lt", "adrp.sturb", "extr", "uqshl",
        "smax", "uqsub.sqshlu", "ands", "madd", "umin", "b.gt", "uabdl2", "ldrsb.ldpsw.rsubhn", "uqadd", "sttrb",
        "stxr", "adds", "rsubhn2.umlsl2", "sbcs.fmadd", "usubw", "sqshl", "stur.ldrsh.smlsl2", "ldrsw", "fnmadd", "stxrb.sbfiz",
        "adcs", "bics.ldrb", "l1ursb", "subs.uhsub", "ldurh", "uabd", "sqdmlal"
    )
    private val prefixes = arrayOf(
        "", "", "12", "24", "18", "30", "36", "12", "48", "7", "35", "40", "17", "23", "29",
        "isabel", "kale", "sulli", "van", "merry", "kyle", "james", "maddux",
        "tony", "hayden", "paul", "elijah", "dorothy", "sally", "bran",
        incept(830819), "veil"
    )

    private fun incept(n: Int): String {
        val word1 = dict1[n % dict1.size]
        val word2 = dict2[(n + 31) % dict2.size]
        return "$word1.$word2"
    }

    private fun genSalt(userId: Long, encType: Int): ByteArray {
        if (userId <= 0) return ByteArray(16)
        val saltStr = (prefixes.getOrElse(encType) {
            throw IllegalArgumentException("Unsupported encoding type $encType")
        } + userId.toString()).take(16)
        return saltStr.padEnd(16, '\u0000').toByteArray(Charsets.UTF_8)
    }

    private fun pkcs16adjust(a: ByteArray, aOff: Int, b: ByteArray) {
        var carry = 1
        for (i in b.indices.reversed()) {
            val sum = (a[aOff + i].toInt() and 0xff) + (b[i].toInt() and 0xff) + carry
            a[aOff + i] = (sum and 0xff).toByte()
            carry = sum ushr 8
        }
    }

    private fun deriveKey(password: ByteArray, salt: ByteArray, iterations: Int, dkeySize: Int): ByteArray {
        val passwordUtf16 = (password + byteArrayOf(0))
            .toString(Charsets.US_ASCII)
            .toByteArray(Charsets.UTF_16BE)

        val hasher = MessageDigest.getInstance("SHA-1")
        val v = 64
        val u = hasher.digestLength

        val D = ByteArray(v) { 0x01 }
        val S = ByteArray(v * ((salt.size + v - 1) / v)).apply {
            for (i in indices) {
                this[i] = salt[i % salt.size]
            }
        }
        val P = ByteArray(v * ((passwordUtf16.size + v - 1) / v)).apply {
            for (i in indices) {
                this[i] = passwordUtf16[i % passwordUtf16.size]
            }
        }
        val I = S + P
        val B = ByteArray(v)
        val c = (dkeySize + u - 1) / u
        val dKey = ByteArray(dkeySize)

        for (i in 1..c) {
            hasher.reset()
            hasher.update(D)
            hasher.update(I)
            var A = hasher.digest()

            repeat(iterations - 1) {
                A = hasher.digest(A)
            }

            for (j in B.indices) {
                B[j] = A[j % A.size]
            }

            for (j in 0 until (I.size / v)) {
                pkcs16adjust(I, j * v, B)
            }

            val start = (i - 1) * u
            if (i == c) {
                System.arraycopy(A, 0, dKey, start, dkeySize - start)
            } else {
                System.arraycopy(A, 0, dKey, start, A.size)
            }
        }

        return dKey
    }

    fun decrypt(userId: Long, encType: Int, b64Ciphertext: String): String {
        val defaultKey = byteArrayOf(
            0x16, 0x08, 0x09, 0x6f, 0x02, 0x17, 0x2b, 0x08,
            0x21, 0x21, 0x0a, 0x10, 0x03, 0x03, 0x07, 0x06
        )
        val iv = byteArrayOf(
            0x0f, 0x08, 0x01, 0x00, 0x19, 0x47, 0x25, 0xdc.toByte(),
            0x15, 0xf5.toByte(), 0x17, 0xe0.toByte(), 0xe1.toByte(), 0x15, 0x0c, 0x35
        )

        val salt = genSalt(userId, encType)
        val saltKey = salt.decodeToString()

        val key = keyCache.getOrPut(saltKey) {
            deriveKey(defaultKey, salt, iterations = 2, dkeySize = 32)
        }

        val cipher = Cipher.getInstance("AES/CBC/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        }

        val ciphertext = Base64.getDecoder().decode(b64Ciphertext)
        if (ciphertext.isEmpty()) {
            return b64Ciphertext
        }
        val padded = cipher.doFinal(ciphertext)

        // 패딩 값이 유효한지 검사
        val paddingValue = padded.last().toInt() and 0xff
        if (paddingValue < 1 || paddingValue > 16) {
            throw IllegalArgumentException("Invalid padding")
        }
        val plaintextBytes = padded.copyOfRange(0, padded.size - paddingValue)
        return try {
            plaintextBytes.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            plaintextBytes.joinToString("") { "%02x".format(it) }
        }
    }
}