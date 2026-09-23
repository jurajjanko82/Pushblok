package com.pushblok.app

import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/** Výsledok spracovania jedného snímku z kamery. */
data class FrameResult(
    val repCounted: Boolean = false,
    /** Pri SECONDS cvikoch: koľko sekúnd (spravidla 0 alebo malý zlomok) sa pripočítalo v tomto snímku. */
    val goodFormHeld: Boolean = false,
    /** Krátka správa pre hlas/text (napr. "Choď nižšie", "Super forma!"). Môže byť null. */
    val feedback: String? = null,
    /** True = pozitívna spätná väzba (pochvala), false = korekcia techniky. */
    val isPositive: Boolean = false
)

/**
 * Jeden engine na cvik podľa jeho ExerciseType. Vytvor nový pri každom spustení cvičenia.
 */
class ExerciseEngine(private val type: ExerciseType) {

    var repCount: Int = 0
        private set
    var secondsHeld: Double = 0.0
        private set

    private enum class State { UP, DOWN }
    private var state = State.UP
    private var toggleOpen = false
    private var lastStateChangeMs = 0L
    private var repsSinceLastPraise = 0

    // pre CALF_RAISES kalibráciu
    private var calibratedBaseline: Double? = null

    fun process(pose: Pose, dtMillis: Long): FrameResult {
        return when (type) {
            ExerciseType.SQUATS -> angleCycle(pose, hip(), knee(), ankle(), 100.0, 155.0,
                formCheck = { backLeanCheck(pose) })
            ExerciseType.JUMP_SQUATS -> angleCycle(pose, hip(), knee(), ankle(), 95.0, 158.0)
            ExerciseType.LUNGES -> angleCycleMinSide(pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, 100.0, 160.0)
            ExerciseType.SIDE_LUNGES -> angleCycleMinSide(pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE, 110.0, 160.0)
            ExerciseType.CALF_RAISES -> calfRaise(pose)
            ExerciseType.WALL_SIT -> timedHold(dtMillis) { angleOk(pose, hip(), knee(), ankle(), 75.0, 105.0) }
            ExerciseType.HIGH_KNEES -> toggle(kneeAboveHip(pose))

            ExerciseType.PLANK -> timedHold(dtMillis) { bodyLineOk(pose) }
            ExerciseType.SIDE_PLANK_LEFT -> timedHold(dtMillis) { bodyLineOk(pose) }
            ExerciseType.SIDE_PLANK_RIGHT -> timedHold(dtMillis) { bodyLineOk(pose) }
            ExerciseType.CRUNCHES -> angleCycle(pose, shoulder(), hip(), knee(), 75.0, 110.0)
            ExerciseType.BICYCLE_CRUNCHES -> toggle(elbowNearOppositeKnee(pose))
            ExerciseType.LEG_RAISES -> angleCycle(pose, shoulder(), hip(), ankle(), 100.0, 160.0)
            ExerciseType.MOUNTAIN_CLIMBERS -> toggle(kneeNearChest(pose))

            ExerciseType.PUSHUPS -> angleCycle(pose, shoulder(), elbow(), wrist(), 95.0, 150.0,
                formCheck = { bodyLineOk(pose).let { if (!it) "Drž telo v rovnej línii" else null } })
            ExerciseType.DIAMOND_PUSHUPS -> angleCycle(pose, shoulder(), elbow(), wrist(), 90.0, 150.0)
            ExerciseType.TRICEP_DIPS -> angleCycle(pose, shoulder(), elbow(), wrist(), 90.0, 155.0)
            ExerciseType.PLANK_UPS -> angleCycle(pose, shoulder(), elbow(), wrist(), 100.0, 160.0)
            ExerciseType.SHOULDER_TAPS -> toggle(handNearOppositeShoulder(pose))
            ExerciseType.ARM_CIRCLES -> timedHold(dtMillis) { armsExtended(pose) }

            ExerciseType.JUMPING_JACKS -> toggle(armsUpLegsApart(pose))
            ExerciseType.BURPEES -> angleCycle(pose, hip(), knee(), ankle(), 90.0, 160.0)
        }
    }

    // ---------- Generický cyklus podľa uhla (drep, klik...) ----------

