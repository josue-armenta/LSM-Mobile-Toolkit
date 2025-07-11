package com.unade.lsm

import android.app.Application
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.unade.lsm.services.CoroutineUploadWorker
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val uploadWorkRequest: WorkRequest =
            PeriodicWorkRequestBuilder<CoroutineUploadWorker>(
                15, TimeUnit.MINUTES
            ).build()
        WorkManager.getInstance(this).enqueue(uploadWorkRequest)
    }
}