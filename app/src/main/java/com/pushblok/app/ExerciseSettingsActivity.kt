package com.pushblok.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushblok.app.databinding.ActivityExerciseSettingsBinding

class ExerciseSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseSettingsBinding
    private var pendingExercise: ExerciseType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = ExerciseListAdapter(
            onStart = { startExercise(it) },
            onCreditsChanged = { type, credits -> ExerciseSettings.setCreditsPerUnit(this, type, credits) },
            getCredits = { ExerciseSettings.getCreditsPerUnit(this, it) }
        )
    }

    private fun startExercise(type: ExerciseType) {
        pendingExercise = type
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 77)
        } else {
            launchExercise(type)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            pendingExercise?.let { launchExercise(it) }
        }
    }

    private fun launchExercise(type: ExerciseType) {
        val intent = Intent(this, ExerciseActivity::class.java)
        intent.putExtra(ExerciseActivity.EXTRA_EXERCISE_TYPE, type.name)
        startActivity(intent)
    }
}
