package vn.zenity.football.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Created by vinhdn on 01-Mar-18.
 */
@Entity(tableName = "ImagePixel")
data class ImagePixel(
        var tag: Int?,
        var enable: Boolean?,
        @PrimaryKey
        @SerializedName("name")
        var path: String,
        var savedData: String?,
        var isFavorite: Boolean?,
        @SerializedName("unlock_coin")
        var unlockCoin: Int?,
        @SerializedName("complete_coin")
        var completeCoin: Int?,
        var isUnlocked: Boolean? = false,
        var lastDraw: Long? = 0L) {

    var bitmap: ByteArray? = null
    fun getDrawBitmap(): Bitmap? {
        bitmap?.let {
            return BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (other is ImagePixel)
            return other.path == this.path
        return false
    }

    override fun hashCode(): Int {
        var result = tag ?: 0
        result = 31 * result + (enable?.hashCode() ?: 0)
        result = 31 * result + path.hashCode()
        result = 31 * result + (savedData?.hashCode() ?: 0)
        result = 31 * result + (isFavorite?.hashCode() ?: 0)
        result = 31 * result + (unlockCoin ?: 0)
        result = 31 * result + (completeCoin ?: 0)
        result = 31 * result + (isUnlocked?.hashCode() ?: 0)
        result = 31 * result + (lastDraw?.hashCode() ?: 0)
        result = 31 * result + (bitmap?.let { Arrays.hashCode(it) } ?: 0)
        return result
    }
}

@Entity(tableName = "FinishedImage")
data class FinishedImage(
        @PrimaryKey(autoGenerate = true)
        var id: Int?,
        var path: String?,
        var savedData: String?,
        var time: Long?) {
    var bitmap: ByteArray? = null
    fun getDrawBitmap(): Bitmap? {
        bitmap?.let {
            return BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        return null
    }

    public constructor(image: ImagePixel) : this(null, image.path, image.savedData, System.currentTimeMillis()) {
        this.bitmap = image.bitmap
    }
}

data class Category(var tag: Int,
                    var name: String,
                    var images: ArrayList<ImagePixel>? = null,
                    var prvSize: Int = 0) {

    override fun equals(other: Any?): Boolean {
        if (other is Category)
            return this.name == other.name
        return false
    }

    override fun hashCode(): Int {
        var result = tag
        result = 31 * result + name.hashCode()
        result = 31 * result + (images?.hashCode() ?: 0)
        return result
    }
}

data class DataCollection(var version: String,
                          var data: List<Category>?)