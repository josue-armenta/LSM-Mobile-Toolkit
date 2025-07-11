package com.unade.lsm.data

import android.app.Application
import android.net.Uri
import android.util.Log

import com.unade.lsm.database.Db
import com.unade.lsm.database.models.SamplesFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class Repository(
    private val applicationContext: Application,
    private val db: Db = Db.getInstance(applicationContext),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getSamplesFiles(): Flow<List<SamplesFile>> =
        db.samplesFileDao.loadAllFiles()

    suspend fun countBatchesBy(signIndex: Int): Int {
        return withContext(dispatcher) {
            db.samplesFileDao.countBatchesBy("${signIndex}_%")
        }
    }

    suspend fun insertSamplesFile(filename: String) {
        withContext(dispatcher) {
            db.samplesFileDao.insert(
                SamplesFile(filename)
            )
        }
    }
}