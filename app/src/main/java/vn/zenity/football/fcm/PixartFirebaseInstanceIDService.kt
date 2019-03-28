package vn.zenity.football.fcm

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService

/**
 * Created by vinh on 1/2/18.
 */
class PixartFirebaseInstanceIDService: FirebaseInstanceIdService() {
    override fun onTokenRefresh() {
        val token = FirebaseInstanceId.getInstance().token
        token?.let {
            sendRegistrationToServer(token)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        Log.d("TOKEN FCM", token)
    }
}