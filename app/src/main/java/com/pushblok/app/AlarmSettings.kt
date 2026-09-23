package com.pushblok.app

import android.content.Context

object AlarmSettings {
    private const val PREFS = "pushblok_alarm_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_REQUIRED_REPS = "required_reps"
    private const val KEY_EXERCISE_TYPE = "exercise_type"
    private const val KEY_CREDIT_COST_TO_SKIP = "credit_cost_to_skip"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context) = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun getHour(ctx: Context) = prefs(ctx).getInt(KEY_HOUR, 7)
    fun getMinute(ctx: Context) = prefs(ctx).getInt(KEY_MINUTE, 0)
    fun getRequiredReps(ctx: Context) = prefs(ctx).getInt(KEY_REQUIRED_REPS, 15)
    fun getExerciseType(ctx: Context): ExerciseType =
        ExerciseType.valueOf(prefs(ctx).getString(KEY_EXERCISE_TYPE, ExerciseType.SQUATS.name)!!)
    fun getCreditCostToSkip(ctx: Context) = prefs(ctx).getInt(KEY_CREDIT_COST_TO_SKIP, 20)

    fun save(
        ctx: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        requiredReps: Int,
        exerciseType: ExerciseType,
        creditCostToSkip: Int
    ) {
        prefs(ctx).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .putInt(KEY_REQUIRED_REPS, requiredReps)
            .putString(KEY_EXERCISE_TYPE, exerciseType.name)
            .putInt(KEY_CREDIT_COST_TO_SKIP, creditCostToSkip)
            .apply()
    }
}
