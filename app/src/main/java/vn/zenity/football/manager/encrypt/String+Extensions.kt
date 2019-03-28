package vn.zenity.football.manager.encrypt

/**
 * Created by vinhdn on 04-Mar-18.
 */
fun String.encryptMsg(): String {
    val key = "12345678901234556678901234567"
    val iv = "1234567894501269"
    return AES.instance.encrypt(this, key, iv) ?: ""
}

fun String.decryptMsg(): String {
    val key = "12345678901234556678901234567"
    val iv = "1234567894501269"
    return AES.instance.decrypt(this, key, iv) ?: ""
}