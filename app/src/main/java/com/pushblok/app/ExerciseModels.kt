package com.pushblok.app

import android.content.Context

enum class ExerciseCategory(val label: String) {
    LEGS("Nohy"),
    CORE("Brucho / Core"),
    ARMS_CHEST("Ruky / hruď"),
    CARDIO("Kardio / celé telo")
}

enum class ExerciseUnit { REPS, SECONDS }

enum class ExerciseType(
    val label: String,
    val category: ExerciseCategory,
    val unit: ExerciseUnit,
    /** Rada, ako polozit telefon pre co najlepsie rozpoznanie. */
    val placementTip: String
) {
    // ---- NOHY ----
    SQUATS("Drepy", ExerciseCategory.LEGS, ExerciseUnit.REPS,
        "Telefón oprený čelom k tebe, vidno celé telo od hlavy po členky."),
    LUNGES("Výpady", ExerciseCategory.LEGS, ExerciseUnit.REPS,
        "Telefón čelom/bokom k tebe, vidno obe nohy pri výpade dopredu."),
    JUMP_SQUATS("Drepy s výskokom", ExerciseCategory.LEGS, ExerciseUnit.REPS,
        "Telefón čelom k tebe, dosť miesta na výskok."),
    SIDE_LUNGES("Bočné výpady", ExerciseCategory.LEGS, ExerciseUnit.REPS,
        "Telefón čelom k tebe, vidno šírku pohybu do strán."),
    CALF_RAISES("Výpony na špičky", ExerciseCategory.LEGS, ExerciseUnit.REPS,
        "Telefón nižšie, aby bolo dobre vidno chodidlá a členky."),
    WALL_SIT("Sed pri stene (výdrž)", ExerciseCategory.LEGS, ExerciseUnit.SECONDS,
        "Telefón čelom k tebe, sadni si chrbtom o stenu do 90°."),
    HIGH_KNEES("Beh na mieste (vysoké kolená)", ExerciseCategory.LEGS, ExerciseUnit.REPS,
        "Telefón čelom k tebe, vidno kolená nad pás."),

    // ---- CORE / BRUCHO ----
    PLANK("Plank (výdrž)", ExerciseCategory.CORE, ExerciseUnit.SECONDS,
        "Telefón na zemi, bokom, tak aby bolo vidno telo od ramien po členky."),
    SIDE_PLANK_LEFT("Bočný plank - ľavá", ExerciseCategory.CORE, ExerciseUnit.SECONDS,
        "Telefón na zemi oproti tebe, ľahni si na ľavý bok."),
    SIDE_PLANK_RIGHT("Bočný plank - pravá", ExerciseCategory.CORE, ExerciseUnit.SECONDS,
        "Telefón na zemi oproti tebe, ľahni si na pravý bok."),
    CRUNCHES("Brušáky", ExerciseCategory.CORE, ExerciseUnit.REPS,
        "Telefón na zemi pri nohách, mieri na hornú časť tela."),
    BICYCLE_CRUNCHES("Bicyklové brušáky", ExerciseCategory.CORE, ExerciseUnit.REPS,
        "Telefón na zemi pri nohách, vidno lakte aj kolená."),
    LEG_RAISES("Zdvihy nôh v ľahu", ExerciseCategory.CORE, ExerciseUnit.REPS,
        "Telefón na zemi pri hlave, mieri na celé telo."),
    MOUNTAIN_CLIMBERS("Mountain climbers", ExerciseCategory.CORE, ExerciseUnit.REPS,
        "Telefón na zemi, bokom, v planku vidno kolená aj ramená."),

    // ---- RUKY / HRUĎ ----
    PUSHUPS("Kliky", ExerciseCategory.ARMS_CHEST, ExerciseUnit.REPS,
        "Telefón na zemi, bokom, vidno rameno-lakeť-zápästie."),
    DIAMOND_PUSHUPS("Diamantové kliky", ExerciseCategory.ARMS_CHEST, ExerciseUnit.REPS,
        "Telefón na zemi, bokom, ruky pod hrudníkom v tvare diamantu."),
    TRICEP_DIPS("Tricepové kliky (na stoličke)", ExerciseCategory.ARMS_CHEST, ExerciseUnit.REPS,
        "Telefón bokom, vidno ohýbanie lakťov pri spúšťaní tela."),
    PLANK_UPS("Plank-up (z podlahy do vzporu)", ExerciseCategory.ARMS_CHEST, ExerciseUnit.REPS,
        "Telefón na zemi, bokom, vidno celú paži pri striedaní polôh."),
    SHOULDER_TAPS("Shoulder taps", ExerciseCategory.ARMS_CHEST, ExerciseUnit.REPS,
        "Telefón na zemi, čelom, v planku vidno obe ramená a ruky."),
    ARM_CIRCLES("Krúženie rukami (výdrž)", ExerciseCategory.ARMS_CHEST, ExerciseUnit.SECONDS,
        "Telefón čelom k tebe, vidno obe natiahnuté ruky."),

    // ---- KARDIO / CELÉ TELO ----
    JUMPING_JACKS("Jumping Jacks", ExerciseCategory.CARDIO, ExerciseUnit.REPS,
        "Telefón čelom k tebe, dosť miesta na roznoženie."),
    BURPEES("Burpees", ExerciseCategory.CARDIO, ExerciseUnit.REPS,
        "Telefón čelom k tebe z väčšej vzdialenosti, vidno celé telo pri páde aj výskoku.");

    companion object {
        fun byCategory(cat: ExerciseCategory) = values().filter { it.category == cat }
    }
}

/**
 * Ukladá, koľko kreditov dostaneš za 1 opakovanie (REPS) alebo za N sekúnd
 * v správnej polohe (SECONDS) pre každý cvik.
 */
object ExerciseSettings {

    private const val PREFS = "pushblok_exercise_prefs"
    private const val KEY_PREFIX_CREDITS = "credits_per_unit_"
    const val SECONDS_PER_CREDIT_UNIT = 5 // pri SECONDS cvikoch: 1 "jednotka" = 5 sekúnd výdrže

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCreditsPerUnit(ctx: Context, type: ExerciseType): Int =
        prefs(ctx).getInt(KEY_PREFIX_CREDITS + type.name, 1)

    fun setCreditsPerUnit(ctx: Context, type: ExerciseType, credits: Int) {
        prefs(ctx).edit().putInt(KEY_PREFIX_CREDITS + type.name, credits.coerceAtLeast(0)).apply()
    }
}
