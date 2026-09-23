package com.pushblok.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.pushblok.app.databinding.ActivityExerciseBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExerciseActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXERCISE_TYPE = "extra_exercise_type"
        /** Ak sa cvičenie spúšťa z budíka: počet opakovaní ALEBO sekúnd, ktoré treba splniť. */
        const val EXTRA_REQUIRED_REPS = "extra_required_reps"
    }

    private lateinit var binding: ActivityExerciseBinding
    private lateinit var exerciseType: ExerciseType
    private lateinit var engine: ExerciseEngine
    private lateinit var voiceCoach: VoiceCoach
    private var requiredAmount: Int = 0
    private lateinit var cameraExecutor: ExecutorService
    private var creditsEarnedThisSession = 0
    private var lastFrameTimeMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseType = ExerciseType.valueOf(
            intent.getStringExtra(EXTRA_EXERCISE_TYPE) ?: ExerciseType.SQUATS.name
        )
        requiredAmount = intent.getIntExtra(EXTRA_REQUIRED_REPS, 0)
        engine = ExerciseEngine(exerciseType)
        voiceCoach = VoiceCoach(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.tvExerciseName.text = exerciseType.label
        binding.tvTip.text = exerciseType.placementTip
        updateCountText()

        binding.btnFinish.setOnClickListener { finishSession() }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val poseDetector = PoseDetection.getClient(
                AccuratePoseDetectorOptions.Builder()
                    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                    .build()
            )

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImage(imageProxy, poseDetector)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Kameru sa nepodarilo spustiť.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImage(
        imageProxy: ImageProxy,
        poseDetector: com.google.mlkit.vision.pose.PoseDetector
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        poseDetector.process(inputImage)
            .addOnSuccessListener { pose: Pose -> onPoseDetected(pose) }
            .addOnCompleteListener { imageProxy.close() }
    }

    private fun onPoseDetected(pose: Pose) {
        val now = System.currentTimeMillis()
        val dt = if (lastFrameTimeMs == 0L) 0L else (now - lastFrameTimeMs).coerceAtMost(500L)
        lastFrameTimeMs = now

        val result = engine.process(pose, dt)

        runOnUiThread {
            if (result.repCounted) {
                val credits = ExerciseSettings.getCreditsPerUnit(this, exerciseType)
                CreditManager.addCredits(this, credits)
                creditsEarnedThisSession += credits
                updateCountText()
                checkGoalReached()
            } else if (result.goodFormHeld) {
                updateCountText() // aktualizuj bežiaci čas výdrže
            }

            result.feedback?.let { msg ->
                binding.tvFeedback.text = msg
                voiceCoach.speak(msg)
            }
        }
    }

    private fun checkGoalReached() {
        if (requiredAmount <= 0) return
        val current = if (exerciseType.unit == ExerciseUnit.SECONDS)
            (engine.secondsHeld).toInt() else engine.repCount
        if (current >= requiredAmount) {
            Toast.makeText(this, "Splnené! Budík sa vypína.", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finishSession()
        }
    }

    private fun updateCountText() {
        val current = if (exerciseType.unit == ExerciseUnit.SECONDS)
            engine.secondsHeld.toInt() else engine.repCount
        val unitLabel = if (exerciseType.unit == ExerciseUnit.SECONDS) "s" else ""
        val target = if (requiredAmount > 0) " / $requiredAmount$unitLabel" else unitLabel
        binding.tvCount.text = "$current$target"
        binding.tvCreditsEarned.text = "Získané kredity: $creditsEarnedThisSession"
    }

    private fun finishSession() {
        val amount = if (exerciseType.unit == ExerciseUnit.SECONDS) engine.secondsHeld.toInt() else engine.repCount
        val unitWord = if (exerciseType.unit == ExerciseUnit.SECONDS) "sekúnd výdrže" else "opakovaní"
        Toast.makeText(
            this,
            "Hotovo! $amount $unitWord (+$creditsEarnedThisSession kr.)",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        voiceCoach.shutdown()
    }
}
