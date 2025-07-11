package com.unade.lsm.database

import android.app.Application
import androidx.room.Room

class Db private constructor(applicationContext: Application) {

    var samplesFileDao: SamplesFileDao

    init {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        ).build()
        samplesFileDao = db.samplesFileDao()
    }

    companion object {
        @Volatile
        private var instance: Db? = null

        fun getInstance(applicationContext: Application) =
            instance ?: synchronized(this) {
                instance ?: Db(applicationContext).also { instance = it }
            }
    }
}