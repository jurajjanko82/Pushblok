package com.pushblok.app

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.pushblok.app.databinding.ActivityAlarmSetBinding

class AlarmSetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmSetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmSetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exerciseNames = ExerciseType.values().map { it.label }
        binding.spinnerExercise.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, exerciseNames
        )

        // Načítanie uložených hodnôt
        binding.switchEnabled.isChecked = AlarmSettings.isEnabled(this)
        binding.timePicker.hour = AlarmSettings.getHour(this)
        binding.timePicker.minute = AlarmSettings.getMinute(this)
        binding.etRequiredReps.setText(AlarmSettings.getRequiredReps(this).toString())
        binding.etCreditCost.setText(AlarmSettings.getCreditCostToSkip(this).toString())
        val savedType = AlarmSettings.getExerciseType(this)
        binding.spinnerExercise.setSelection(ExerciseType.values().indexOf(savedType))

        binding.btnSave.setOnClickListener { saveAndSchedule() }
    }

    private fun saveAndSchedule() {
        val enabled = binding.switchEnabled.isChecked
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val reps = binding.etRequiredReps.text.toString().toIntOrNull() ?: 15
        val cost = binding.etCreditCost.text.toString().toIntOrNull() ?: 20
        val type = ExerciseType.values()[binding.spinnerExercise.selectedItemPosition]

        AlarmSettings.save(this, enabled, hour, minute, reps, type, cost)

        if (enabled) {
            AlarmReceiver.scheduleAlarm(this, hour, minute)
            android.widget.Toast.makeText(this, "Budík nastavený na $hour:$minute", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            AlarmReceiver.cancelAlarm(this)
            android.widget.Toast.makeText(this, "Budík vypnutý", android.widget.Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
