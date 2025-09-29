package com.example.webchecker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.net.URL

class WebCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val CHANNEL_ID = "WebCheckerChannel"
    private var continuousToneThread: Thread? = null

    override fun doWork(): Result {
        val url = inputData.getString("url") ?: return Result.failure()
        val continuousAlarm = inputData.getBoolean("continuousAlarm", false)

        try {
            val content = URL(url).openConnection().getInputStream().bufferedReader().use { it.readText() }
            val prefs = applicationContext.getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)
            val lastContent = prefs.getString("lastContent", "")

            // Detecta mudança (ou força True para teste)
//            if (true) { // Troque para:
            if (!lastContent.isNullOrEmpty() && content != lastContent) {
                showNotification("Mudança detectada!", "O site monitorado foi alterado.")
                Log.d("WebCheckWorker", "Mudança detectada no site!")

                // Bipe
                if (continuousAlarm) {
                    startContinuousTone()
                } else {
                    val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
                }

                prefs.edit { putString("lastContent", content) }
            } else if (lastContent.isNullOrEmpty()) {
                prefs.edit { putString("lastContent", content) }
            }

        } catch (e: Exception) {
            Log.e("WebCheckWorker", "Erro ao verificar site: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificações do WebChecker",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notificações de alterações detectadas pelo WebChecker" }

            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun startContinuousTone() {
        if (continuousToneThread?.isAlive == true) return

        continuousToneThread = Thread {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            while (!Thread.currentThread().isInterrupted) {
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
                Thread.sleep(1200)
            }
            tone.release()
        }
        continuousToneThread?.start()
    }

    fun stopContinuousTone() {
        continuousToneThread?.interrupt()
    }
}
