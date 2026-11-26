package com.h1son5633.coda.net

import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * NetEase Cloud Music request encryption, the same schemes the open-source
 * ecosystem (NeteaseCloudMusicApi et al.) uses and Coda's platform layer
 * stands on:
 *  - eapi: AES-128-ECB over a digest-bound payload, for app API endpoints.
 *  - weapi: double AES-128-CBC + textbook RSA over a random session key,
 *    for web endpoints (QR login lives here).
 * Verified byte-for-byte against music.163.com from the build host.
 */
object NeteaseCrypto {

    private const val EAPI_KEY = "e82ckenh8dichen8"
    private const val WEAPI_PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val WEAPI_IV = "0102030405060708"
    private const val RSA_MODULUS =
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725" +
        "152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312" +
        "ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424" +
        "d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"
    private val RSA_EXPONENT = BigInteger("10001", 16)

    private fun md5Hex(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun aesEcbEncrypt(plain: ByteArray, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        return cipher.doFinal(plain)
    }

    private fun aesCbcEncrypt(plain: ByteArray, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(WEAPI_IV.toByteArray(Charsets.UTF_8))
        )
        return cipher.doFinal(plain)
    }

    /** Returns the `params` form value for an eapi call to [apiPath] (e.g. "/api/cloudsearch/pc"). */
    fun eapiParams(apiPath: String, dataJson: String): String {
        val digest = md5Hex("nobody${apiPath}use${dataJson}md5forencrypt")
        val payload = "$apiPath-36cd479b6b5-$dataJson-36cd479b6b5-$digest"
        val encrypted = aesEcbEncrypt(payload.toByteArray(Charsets.UTF_8), EAPI_KEY)
        return encrypted.joinToString("") { "%02X".format(it) }
    }

    data class WeapiResult(val params: String, val encSecKey: String)

    /** Returns the `params`/`encSecKey` form values for a weapi call. */
    fun weapiParams(dataJson: String): WeapiResult {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val secretKey = (1..16).map { alphabet.random() }.joinToString("")
        val first = Base64.encodeToString(
            aesCbcEncrypt(dataJson.toByteArray(Charsets.UTF_8), WEAPI_PRESET_KEY),
            Base64.NO_WRAP
        )
        val second = Base64.encodeToString(
            aesCbcEncrypt(first.toByteArray(Charsets.UTF_8), secretKey),
            Base64.NO_WRAP
        )
        // Textbook RSA over the reversed secret key, zero-padded to 256 hex chars.
        val reversedHex = secretKey.reversed().toByteArray(Charsets.UTF_8)
            .joinToString("") { "%02x".format(it) }
        val encrypted = BigInteger(reversedHex, 16)
            .modPow(RSA_EXPONENT, BigInteger(RSA_MODULUS, 16))
        return WeapiResult(second, encrypted.toString(16).padStart(256, '0'))
    }
}
