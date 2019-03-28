package vn.zenity.football.manager

import android.arch.persistence.room.Room
import vn.zenity.football.app.App


/**
 * Created by vinhdn on 04-Mar-18.
 */
class PixelDB {
    companion object {
        var instance: PixelDB? = null
        fun getInstance() : AppDatabase {
            if (instance == null) {
                instance = PixelDB()
            }
            return instance!!.appDatabase
        }
    }

    private lateinit var appDatabase: AppDatabase

    init {
        val context = App.get()
        //Intiliaze the room database with database name
        appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "pixel.sqlite")
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
    }

    private fun getAppDatabase(): AppDatabase {
        return appDatabase
    }
}