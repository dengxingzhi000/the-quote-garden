package com.quotegarden

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.quotegarden.core.datastore.TranslationPatcher
import com.quotegarden.sync.DailySyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class QuoteGardenApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var translationPatcher: TranslationPatcher

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        translationPatcher.ensureApplied()
        DailySyncWorker.enqueue(this)
        DailySyncWorker.enqueueImmediate(this)
    }
}
