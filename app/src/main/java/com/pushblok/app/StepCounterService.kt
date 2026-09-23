package com.pushblok.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Počíta kroky pomocou TYPE_STEP_COUNTER (kumulatívny počítadlo od bootu).
 * Každých STEPS_PER_CREDIT krokov = 1 kredit. Funguje úplne offline.
 */
class StepCounterService : Service(), SensorEventListener {

    companion object {
        const val STEPS_PER_CREDIT = 100 // 100 krokov = 1 kredit (nastaviteľné)
        const val CHANNEL_ID = "step_counter_channel"
        const val NOTIF_ID = 1
        private const val PREFS = "pushblok_steps"
        private const val KEY_BASELINE = "baseline_steps"
        private const val KEY_CREDITED_STEPS = "credited_steps"
        private const val KEY_DATE = "steps_date"
    }

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private fun prefs(): SharedPreferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent) {
        val totalStepsSinceBoot = event.values[0].toInt()
        val p = prefs()

        // Ak sa zmenil deň, resetujeme baseline (nové kroky sa počítajú od dneška)
        if (p.getString(KEY_DATE, null) != today()) {
            p.edit()
                .putInt(KEY_BASELINE, totalStepsSinceBoot)
                .putInt(KEY_CREDITED_STEPS, 0)
                .putString(KEY_DATE, today())
                .apply()
            return
        }

        val baseline = p.getInt(KEY_BASELINE, totalStepsSinceBoot)
        val stepsToday = (totalStepsSinceBoot - baseline).coerceAtLeast(0)
        val alreadyCredited = p.getInt(KEY_CREDITED_STEPS, 0)

        val newCreditableSteps = stepsToday - alreadyCredited
        if (newCreditableSteps >= STEPS_PER_CREDIT) {
            val creditsToAdd = newCreditableSteps / STEPS_PER_CREDIT
            CreditManager.addCredits(this, creditsToAdd)
            p.edit()
                .putInt(KEY_CREDITED_STEPS, alreadyCredited + creditsToAdd * STEPS_PER_CREDIT)
                .apply()
            updateNotification(stepsToday)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Krokomer PushBlok", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(stepsToday: Int = 0): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PushBlok sleduje kroky")
            .setContentText("Dnes: $stepsToday krokov")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

    private fun updateNotification(stepsToday: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(stepsToday))
    }
}