    private fun angleCycle(
        pose: Pose, a: Int, b: Int, c: Int,
        downThreshold: Double, upThreshold: Double,
        formCheck: (() -> String?)? = null
    ): FrameResult {
        val angle = angleBetween(pose, a, b, c) ?: return FrameResult()
        var rep = false
        var feedback: String? = null
        var positive = false

        when (state) {
            State.UP -> if (angle < downThreshold) state = State.DOWN
            State.DOWN -> if (angle > upThreshold) {
                state = State.UP
                repCount++
                rep = true
                repsSinceLastPraise++
                if (repsSinceLastPraise >= 5) {
                    feedback = "Super forma, pokračuj!"
                    positive = true
                    repsSinceLastPraise = 0
                }
            }
        }

        // ak zostane "zaseknutý" v DOWN a nedosiahne dostatočnú hĺbku, netreba nič - jednoducho sa rep nezapočíta
        if (!rep && formCheck != null) {
            val warning = formCheck()
            if (warning != null) feedback = warning
        }

        return FrameResult(repCounted = rep, feedback = feedback, isPositive = positive)
    }

    /** Ako angleCycle, ale vyberie tú stranu (ľavú/pravú), ktorá má momentálne menší uhol (napr. predná noha pri výpade). */
    private fun angleCycleMinSide(
        pose: Pose,
        lA: Int, lB: Int, lC: Int, rA: Int, rB: Int, rC: Int,
        downThreshold: Double, upThreshold: Double
    ): FrameResult {
        val left = angleBetween(pose, lA, lB, lC)
        val right = angleBetween(pose, rA, rB, rC)
        val angle = when {
            left != null && right != null -> minOf(left, right)
            left != null -> left
            right != null -> right
            else -> return FrameResult()
        }
        var rep = false
        when (state) {
            State.UP -> if (angle < downThreshold) state = State.DOWN
            State.DOWN -> if (angle > upThreshold) {
                state = State.UP
                repCount++
                rep = true
            }
        }
        return FrameResult(repCounted = rep)
    }

    // ---------- Striedavé cviky (jumping jacks, high knees, mountain climbers...) ----------

    private fun toggle(conditionMet: Boolean): FrameResult {
        var rep = false
        if (conditionMet && !toggleOpen) {
            toggleOpen = true
        } else if (!conditionMet && toggleOpen) {
            toggleOpen = false
            repCount++
            rep = true
        }
        return FrameResult(repCounted = rep)
    }

    // ---------- Výdrže (plank, wall sit...) ----------

    private fun timedHold(dtMillis: Long, formOk: () -> Boolean): FrameResult {
        val ok = formOk()
        return if (ok) {
            secondsHeld += dtMillis / 1000.0
            val wholeUnitsNow = (secondsHeld / ExerciseSettings.SECONDS_PER_CREDIT_UNIT).toInt()
            val wholeUnitsBefore = ((secondsHeld - dtMillis / 1000.0) / ExerciseSettings.SECONDS_PER_CREDIT_UNIT).toInt()
            FrameResult(goodFormHeld = true, repCounted = wholeUnitsNow > wholeUnitsBefore)
        } else {
            FrameResult(feedback = "Uprav polohu, telo drž rovno")
        }
    }

    // ---------- Výpony na špičky (kalibrácia) ----------

    private fun calfRaise(pose: Pose): FrameResult {
        val ankle = landmark(pose, PoseLandmark.LEFT_ANKLE) ?: landmark(pose, PoseLandmark.RIGHT_ANKLE) ?: return FrameResult()
        val hip = landmark(pose, PoseLandmark.LEFT_HIP) ?: landmark(pose, PoseLandmark.RIGHT_HIP) ?: return FrameResult()
        val bodyScale = abs(hip.y - ankle.y).toDouble().coerceAtLeast(0.01)

        if (calibratedBaseline == null) {
            calibratedBaseline = ankle.y.toDouble()
            return FrameResult()
        }
        val delta = calibratedBaseline!! - ankle.y // kladné = pata sa zdvihla (ankle.y sa zmenšilo)
        val threshold = bodyScale * 0.03

        var rep = false
        when (state) {
            State.UP -> if (delta > threshold) state = State.DOWN // "DOWN" tu = zdvihnutá päta
            State.DOWN -> if (delta < threshold * 0.3) {
                state = State.UP
                repCount++
                rep = true
            }
        }
        return FrameResult(repCounted = rep)
    }

