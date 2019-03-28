package vn.zenity.football.manager.encrypt

import java.security.SecureRandom
import kotlin.experimental.and

/**
 * Created by vinhdn on 04-Mar-18.
 */
public class CommonFunction {
    /**
     * Convert bytes to hex
     *
     * @param bytes
     * @return
     */
    companion object {
        @JvmStatic
        fun bytesToHex(bytes: ByteArray?): String? {
            var result: String? = null
            if (bytes != null) {
                val hexArray = "0123456789ABCDEF".toCharArray()
                val hexChars = CharArray(bytes.size * 2)
                for (j in bytes.indices) {
                    val v = (bytes[j] and 0xFF.toByte()).toInt()

                    val val0 = hexArray[(v and 0xF0) ushr 4]
                    val val1 = hexArray[v and 0x0F]
                    hexChars[j * 2] = val0
                    hexChars[j * 2 + 1] = val1
                }
                result = String(hexChars)
            }

            return result
        }

        /**
         * @param s
         * @return
         */
        @JvmStatic
        fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }

        /**
         * Generate random token
         *
         * @return String
         */
        @JvmStatic
        fun createTokenRandom(): String {
            val random = SecureRandom()
            val bytes = ByteArray(20)
            random.nextBytes(bytes)
            return bytes.toString()
        }
    }

}