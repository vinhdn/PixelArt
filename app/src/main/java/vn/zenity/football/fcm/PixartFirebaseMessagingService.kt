package vn.zenity.football.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationManager
import android.media.RingtoneManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.app.NotificationChannel
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.app.NotificationCompat.BADGE_ICON_LARGE
import com.google.gson.Gson
import vn.zenity.football.MainActivity
import vn.zenity.football.R
import vn.zenity.football.app.App


/**
 * Created by vinh on 1/2/18.
 */
class PixartFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage?) {
        message?.notification?.body?.let {
            var data: String? = null
            message.data?.let {
                data = Gson().toJson(it)
            }
            sendNotification(message.notification?.title
                    ?: App.get().getString(R.string.app_name), it, data)
        }
    }

    private fun sendNotification(title: String = App.get().getString(R.string.app_name), messageBody: String, data: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        data?.let {
            intent.putExtra("data", data)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                .setSmallIcon(R.drawable.ic_launcher_small)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setBadgeIconType(BADGE_ICON_LARGE)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val channelName = getString(R.string.default_notification_channel_name)
            val chanel = NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_DEFAULT)
            chanel.description = messageBody
            chanel.enableVibration(true)
            chanel.enableLights(true)
            notificationManager.createNotificationChannel(chanel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

}