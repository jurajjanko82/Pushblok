package com.pushblok.app

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pushblok.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestRuntimePermissions()
        MidnightResetReceiver.scheduleNext(this)
        ensureStepServiceRunning()

        binding.btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }
        binding.btnSavings.setOnClickListener {
            startActivity(Intent(this, SavingsActivity::class.java))
        }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnDeviceAdmin.setOnClickListener { requestDeviceAdmin() }

        binding.btnExercises.setOnClickListener {
            startActivity(Intent(this, ExerciseSettingsActivity::class.java))
        }
        binding.btnAlarm.setOnClickListener {
            startActivity(Intent(this, AlarmSetActivity::class.java))
        }

        binding.switchWholePhone.setOnCheckedChangeListener { _, isChecked ->
            BlockManager.setWholePhoneBlock(this, isChecked)
        }

        binding.btnAddManualCredit.setOnClickListener {
            // Príklad: manuálne pridanie kreditov za "cvičenie" / kliky (tlačidlo v UI)
            CreditManager.addCredits(this, 1)
            refreshUi()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        binding.tvCredits.text = "Kredity: ${CreditManager.getCredits(this)} min"
        binding.tvSavings.text = "Vkladná knižka: ${CreditManager.getSavings(this)} kr."
        binding.switchWholePhone.isChecked = BlockManager.isWholePhoneBlockEnabled(this)
        binding.tvAccessibilityStatus.text =
            if (isAccessibilityServiceEnabled()) "Blokovanie appiek: AKTÍVNE"
            else "Blokovanie appiek: VYPNUTÉ (klikni na tlačidlo nižšie)"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${BlockerAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(service, ignoreCase = true)) return true
        }
        return false
    }

    private fun requestDeviceAdmin() {
        val compName = ComponentName(this, PhoneAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Potrebné na zamknutie celého telefónu, kým si neodomkneš čas kreditmi."
            )
        }
        startActivity(intent)
    }

    private fun ensureStepServiceRunning() {
        val intent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 42)
        }
    }
}
