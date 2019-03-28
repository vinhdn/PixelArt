package vn.zenity.football.app

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.annotation.Nullable
import android.support.annotation.RequiresApi
import android.support.multidex.MultiDexApplication
import vn.zenity.football.R
import vn.zenity.football.models.Category
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId

/**
 * Created by vinhdn on 01-Mar-18.
 */
class App : MultiDexApplication() {

    var data: ArrayList<Category>? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)
        val gcmToken = FirebaseInstanceId.getInstance().token

    }

    companion object {

        private lateinit var instance: App
        fun get(): App {
            return instance
        }
    }

    private var customResources: Resources? = null

    override fun getResources(): Resources {
        if (customResources == null) {
            customResources = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                ResourcesForSupportFirebaseNotificationsOnAndroid8(super.getResources())
            } else {
                super.getResources()
            }
        }
        return customResources!!
    }
}

class ResourcesForSupportFirebaseNotificationsOnAndroid8(private val resources: Resources) : Resources(resources.assets, resources.displayMetrics, resources.configuration) {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(Resources.NotFoundException::class)
    override fun getDrawable(id: Int, @Nullable theme: Resources.Theme?): Drawable {
        if (id == R.drawable.ic_launcher_small) {
            val drawable = resources.getDrawable(id, theme)
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            return drawable
        } else {
            return resources.getDrawable(id, theme)
        }
    }
}