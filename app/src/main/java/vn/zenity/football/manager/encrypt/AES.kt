package vn.zenity.football.manager.encrypt

import android.util.Log
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.security.*
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * Created by tranduc on 12/18/17.
 */
public class AES {

    private var cipher: Cipher? = null
    private var key: ByteArray? = null
    private var iv: ByteArray? = null

    private enum class EncryptMode {
        ENCRYPT, DECRYPT
    }

    init {
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
//            java.security.Security.addProvider(BouncyCastleProvider())
            key = ByteArray(16) // 256 bit key space
            iv = ByteArray(16) // 128 bit IV
        } catch (e: NoSuchAlgorithmException) {
            cipher = null
            Log.e(TAG, "AES/CBC/PKCS5Padding not found:", e)
        } catch (e: NoSuchPaddingException) {
            cipher = null
            Log.e(TAG, "AES/CBC/PKCS5Padding padding not found:", e)
        }

    }

    /**
     * encrypt text
     *
     * @param inputText
     * @param encryptionKey
     * @param mode
     * @param initVector
     * @return
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    @Throws(UnsupportedEncodingException::class, InvalidKeyException::class, InvalidAlgorithmParameterException::class, IllegalBlockSizeException::class, BadPaddingException::class)
    private fun encryptDecrypt(inputText: String, encryptionKey: String, mode: EncryptMode, initVector: String): String {
        var out: String? = null
        var len = encryptionKey.toByteArray(charset("UTF-8")).size

        if (encryptionKey.toByteArray(charset("UTF-8")).size > key!!.size) {
            len = key!!.size
        }

        var ivlen = initVector.toByteArray(charset("UTF-8")).size

        if (initVector.toByteArray(charset("UTF-8")).size > iv!!.size) {
            ivlen = iv!!.size
        }

        System.arraycopy(encryptionKey.toByteArray(charset("UTF-8")), 0, key!!, 0, len)
        System.arraycopy(initVector.toByteArray(charset("UTF-8")), 0, iv!!, 0, ivlen)

        val keySpec = SecretKeySpec(key, "AES")

        val ivSpec = IvParameterSpec(iv)

        // encryption
        if (mode == EncryptMode.ENCRYPT) {
            // Potentially insecure random numbers on Android 4.3 and older.
            // Read
            // https://android-developers.blogspot.com/2013/08/some-securerandom-thoughts.html
            // for more info.

            cipher!!.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val results = cipher!!.doFinal(inputText.toByteArray(charset("UTF-8")))
            out = CommonFunction.bytesToHex(results)
        }

        // decryption
        if (mode == EncryptMode.DECRYPT) {
            cipher!!.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedValue = CommonFunction.hexStringToByteArray(inputText)
            val decryptedVal = cipher!!.doFinal(decodedValue)
            out = String(decryptedVal, Charset.forName("UTF-8"))
        }
        return out ?: ""
    }

    /***
     * This function encrypts the plain text to cipher text using the key
     * provided. You'll have to use the same key for decryption
     *
     * @param _plainText
     * Plain text to be encrypted
     * @param _key
     * Encryption Key. You'll have to use the same key for decryption
     * @param _iv
     * initialization Vector
     * @return returns encrypted (cipher) text
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    @Synchronized
    fun encrypt(plainText: String, key: String, iv: String): String? {
        var strResult: String? = ""
        try {
            strResult = encryptDecrypt(plainText, key, EncryptMode.ENCRYPT, iv)
        } catch (e: InvalidKeyException) {
            strResult = null
            Log.e(TAG, "Encryption Error - Key is invalid: ", e)
        } catch (e: UnsupportedEncodingException) {
            strResult = null
            Log.e(TAG, "Encryption Error - Encoding is invalid: ", e)
        } catch (e: InvalidAlgorithmParameterException) {
            strResult = null
            Log.e(TAG,"Encryption Error - Param is invalid: ", e)
        } catch (e: IllegalBlockSizeException) {
            strResult = null
            Log.e(TAG, "Encryption Error - Blocksize is invalid: ", e)
        } catch (e: BadPaddingException) {
            strResult = null
            Log.e(TAG,"Encryption Error - Bad padding: ", e)
        }

        return strResult
    }

    /***
     * This funtion decrypts the encrypted text to plain text using the key
     * provided. You'll have to use the same key which you used during
     * encryption
     *
     * @param encryptedText
     * Encrypted/Cipher text to be decrypted
     * @param key
     * Encryption key which you used during encryption
     * @param iv
     * initialization Vector
     * @return encrypted value
     * @throws InvalidKeyException
     * @throws UnsupportedEncodingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    @Synchronized
    fun decrypt(encryptedText: String, key: String, iv: String): String? {
        var strResult: String? = ""
        try {
            strResult = encryptDecrypt(encryptedText, key, EncryptMode.DECRYPT, iv)
        } catch (e: InvalidKeyException) {
            strResult = null
            Log.e(TAG, "Encryption Error - Key is invalid: ", e)
        } catch (e: UnsupportedEncodingException) {
            strResult = null
            Log.e(TAG, "Encryption Error - Encoding is invalid: ", e)
        } catch (e: InvalidAlgorithmParameterException) {
            strResult = null
            Log.e(TAG,"Encryption Error - Param is invalid: ", e)
        } catch (e: IllegalBlockSizeException) {
            strResult = null
            Log.e(TAG, "Encryption Error - Blocksize is invalid: ", e)
        } catch (e: BadPaddingException) {
            strResult = null
            Log.e(TAG,"Encryption Error - Bad padding: ", e)
        }

        return strResult
    }

    companion object {
        private val TAG = "AES"
        private var aes: AES? = null

        /**
         * get an instance of AES
         *
         * @return
         */
        val instance: AES
            @Synchronized get() {
                if (aes == null) {
                    aes = AES()
                }
                return aes!!
            }

        /**
         *
         * @param inputString
         * @return
         */
        fun md5(inputString: String): String {
            val MD5 = "MD5"
            try {
                // Create MD5 Hash
                val digest = java.security.MessageDigest.getInstance(MD5)
                digest.update(inputString.toByteArray())
                val messageDigest = digest.digest()

                return CommonFunction.bytesToHex(messageDigest)
                        ?: ""

            } catch (e: NoSuchAlgorithmException) {
                Log.e(TAG, "Can't MD5", e)
            }

            return ""
        }

        /***
         * This function computes the SHA256 hash of input string
         *
         * @param text
         * input text whose SHA256 hash has to be computed
         * @param length
         * length of the text to be returned
         * @return returns SHA256 hash of input text
         * @throws NoSuchAlgorithmException
         * @throws UnsupportedEncodingException
         */
        @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
        fun SHA256(text: String, length: Int): String {

            val resultStr: String
            val md = MessageDigest.getInstance("SHA-256")

            md.update(text.toByteArray(charset("UTF-8")))
            val digest = md.digest()

            val result = StringBuffer()
            for (b in digest) {
                result.append(String.format("%02x", b)) // convert to hex
            }

            if (length > result.toString().length) {
                resultStr = result.toString()
            } else {
                resultStr = result.toString().substring(0, length)
            }

            return resultStr

        }

        /**
         * this function generates random string for given length
         *
         * @param length
         * Desired length * @return
         */
        fun generateRandomIV(length: Int): String {
            val ranGen = SecureRandom()
            val aesKey = ByteArray(16)
            ranGen.nextBytes(aesKey)
            val result = StringBuffer()
            for (b in aesKey) {
                result.append(String.format("%02x", b)) // convert to hex
            }
            return if (length > result.toString().length) {
                result.toString()
            } else {
                result.toString().substring(0, length)
            }
        }

        /**
         * Tao key random
         *
         * @return
         */

        fun createAesKey(): String? {
            var result: String? = ""
            try {
                val key = SHA256(CommonFunction.createTokenRandom(), 32)
                val iv = generateRandomIV(16)
                result = key + StringConstant.IVAESKEYSPACE + iv
            } catch (e: NoSuchAlgorithmException) {
                result = null
                Log.e(TAG, "Can't create aes key", e)
            } catch (e: UnsupportedEncodingException) {
                result = null
                Log.e(TAG, "Can't create aes key - invalid encoding", e)
            }

            return result
        }
    }
}