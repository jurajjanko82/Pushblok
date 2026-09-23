package com.pushblok.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pushblok.app.databinding.ActivityAlarmDismissBinding

class AlarmDismissActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmDismissBinding
    private val exerciseRequestCode = 55

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmDismissBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Zobrazí sa aj nad uzamknutou obrazovkou
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val requiredReps = AlarmSettings.getRequiredReps(this)
        val exerciseType = AlarmSettings.getExerciseType(this)
        val creditCost = AlarmSettings.getCreditCostToSkip(this)

        binding.tvInfo.text =
            "Budík zvoní!\n\nSpravi $requiredReps × ${exerciseType.label}\nalebo zaplať $creditCost kreditov."

        binding.btnExercise.text = "Cvičiť (${exerciseType.label})"
        binding.btnExercise.setOnClickListener {
            val intent = Intent(this, ExerciseActivity::class.java).apply {
                putExtra(ExerciseActivity.EXTRA_EXERCISE_TYPE, exerciseType.name)
                putExtra(ExerciseActivity.EXTRA_REQUIRED_REPS, requiredReps)
            }
            startActivityForResult(intent, exerciseRequestCode)
        }

        binding.btnPayCredits.text = "Zaplatiť $creditCost kreditov"
        binding.btnPayCredits.setOnClickListener {
            if (CreditManager.spendCredits(this, creditCost)) {
                dismissAlarm()
            } else {
                Toast.makeText(this, "Nemáš dosť kreditov.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exerciseRequestCode && resultCode == Activity.RESULT_OK) {
            dismissAlarm()
        }
    }

    private fun dismissAlarm() {
        AlarmSoundService.stop(this)
        Toast.makeText(this, "Budík vypnutý.", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onBackPressed() {
        // Budík sa nedá vypnúť tlačidlom späť - musí sa splniť cvičenie alebo zaplatiť.
    }
}