    // ---------- Pomocné podmienky pre konkrétne cviky ----------

    private fun kneeAboveHip(pose: Pose): Boolean {
        val lKnee = landmark(pose, PoseLandmark.LEFT_KNEE)
        val rKnee = landmark(pose, PoseLandmark.RIGHT_KNEE)
        val lHip = landmark(pose, PoseLandmark.LEFT_HIP)
        val rHip = landmark(pose, PoseLandmark.RIGHT_HIP)
        if (lKnee == null || rKnee == null || lHip == null || rHip == null) return false
        val hipY = (lHip.y + rHip.y) / 2
        return lKnee.y < hipY || rKnee.y < hipY
    }

    private fun elbowNearOppositeKnee(pose: Pose): Boolean {
        val lElbow = landmark(pose, PoseLandmark.LEFT_ELBOW)
        val rElbow = landmark(pose, PoseLandmark.RIGHT_ELBOW)
        val lKnee = landmark(pose, PoseLandmark.LEFT_KNEE)
        val rKnee = landmark(pose, PoseLandmark.RIGHT_KNEE)
        val scale = bodyScale(pose) ?: return false
        if (lElbow == null || rElbow == null || lKnee == null || rKnee == null) return false
        val d1 = dist2D(lElbow, rKnee)
        val d2 = dist2D(rElbow, lKnee)
        return d1 < scale * 0.6 || d2 < scale * 0.6
    }

    private fun kneeNearChest(pose: Pose): Boolean {
        val lKnee = landmark(pose, PoseLandmark.LEFT_KNEE)
        val rKnee = landmark(pose, PoseLandmark.RIGHT_KNEE)
        val lShoulder = landmark(pose, PoseLandmark.LEFT_SHOULDER)
        val rShoulder = landmark(pose, PoseLandmark.RIGHT_SHOULDER)
        val scale = bodyScale(pose) ?: return false
        if (lKnee == null || rKnee == null || lShoulder == null || rShoulder == null) return false
        val d1 = dist2D(lKnee, lShoulder)
        val d2 = dist2D(rKnee, rShoulder)
        return d1 < scale * 1.1 || d2 < scale * 1.1
    }

    private fun handNearOppositeShoulder(pose: Pose): Boolean {
        val lWrist = landmark(pose, PoseLandmark.LEFT_WRIST)
        val rWrist = landmark(pose, PoseLandmark.RIGHT_WRIST)
        val lShoulder = landmark(pose, PoseLandmark.LEFT_SHOULDER)
        val rShoulder = landmark(pose, PoseLandmark.RIGHT_SHOULDER)
        val scale = bodyScale(pose) ?: return false
        if (lWrist == null || rWrist == null || lShoulder == null || rShoulder == null) return false
        val d1 = dist2D(lWrist, rShoulder)
        val d2 = dist2D(rWrist, lShoulder)
        return d1 < scale * 0.5 || d2 < scale * 0.5
    }

    private fun armsUpLegsApart(pose: Pose): Boolean {
        val lWrist = landmark(pose, PoseLandmark.LEFT_WRIST) ?: return false
        val rWrist = landmark(pose, PoseLandmark.RIGHT_WRIST) ?: return false
        val lShoulder = landmark(pose, PoseLandmark.LEFT_SHOULDER) ?: return false
        val rShoulder = landmark(pose, PoseLandmark.RIGHT_SHOULDER) ?: return false
        val lAnkle = landmark(pose, PoseLandmark.LEFT_ANKLE) ?: return false
        val rAnkle = landmark(pose, PoseLandmark.RIGHT_ANKLE) ?: return false

        val armsUp = lWrist.y < lShoulder.y && rWrist.y < rShoulder.y
        val shoulderWidth = abs(lShoulder.x - rShoulder.x)
        val ankleSpread = abs(lAnkle.x - rAnkle.x)
        val legsApart = shoulderWidth > 0 && ankleSpread > shoulderWidth * 1.4
        return armsUp && legsApart
    }

