package vn.zenity.football.extensions

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import vn.zenity.football.app.App
import vn.zenity.football.manager.PixelDB
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.InputStream

/**
 * Created by vinh on 3/23/18.
 */

fun String.loadBitmapToShare(delegate: (Bitmap?) -> Unit) {
    doAsync {
        var ims: InputStream? = null
        try {
//            ims = App.get().assets.open("resource/${this@loadBitmapAsset}")
            ims = App.get().assets.open("images/${this@loadBitmapToShare}")
            // load image as Drawable
//            val d = Drawable.createFromStream(ims, null)
            val bitmap = BitmapFactory.decodeStream(ims).toGrayscale((0.6f * 255).toInt())
            if (bitmap.width <= 0) {
                ims?.close()
                delegate.invoke(null)
                return@doAsync
            }
            // set image to ImageView
            ims.close()
            var w = Resources.getSystem().displayMetrics.widthPixels
            if (w <= 0) w = 1024
            val h = ((w * bitmap.height).toFloat() / bitmap.width.toFloat()).toInt()
            val imp = PixelDB.getInstance().imageDao().getImagePixelByName(this@loadBitmapToShare)
            var colorBitmap: ByteArray = byteArrayOf()
            imp?.let { impLet ->
                colorBitmap = impLet.bitmap ?: return@let
            }
            val fullBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(fullBitmap)
            c.drawBitmap(bitmap, 0f, 0f, null)
            if (colorBitmap.isNotEmpty()) {
                val bb = BitmapFactory.decodeByteArray(colorBitmap, 0, colorBitmap.size)
                c.drawBitmap(bb, 0f, 0f, null)
            }
            val bitmapShare = Bitmap.createScaledBitmap(fullBitmap, w, h, false)
            uiThread {
                //                setImageDrawable(d)
                delegate.invoke(bitmapShare)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            ims?.close()
            delegate.invoke(null)
        }
    }
}