package com.example.webchecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)
            val url = prefs.getString("url", "https://www.sjc.sp.gov.br/servicos/educacao-e-cidadania/educacao-infantil/") ?: return
            val interval = prefs.getInt("interval", 15)
            val continuous = prefs.getBoolean("continuousAlarm", false)

            val data = Data.Builder()
                .putString("url", url)
                .putBoolean("continuousAlarm", continuous)
                .build()

            val work = PeriodicWorkRequestBuilder<WebCheckWorker>(
                interval.toLong().coerceAtLeast(15), TimeUnit.MINUTES
            ).setInputData(data).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "WebCheckWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                work
            )
        }
    }
}
