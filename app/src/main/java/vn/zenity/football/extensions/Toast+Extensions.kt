package vn.zenity.football.extensions

import android.widget.Toast
import vn.zenity.football.app.App

/**
 * Created by vinhdn on 01-Mar-18.
 */
fun toast(message: Any, length: Int = Toast.LENGTH_LONG) {
    when (message) {
        is String -> Toast.makeText(App.get(), message, length).show()
        is Int -> Toast.makeText(App.get(), message, length).show()
        else -> throw IllegalArgumentException("Argument message type is invalid. The first argument is only accepted on Int or String")
    }
}