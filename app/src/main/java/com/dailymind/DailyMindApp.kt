package com.dailymind

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dailymind.core.data.SeedImporter
import com.dailymind.sync.DailySyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DailyMindApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var seedImporter: SeedImporter

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        DailySyncWorker.enqueue(this)
        DailySyncWorker.enqueueImmediate(this)
        appScope.launch {
            runCatching {
                val json = assets.open("quotes_seed.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
                seedImporter.importIfEmpty(json)
            }
        }
    }
}
