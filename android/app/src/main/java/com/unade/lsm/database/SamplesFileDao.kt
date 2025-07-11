package com.unade.lsm.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.unade.lsm.database.models.SamplesFile
import kotlinx.coroutines.flow.Flow

@Dao
interface SamplesFileDao {

    @Query("SELECT * FROM samples_file ORDER BY uploaded ASC")
    fun loadAllFiles(): Flow<List<SamplesFile>>

    @Query("SELECT COUNT(*) FROM samples_file WHERE name LIKE :search")
    fun countBatchesBy(search: String): Int

    @Insert
    fun insert(entry: SamplesFile)

    @Update
    fun update(entry: SamplesFile)

    @Delete
    fun delete(entry: SamplesFile)

}