package com.messege.alarmbot.kakao

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object KakaoDecrypt {
    val keyCache = mutableMapOf<String, ByteArray>()

    private fun incept(n: Int): String {
        val dict1 = listOf(
            "adrp.ldrsh.ldnp", "ldpsw", "umax", "stnp.rsubhn", "sqdmlsl", "uqrshl.csel", "sqshlu", "umin.usubl.umlsl", "cbnz.adds", "tbnz",
            "usubl2", "stxr", "sbfx", "strh", "stxrb.adcs", "stxrh", "ands.urhadd", "subs", "sbcs", "fnmadd.ldxrb.saddl",
            "stur", "ldrsb", "strb", "prfm", "ubfiz", "ldrsw.madd.msub.sturb.ldursb", "ldrb", "b.eq", "ldur.sbfiz", "extr",
            "fmadd", "uqadd", "sshr.uzp1.sttrb", "umlsl2", "rsubhn2.ldrh.uqsub", "uqshl", "uabd", "ursra", "usubw", "uaddl2",
            "b.gt", "b.lt", "sqshl", "bics", "smin.ubfx", "smlsl2", "uabdl2", "zip2.ssubw2", "ccmp", "sqdmlal",
            "b.al", "smax.ldurh.uhsub", "fcvtxn2", "b.pl"
        )

        val dict2 = listOf(
            "saddl", "urhadd", "ubfiz.sqdmlsl.tbnz.stnp", "smin", "strh", "ccmp", "usubl", "umlsl", "uzp1", "sbfx",
            "b.eq", "zip2.prfm.strb", "msub", "b.pl", "csel", "stxrh.ldxrb", "uqrshl.ldrh", "cbnz", "ursra", "sshr.ubfx.ldur.ldnp",
            "fcvtxn2", "usubl2", "uaddl2", "b.al", "ssubw2", "umax", "b.lt", "adrp.sturb", "extr", "uqshl",
            "smax", "uqsub.sqshlu", "ands", "madd", "umin", "b.gt", "uabdl2", "ldrsb.ldpsw.rsubhn", "uqadd", "sttrb",
            "stxr", "adds", "rsubhn2.umlsl2", "sbcs.fmadd", "usubw", "sqshl", "stur.ldrsh.smlsl2", "ldrsw", "fnmadd", "stxrb.sbfiz",
            "adcs", "bics.ldrb", "l1ursb", "subs.uhsub", "ldurh", "uabd", "sqdmlal"
        )

        val word1 = dict1[n % dict1.size]
        val word2 = dict2[(n + 31) % dict2.size]
        return "$word1.$word2"
    }

    private fun genSalt(userId: Long, encType: Int): ByteArray {
        if (userId <= 0) return ByteArray(16)

        val prefixes = arrayOf(
            "", "", "12", "24", "18", "30", "36", "12", "48", "7", "35", "40", "17", "23", "29",
            "isabel", "kale", "sulli", "van", "merry", "kyle", "james", "maddux",
            "tony", "hayden", "paul", "elijah", "dorothy", "sally", "bran",
            incept(830819), "veil"
        )

        val saltStr = try {
            (prefixes[encType] + userId.toString()).take(16)
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Unsupported encoding type $encType")
        }
        val saltBytes = saltStr.padEnd(16, '\u0000').toByteArray(Charsets.UTF_8)
        return saltBytes
    }

    private fun pkcs16adjust(a: ByteArray, aOff: Int, b: ByteArray) {
        var x = (b[b.size - 1].toInt() and 0xff) + (a[aOff + b.size - 1].toInt() and 0xff) + 1
        a[aOff + b.size - 1] = (x % 256).toByte()
        x = x shr 8

        for (i in b.size - 2 downTo 0) {
            x += (b[i].toInt() and 0xff) + (a[aOff + i].toInt() and 0xff)
            a[aOff + i] = (x % 256).toByte()
            x = x shr 8
        }
    }

    private fun deriveKey(password: ByteArray, salt: ByteArray, iterations: Int, dkeySize: Int): ByteArray {
        val passwordUtf16 = (password + byteArrayOf(0)).toString(Charsets.US_ASCII).toByteArray(Charsets.UTF_16BE)

        val hasher = MessageDigest.getInstance("SHA-1")
        val v = 64
        val u = hasher.digestLength

        val D = ByteArray(v) { 1.toByte() }
        val S = ByteArray(v * ((salt.size + v - 1) / v)) { salt[it % salt.size] }
        val P = ByteArray(v * ((passwordUtf16.size + v - 1) / v)) { passwordUtf16[it % passwordUtf16.size] }

        val I = S + P
        val B = ByteArray(v)
        val c = (dkeySize + u - 1) / u

        val dKey = ByteArray(dkeySize)

        for (i in 1..c) {
            hasher.reset()
            hasher.update(D)
            hasher.update(I)
            var A = hasher.digest()

            for (j in 1 until iterations) {
                hasher.reset()
                hasher.update(A)
                A = hasher.digest()
            }

            A = A.copyOf()  // A = list(A) 변환
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
        var key = byteArrayOf(0x16, 0x08, 0x09, 0x6f, 0x02, 0x17, 0x2b, 0x08,
            0x21, 0x21, 0x0a, 0x10, 0x03, 0x03, 0x07, 0x06)
        val iv = byteArrayOf(0x0f, 0x08, 0x01, 0x00, 0x19, 0x47, 0x25, 0xdc.toByte(),
            0x15, 0xf5.toByte(), 0x17, 0xe0.toByte(), 0xe1.toByte(), 0x15, 0x0c, 0x35)

        val salt = genSalt(userId, encType)
        val saltKey = salt.decodeToString()

        if (keyCache.containsKey(saltKey)) {
            key = keyCache[saltKey]!!
        } else {
            key = deriveKey(key, salt, 2, 32)
            keyCache[saltKey] = key
        }

        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))

        val ciphertext = Base64.getDecoder().decode(b64Ciphertext)
        if (ciphertext.isEmpty()) {
            return b64Ciphertext
        }
        val padded = cipher.doFinal(ciphertext)
        val plaintext = try {
            padded.copyOfRange(0, padded.size - padded.last().toInt())
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Unable to decrypt data", e)
        }

        return try {
            plaintext.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            plaintext.joinToString("") { "%02x".format(it) } // Hex 값으로 반환
        }
    }
}