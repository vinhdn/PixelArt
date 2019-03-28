package vn.zenity.football.extensions

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.AsyncTask
import android.os.Environment
import android.support.v4.content.FileProvider
import vn.zenity.football.app.App
import java.io.File
import java.io.FileOutputStream
import android.media.MediaScannerConnection


/**
 * Created by vinhdn on 06-Mar-18.
 */

fun Bitmap.saveImage(finishListener: ((String?) -> Unit)? = null) {
    SaveBitmapToFile(finishListener).execute(this)
}

class SaveBitmapToFile(var finishListener: ((String?) -> Unit)? = null) : AsyncTask<Bitmap, Void, String?>() {

    override fun doInBackground(vararg params: Bitmap): String? {
        if (params.isEmpty()) return null
        try {
            val fileName = "${System.currentTimeMillis()}.png"
            val file = File(checkAndCreateProjectDirs(), fileName)
            file.createNewFile()
            val out = FileOutputStream(file)
            val fullBitmap = Bitmap.createBitmap(params[0].width, params[0].height, Bitmap.Config.ARGB_8888)
            val c = Canvas(fullBitmap)
            c.drawColor(Color.WHITE)
            c.drawBitmap(params[0], 0f, 0f, null)
            fullBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            file.absolutePath.saveImageToGallery()
            return fileName
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(aVoid: String?) {
        vn.zenity.football.extensions.Tool.freeMemory()
        if (finishListener == null) {
            if (aVoid != null) {
                toast("Saved successfully")
            } else {
                toast("Cancel save this file")
            }
        }
        finishListener?.invoke(aVoid)
        super.onPostExecute(aVoid)
    }
}

fun String.saveImageToGallery() {
    MediaScannerConnection.scanFile(App.get(), arrayOf(this), null
    ) { _, _ -> }
}

fun checkAndCreateProjectDirs(): File {
    val path = Environment.getExternalStorageDirectory().path + "/WCPixelArt"
    val dirs = File(path)
    if (!dirs.exists()) {
        dirs.mkdirs()
    }
    return dirs
}

fun Bitmap.toGrayscale(alpha: Int = 255): Bitmap {
    val width: Int
    val height: Int
    height = this.height
    width = this.width

    val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmpGrayscale)
    val paint = Paint()
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    val f = ColorMatrixColorFilter(cm)
    paint.colorFilter = f
    paint.alpha = alpha
    c.drawBitmap(this, 0f, 0f, paint)
    return bmpGrayscale
}

fun Bitmap.share(context: Context) {
    this.saveImage {
        it?.let {
            val file = File(checkAndCreateProjectDirs(), it)
            val bitmapUri = FileProvider.getUriForFile(context, App.get().packageName + ".share", file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(Intent.EXTRA_STREAM, bitmapUri)
            intent.type = "image/jpeg"
            val iTShare = Intent.createChooser(intent, "Share")
            iTShare.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            App.get().startActivity(iTShare)
        }
    }
}

fun Int.colorIsDark() : Boolean {
    val darkness = 1 - (0.299 * Color.red(this) + 0.587 * Color.green(this) + 0.114 * Color.blue(this)) / 255
    return darkness >= 0.4
}