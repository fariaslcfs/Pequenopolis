package com.example.webchecker

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var etInterval: EditText
    private lateinit var cbContinuousAlarm: CheckBox
    private lateinit var btnStartStop: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.edtUrl)
        etInterval = findViewById(R.id.edtInterval)
        cbContinuousAlarm = findViewById(R.id.chkContinuousBeep)
        btnStartStop = findViewById(R.id.btnStartStop)

        val prefs = getSharedPreferences("WebCheckerPrefs", Context.MODE_PRIVATE)
        val defaultUrl = "https://www.sjc.sp.gov.br/servicos/educacao-e-cidadania/educacao-infantil/"
        val defaultInterval = 15
        val defaultContinuous = false

        etUrl.setText(prefs.getString("url", defaultUrl))
        etInterval.setText(prefs.getInt("interval", defaultInterval).toString())
        cbContinuousAlarm.isChecked = prefs.getBoolean("continuousAlarm", defaultContinuous)

        updateButtonText()

        btnStartStop.setOnClickListener {
            val workManager = WorkManager.getInstance(this)
            val workInfos = workManager.getWorkInfosForUniqueWork("WebCheckWorker").get()
            val isMonitoring = workInfos.any { it.state.isFinished.not() }

            if (isMonitoring) {
                // Parar monitoramento
                stopMonitoring()
                Toast.makeText(this, "Monitoramento parado", Toast.LENGTH_SHORT).show()
            } else {
                // Iniciar monitoramento
                val url = etUrl.text.toString().ifEmpty { defaultUrl }
                val interval = etInterval.text.toString().toIntOrNull() ?: defaultInterval
                val continuous = cbContinuousAlarm.isChecked

                prefs.edit {
                    putString("url", url)
                    putInt("interval", interval)
                    putBoolean("continuousAlarm", continuous)
                }

                startMonitoring(url, interval, continuous)
                Toast.makeText(this, "Monitorando $url a cada $interval min", Toast.LENGTH_SHORT).show()
            }

            updateButtonText()
        }
    }

    private fun startMonitoring(url: String, intervalMinutes: Int, continuous: Boolean) {
        val data = Data.Builder()
            .putString("url", url)
            .putBoolean("continuousAlarm", continuous)
            .build()

        val work = PeriodicWorkRequestBuilder<WebCheckWorker>(
            intervalMinutes.toLong().coerceAtLeast(15), TimeUnit.MINUTES
        ).setInputData(data).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WebCheckWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            work
        )
    }

    private fun stopMonitoring() {
        WorkManager.getInstance(this).cancelUniqueWork("WebCheckWorker")
    }

    private fun updateButtonText() {
        val workManager = WorkManager.getInstance(this)
        val workInfos = workManager.getWorkInfosForUniqueWork("WebCheckWorker").get()
        val isMonitoring = workInfos.any { it.state.isFinished.not() }

        btnStartStop.text = if (isMonitoring) "Parar monitoração" else "Iniciar monitoração"
    }
}
