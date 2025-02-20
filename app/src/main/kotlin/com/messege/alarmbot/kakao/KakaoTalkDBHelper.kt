package com.messege.alarmbot.kakao


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

fun incept(n: Int): String {
    val dict1 = listOf(
        "adrp.ldrsh.ldnp", "ldpsw", "umax", "stnp.rsubhn", "sqdmlsl", "uqrshl.csel", "sqshlu",
        "umin.usubl.umlsl", "cbnz.adds", "tbnz", "usubl2", "stxr", "sbfx", "strh", "stxrb.adcs",
        "stxrh", "ands.urhadd", "subs", "sbcs", "fnmadd.ldxrb.saddl", "stur", "ldrsb", "strb",
        "prfm", "ubfiz", "ldrsw.madd.msub.sturb.ldursb", "ldrb", "b.eq", "ldur.sbfiz", "extr",
        "fmadd", "uqadd", "sshr.uzp1.sttrb", "umlsl2", "rsubhn2.ldrh.uqsub", "uqshl", "uabd",
        "ursra", "usubw", "uaddl2", "b.gt", "b.lt", "sqshl", "bics", "smin.ubfx", "smlsl2",
        "uabdl2", "zip2.ssubw2", "ccmp", "sqdmlal", "b.al", "smax.ldurh.uhsub", "fcvtxn2", "b.pl"
    )

    val dict2 = listOf(
        "saddl", "urhadd", "ubfiz.sqdmlsl.tbnz.stnp", "smin", "strh", "ccmp", "usubl",
        "umlsl", "uzp1", "sbfx", "b.eq", "zip2.prfm.strb", "msub", "b.pl", "csel",
        "stxrh.ldxrb", "uqrshl.ldrh", "cbnz", "ursra", "sshr.ubfx.ldur.ldnp", "fcvtxn2",
        "usubl2", "uaddl2", "b.al", "ssubw2", "umax", "b.lt", "adrp.sturb", "extr",
        "uqshl", "smax", "uqsub.sqshlu", "ands", "madd", "umin", "b.gt", "uabdl2",
        "ldrsb.ldpsw.rsubhn", "uqadd", "sttrb", "stxr", "adds", "rsubhn2.umlsl2",
        "sbcs.fmadd", "usubw", "sqshl", "stur.ldrsh.smlsl2", "ldrsw", "fnmadd", "stxrb.sbfiz",
        "adcs", "bics.ldrb", "l1ursb", "subs.uhsub", "ldurh", "uabd", "sqdmlal"
    )

    val word1 = dict1[n % dict1.size]
    val word2 = dict2[(n + 31) % dict2.size]
    return "$word1.$word2"
}

fun genSalt(userId: Int, encType: Int): ByteArray {
    if (userId <= 0) {
        return ByteArray(16) { 0 } // 16바이트 0으로 채움
    }

    val prefixes = arrayOf(
        "", "", "12", "24", "18", "30", "36", "12", "48", "7", "35", "40", "17", "23", "29",
        "isabel", "kale", "sulli", "van", "merry", "kyle", "james", "maddux",
        "tony", "hayden", "paul", "elijah", "dorothy", "sally", "bran",
        incept(830819), "veil"
    )

    val saltStr = if (encType < prefixes.size) {
        prefixes[encType] + userId.toString()
    } else {
        throw IllegalArgumentException("Unsupported encoding type $encType")
    }
    val truncatedSalt = saltStr.take(16)
    val paddedSalt = truncatedSalt.padEnd(16, '\u0000')

    return paddedSalt.toByteArray(Charsets.UTF_8)
}

fun pkcs16adjust(a: ByteArray, aOff: Int, b: ByteArray) {
    var x = (b[b.size - 1].toInt() and 0xFF) + (a[aOff + b.size - 1].toInt() and 0xFF) + 1
    a[aOff + b.size - 1] = (x % 256).toByte()
    x = x shr 8

    for (i in b.size - 2 downTo 0) {
        x += (b[i].toInt() and 0xFF) + (a[aOff + i].toInt() and 0xFF)
        a[aOff + i] = (x % 256).toByte()
        x = x shr 8
    }
}

