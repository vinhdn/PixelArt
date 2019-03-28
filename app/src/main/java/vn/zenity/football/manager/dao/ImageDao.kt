package vn.zenity.football.manager.dao

import android.arch.persistence.room.*
import vn.zenity.football.models.FinishedImage
import vn.zenity.football.models.ImagePixel

/**
 * Created by vinhdn on 04-Mar-18.
 */
@Dao
interface ImageDao {
    @Query("SELECT * FROM ImagePixel where savedData NOT NULL ORDER BY lastDraw DESC")
    fun getAllOffline(): List<ImagePixel>?

    @Query("SELECT * FROM ImagePixel where path = :name ORDER BY path DESC LIMIT 1")
    fun getImagePixelByName(name: String): ImagePixel?

    @Query("SELECT * FROM FinishedImage where savedData NOT NULL AND time NOT NULL ORDER BY time DESC")
    fun getFinished(): List<FinishedImage>?

    @Query("SELECT * FROM FinishedImage where path = :name ORDER BY path DESC LIMIT 1")
    fun getFinishImageByName(name: String): FinishedImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(image: ImagePixel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFinished(image: FinishedImage)

    @Delete
    fun delete(image: ImagePixel)

    @Delete
    fun deleteFinishedImage(image: FinishedImage)
}