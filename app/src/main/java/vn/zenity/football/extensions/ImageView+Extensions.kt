package vn.zenity.football.extensions

import android.content.res.ColorStateList
import android.graphics.*
import android.widget.ImageView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.widget.ImageViewCompat
import vn.zenity.football.app.App
import vn.zenity.football.manager.PixelDB
import vn.zenity.football.models.ImagePixel
import java.io.InputStream
import java.lang.ref.WeakReference


/**
 * Created by vinhdn on 01-Mar-18.
 */
fun ImageView.loadAsset(path: String, isFullColor: Boolean = false, oldBitmap: ByteArray? = null, isForceUpdateOldBitmap: Boolean = false, delegateGetSavedBitmap: ((ImagePixel) -> Unit)? = null) {
    doAsync {
        var ims: InputStream? = null
        try {
//            ims = App.get().assets.open("resource/$path")
            ims = App.get().assets.open("images/$path")
            // load image as Drawable
//            val d = Drawable.createFromStream(ims, null)
            val bitmap = if (isFullColor) BitmapFactory.decodeStream(ims) else BitmapFactory.decodeStream(ims).toGrayscale((0.8f * 255).toInt())

            var colorBitmap = WeakReference(oldBitmap)
            if (isForceUpdateOldBitmap || colorBitmap.get() == null) {
                if (delegateGetSavedBitmap != null) {
                    val imp = PixelDB.getInstance().imageDao().getImagePixelByName(path)
                    imp?.let { impLet ->
                        colorBitmap = WeakReference(impLet.bitmap)
                        uiThread {
                            delegateGetSavedBitmap.invoke(impLet)
                        }
                    }
                }
            }

            if (colorBitmap.get() != null) {
                val fullBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val c = Canvas(fullBitmap)
                c.drawBitmap(bitmap, 0f, 0f, null)
                val bb = BitmapFactory.decodeByteArray(colorBitmap.get(), 0, colorBitmap.get()?.size
                        ?: 0)
                c.drawBitmap(bb, 0f, 0f, null)
                ims.close()

                uiThread {
                    //                setImageDrawable(d)
                    post {
                        val dipy = App.get().resources.displayMetrics.density
                        var w = (128 * dipy).toInt()
                        if (w <= width ?: 0) {
                            w = width ?: w
                        }
                        val h = ((bitmap.height * w.toFloat()) / ((if (bitmap.width <= 0) 1 else bitmap.width).toFloat())).toInt()
                        setImageBitmap(Bitmap.createScaledBitmap(fullBitmap, w, h, false))
                    }
                }
                return@doAsync
            }
            // set image to ImageView
            ims.close()
            uiThread {
                //                setImageDrawable(d)
                post {
                    val dipy = App.get().resources.displayMetrics.density
                    var w = (128 * dipy).toInt()
                    if (w <= width) {
                        w = width
                    }
                    val h = ((bitmap.height * w.toFloat()) / ((if (bitmap.width <= 0) 1 else bitmap.width).toFloat())).toInt()
                    setImageBitmap(Bitmap.createScaledBitmap(bitmap, w, h, false))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            ims?.close()
        }
    }
}

fun String.loadBitmapAsset(isFullColor: Boolean = false, alpha: Float = 1f, delegate: (Bitmap?) -> Unit) {
    doAsync {
        var ims: InputStream? = null
        try {
//            ims = App.get().assets.open("resource/${this@loadBitmapAsset}")
            ims = App.get().assets.open("images/${this@loadBitmapAsset}")
            // load image as Drawable
//            val d = Drawable.createFromStream(ims, null)
            val bitmap = if (isFullColor) BitmapFactory.decodeStream(ims) else BitmapFactory.decodeStream(ims).toGrayscale(alpha = (alpha * 255).toInt())
            // set image to ImageView
            ims.close()
            val dipy = App.get().resources.displayMetrics.density
            uiThread {
                //                setImageDrawable(d)
                delegate.invoke(bitmap)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            ims?.close()
            delegate.invoke(null)
        }
    }
}

fun freeMemory() {
    System.runFinalization()
    Runtime.getRuntime().gc()
    System.gc()
}

fun ImageView.setTint(color: Int) {
//    this.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP)
//    this.drawable?.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP)
    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(color))
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//        this.drawable?.setTint(color)
//    }else {
//    }
    if (drawable != null) {
        DrawableCompat.setTint(this.drawable, color)
        DrawableCompat.setTintMode(this.drawable, PorterDuff.Mode.MULTIPLY)
    }
}