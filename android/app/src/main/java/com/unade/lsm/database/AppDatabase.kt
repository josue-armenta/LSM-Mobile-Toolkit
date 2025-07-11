package com.unade.lsm.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.unade.lsm.database.models.SamplesFile

@Database(entities = [SamplesFile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun samplesFileDao(): SamplesFileDao
}