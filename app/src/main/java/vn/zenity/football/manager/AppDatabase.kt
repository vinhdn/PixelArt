package vn.zenity.football.manager

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import vn.zenity.football.manager.dao.ImageDao
import vn.zenity.football.models.FinishedImage
import vn.zenity.football.models.ImagePixel

/**
 * Created by vinhdn on 04-Mar-18.
 */
@Database(entities = [ImagePixel::class, FinishedImage::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun imageDao(): ImageDao
}