fun deriveKey(password: ByteArray, salt: ByteArray, iterations: Int, dkeySize: Int): ByteArray {
    val passwordUtf16 = (password + byteArrayOf(0)).toString(Charsets.US_ASCII).toByteArray(Charsets.UTF_16BE)

    val sha1 = MessageDigest.getInstance("SHA-1")
    val v = sha1.digestLength  // 블록 크기 (보통 64바이트)
    val u = sha1.digest().size // 해시 크기 (20바이트)

    val D = ByteArray(v) { 1 }

    val S = ByteArray(v * ((salt.size + v - 1) / v)) { salt[it % salt.size] }
    val P = ByteArray(v * ((passwordUtf16.size + v - 1) / v)) { passwordUtf16[it % passwordUtf16.size] }

    val I = S + P
    val B = ByteArray(v)
    val c = (dkeySize + u - 1) / u

    val dKey = ByteArray(dkeySize)
    for (i in 1..c) {
        sha1.reset()
        sha1.update(D)
        sha1.update(I)
        var A = sha1.digest()

        for (j in 1 until iterations) {
            sha1.reset()
            sha1.update(A)
            A = sha1.digest()
        }

        for (j in B.indices) {
            B[j] = A[j % A.size]
        }

        for (j in 0 until I.size / v) {
            pkcs16adjust(I, j * v, B)
        }

        val start = (i - 1) * u
        if (i == c) {
            A.copyInto(dKey, start, 0, dkeySize - start)
        } else {
            A.copyInto(dKey, start, 0, A.size)
        }
    }

    return dKey
}

private val keyCache = mutableMapOf<ByteArray, ByteArray>()

// 초기 키 값
private val defaultKey = byteArrayOf(
    0x16, 0x08, 0x09, 0x6f, 0x02, 0x17, 0x2b, 0x08,
    0x21, 0x21, 0x0a, 0x10, 0x03, 0x03, 0x07, 0x06
)

// 고정 IV 값
private val iv = byteArrayOf(
    0x0f, 0x08, 0x01, 0x00, 0x19, 0x47, 0x25, 0xdc.toByte(),
    0x15, 0xf5.toByte(), 0x17, 0xe0.toByte(), 0xe1.toByte(), 0x15, 0x0c, 0x35
)

fun decrypt(userId: Int, encType: Int, b64Ciphertext: String): String? {
    try {
        val salt = genSalt(userId, encType)
        val key = keyCache[salt] ?: deriveKey(defaultKey, salt, 2, 32).also { keyCache[salt] = it }

        val secretKey = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

        val ciphertext = Base64.decode(b64Ciphertext, Base64.DEFAULT)
        if (ciphertext.isEmpty()) return b64Ciphertext

        val padded = cipher.doFinal(ciphertext)

        // PKCS5Padding 제거
        val padding = padded.last().toInt()
        val plaintext = padded.copyOfRange(0, padded.size - padding)

        return String(plaintext, Charsets.UTF_8)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

//fun openEncryptedDatabase(context: Context, dbName: String, key: String): SQLiteDatabase? {
//    SQLiteDatabase.loadLibs(context) // SQLCipher 라이브러리 로드
//    val dbFile = File(context.getDatabasePath(dbName).absolutePath)
//    return if (dbFile.exists()) {
//        try {
//            SQLiteDatabase.openDatabase(dbFile.path, key,null, SQLiteDatabase.OPEN_READWRITE)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    } else {
//        println("Database file not found at")
//        null
//    }
//}

class KakaoTalkDBHelper(context: Context, dbName: String, version: Int) :
    SQLiteOpenHelper(context, dbName, null, version) {

    override fun onCreate(db: SQLiteDatabase) {
        // DB 초기화 코드 (필요 시 추가)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // DB 업그레이드 로직 (필요 시 추가)
    }

    companion object {
        fun getDatabase(context: Context, dbName: String): SQLiteDatabase? {
            val dbFile = File(context.getDatabasePath(dbName).absolutePath)
            return if (dbFile.exists()) {
                SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
            } else {
                null
            }
        }
    }

    private val rootName = "/data/data"
    private val appPackageName = "com.kakao.talk"
    private val databases = "/databases"
    private val appDBName = "KakaoTalk.db"

    val dbFile = File("${rootName}/$appPackageName$databases/$appDBName")

    suspend fun testCode(){
        try {
            val process0 = Runtime.getRuntime().exec(arrayOf("su", "-c", "setenforce 0"))
            val result0 = process0.inputStream.bufferedReader().readText()
            println("WILLY >> setenforce 0 : $result0 ")

            val texts = listOf(rootName, "${rootName}/$appPackageName", "${rootName}/$appPackageName$databases","${rootName}/$appPackageName$databases/$appDBName")
            for (text in texts){
                // Chmod
                val result1 = runAsRoot("chmod 777 $text")
                println("WILLY >> $text chmod : $result1 ")

                // List
                val result2 = runAsRoot("ls -al $text")
                println("WILLY >> $text result : $result2 ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun runAsRoot(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Failed to execute command: ${e.message}"
        }
    }
}
