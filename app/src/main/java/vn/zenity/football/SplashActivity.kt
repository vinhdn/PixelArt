package vn.zenity.football

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import vn.zenity.football.app.App
import vn.zenity.football.tools.LoadDataAsyn

/**
 * Created by vinhdn on 01-Mar-18.
 */

class SplashActivity: AppCompatActivity() {
    private var handler: Handler? = null
    private var isDelayFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        runAnimation()

        handler = Handler()
        handler!!.postDelayed({if (App.get().data == null) runAnimation()}, 2000L)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            LoadDataAsyn(assets) {
                if (isDelayFinish) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }.execute()
            handler!!.postDelayed({
                isDelayFinish = true
                if (App.get().data == null) return@postDelayed
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }, 2000L)
        }
        else
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0x456)
    }

    private fun runAnimation() {
        (findViewById<View>(R.id.tv) as View).animate().alpha(1f).scaleY(1.1f).scaleX(1.1f).setDuration(2000L).interpolator = AccelerateDecelerateInterpolator()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 0x456) {
            for (i in grantResults.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    handler!!.postDelayed({ recreate() }, 1000)
                    return
                }
            }
            LoadDataAsyn(assets) {
                if (isDelayFinish) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }.execute()
            handler!!.postDelayed({
                isDelayFinish = true
                if (App.get().data == null) return@postDelayed
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }, 2000L)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}