    private fun armsExtended(pose: Pose): Boolean {
        val lWrist = landmark(pose, PoseLandmark.LEFT_WRIST) ?: return false
        val rWrist = landmark(pose, PoseLandmark.RIGHT_WRIST) ?: return false
        val lShoulder = landmark(pose, PoseLandmark.LEFT_SHOULDER) ?: return false
        val rShoulder = landmark(pose, PoseLandmark.RIGHT_SHOULDER) ?: return false
        val scale = bodyScale(pose) ?: return false
        // ruky natiahnuté od tela do strán/hore (nie pozdĺž tela)
        val lExtended = dist2D(lWrist, lShoulder) > scale * 0.7
        val rExtended = dist2D(rWrist, rShoulder) > scale * 0.7
        return lExtended && rExtended
    }

    private fun bodyLineOk(pose: Pose): Boolean {
        val angle = angleBetween(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_ANKLE)
            ?: angleBetween(pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_ANKLE)
            ?: return true // ak sa nedá vyhodnotiť, radšej neobťažovať upozornením
        return angle in 155.0..185.0
    }

    private fun backLeanCheck(pose: Pose): String? {
        val angle = angleBetween(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
            ?: angleBetween(pose, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
            ?: return null
        return if (angle < 100.0) "Narovnaj chrbát" else null
    }

    private fun angleOk(pose: Pose, a: Int, b: Int, c: Int, min: Double, max: Double): Boolean {
        val angle = angleBetween(pose, a, b, c) ?: return false
        return angle in min..max
    }

    // ---------- Výber ľavej/pravej strany s fallbackom ----------

    private fun hip() = PoseLandmark.LEFT_HIP
    private fun knee() = PoseLandmark.LEFT_KNEE
    private fun ankle() = PoseLandmark.LEFT_ANKLE
    private fun shoulder() = PoseLandmark.LEFT_SHOULDER
    private fun elbow() = PoseLandmark.LEFT_ELBOW
    private fun wrist() = PoseLandmark.LEFT_WRIST

    private fun angleBetween(pose: Pose, primaryA: Int, primaryB: Int, primaryC: Int): Double? {
        // Skús ľavú stranu; ak chýba, skús zodpovedajúcu pravú.
        val a = landmark(pose, primaryA) ?: landmark(pose, rightOf(primaryA)) ?: return null
        val b = landmark(pose, primaryB) ?: landmark(pose, rightOf(primaryB)) ?: return null
        val c = landmark(pose, primaryC) ?: landmark(pose, rightOf(primaryC)) ?: return null
        return angle3(a, b, c)
    }

    private fun rightOf(leftLandmark: Int): Int = when (leftLandmark) {
        PoseLandmark.LEFT_HIP -> PoseLandmark.RIGHT_HIP
        PoseLandmark.LEFT_KNEE -> PoseLandmark.RIGHT_KNEE
        PoseLandmark.LEFT_ANKLE -> PoseLandmark.RIGHT_ANKLE
        PoseLandmark.LEFT_SHOULDER -> PoseLandmark.RIGHT_SHOULDER
        PoseLandmark.LEFT_ELBOW -> PoseLandmark.RIGHT_ELBOW
        PoseLandmark.LEFT_WRIST -> PoseLandmark.RIGHT_WRIST
        else -> leftLandmark
    }

    private fun landmark(pose: Pose, type: Int): PointF3D? = pose.getPoseLandmark(type)?.position3D

    private fun bodyScale(pose: Pose): Double? {
        val lShoulder = landmark(pose, PoseLandmark.LEFT_SHOULDER) ?: return null
        val rShoulder = landmark(pose, PoseLandmark.RIGHT_SHOULDER) ?: return null
        return dist2D(lShoulder, rShoulder).coerceAtLeast(0.01)
    }

    private fun dist2D(a: PointF3D, b: PointF3D): Double {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    private fun angle3(a: PointF3D, b: PointF3D, c: PointF3D): Double {
        val baX = a.x - b.x; val baY = a.y - b.y
        val bcX = c.x - b.x; val bcY = c.y - b.y
        val dot = baX * bcX + baY * bcY
        val magBa = sqrt((baX * baX + baY * baY).toDouble())
        val magBc = sqrt((bcX * bcX + bcY * bcY).toDouble())
        if (magBa == 0.0 || magBc == 0.0) return 180.0
        val cos = (dot / (magBa * magBc)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cos))
    }
}
