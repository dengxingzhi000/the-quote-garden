package com.dailymind.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class DailySyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val syncRepo: SyncRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        syncRepo.syncAll()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object {
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<DailySyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("daily_sync", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
