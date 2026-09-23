package com.pushblok.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

/**
 * Celá logika kreditov beží LOKÁLNE (SharedPreferences) -> appka funguje 100% offline.
 *
 * Pravidlá:
 *  - 1 kredit = 1 minúta odblokovania appiek/telefónu
 *  - kredity sa dajú získať za kroky, kliky, cvičenia
 *  - o 0:00 sa NEULOŽENÉ kredity (tie, čo nie sú vo vkladnej knižke) vynulujú
 *  - vkladná knižka sa odomkne jednorazovo za 200 kreditov
 *  - denný limit vkladu aj výberu je 30 kreditov
 *  - poplatok za vklad aj za výber je 5 kreditov
 */
object CreditManager {

    private const val PREFS = "pushblok_prefs"
    private const val KEY_CREDITS = "credits"
    private const val KEY_SAVINGS = "savings"
    private const val KEY_SAVINGS_UNLOCKED = "savings_unlocked"
    private const val KEY_LAST_RESET_DATE = "last_reset_date"
    private const val KEY_DEPOSITED_TODAY = "deposited_today"
    private const val KEY_WITHDRAWN_TODAY = "withdrawn_today"
    private const val KEY_LAST_LIMIT_DATE = "last_limit_date"

    const val UNLOCK_SAVINGS_COST = 200
    const val DAILY_LIMIT = 30
    const val TRANSACTION_FEE = 5

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String = dateFormat.format(Date())

    /** Zavolať pri každom spustení appky / pri kontrole kreditov. */
    fun checkMidnightReset(ctx: Context) {
        val p = prefs(ctx)
        val lastReset = p.getString(KEY_LAST_RESET_DATE, null)
        val now = today()
        if (lastReset != now) {
            // Nevložené kredity prepadajú. Vklad (savings) ostáva netknutý.
            p.edit()
                .putInt(KEY_CREDITS, 0)
                .putString(KEY_LAST_RESET_DATE, now)
                .apply()
        }
        val lastLimitDate = p.getString(KEY_LAST_LIMIT_DATE, null)
        if (lastLimitDate != now) {
            p.edit()
                .putInt(KEY_DEPOSITED_TODAY, 0)
                .putInt(KEY_WITHDRAWN_TODAY, 0)
                .putString(KEY_LAST_LIMIT_DATE, now)
                .apply()
        }
    }

    fun getCredits(ctx: Context): Int {
        checkMidnightReset(ctx)
        return prefs(ctx).getInt(KEY_CREDITS, 0)
    }

    fun getSavings(ctx: Context): Int = prefs(ctx).getInt(KEY_SAVINGS, 0)

    fun isSavingsUnlocked(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SAVINGS_UNLOCKED, false)

    fun getDepositedToday(ctx: Context): Int {
        checkMidnightReset(ctx)
        return prefs(ctx).getInt(KEY_DEPOSITED_TODAY, 0)
    }

    fun getWithdrawnToday(ctx: Context): Int {
        checkMidnightReset(ctx)
        return prefs(ctx).getInt(KEY_WITHDRAWN_TODAY, 0)
    }

    /** Pripíše kredity získané za kroky/kliky/cvičenia. */
    fun addCredits(ctx: Context, amount: Int) {
        if (amount <= 0) return
        checkMidnightReset(ctx)
        val p = prefs(ctx)
        val current = p.getInt(KEY_CREDITS, 0)
        p.edit().putInt(KEY_CREDITS, current + amount).apply()
    }

    /** Minie kredity na odblokovanie appiek/telefónu. Vráti true ak sa podarilo. */
    fun spendCredits(ctx: Context, amount: Int): Boolean {
        checkMidnightReset(ctx)
        val p = prefs(ctx)
        val current = p.getInt(KEY_CREDITS, 0)
        if (current < amount) return false
        p.edit().putInt(KEY_CREDITS, current - amount).apply()
        return true
    }

    sealed class Result {
        object Success : Result()
        object NotUnlocked : Result()
        object DailyLimitExceeded : Result()
        object InsufficientCredits : Result()
        object InsufficientSavings : Result()
    }

    /** Jednorazové odomknutie vkladnej knižky za 200 kreditov. */
    fun unlockSavings(ctx: Context): Result {
        checkMidnightReset(ctx)
        val p = prefs(ctx)
        if (p.getBoolean(KEY_SAVINGS_UNLOCKED, false)) return Result.Success
        val current = p.getInt(KEY_CREDITS, 0)
        if (current < UNLOCK_SAVINGS_COST) return Result.InsufficientCredits
        p.edit()
            .putInt(KEY_CREDITS, current - UNLOCK_SAVINGS_COST)
            .putBoolean(KEY_SAVINGS_UNLOCKED, true)
            .apply()
        return Result.Success
    }

    /** Vklad kreditov do vkladnej knižky. amount = suma BEZ poplatku; poplatok 5 kreditov sa strhne navyše. */
    fun deposit(ctx: Context, amount: Int): Result {
        checkMidnightReset(ctx)
        val p = prefs(ctx)
        if (!p.getBoolean(KEY_SAVINGS_UNLOCKED, false)) return Result.NotUnlocked
        if (amount <= 0) return Result.Success

        val depositedToday = p.getInt(KEY_DEPOSITED_TODAY, 0)
        if (depositedToday + amount > DAILY_LIMIT) return Result.DailyLimitExceeded

        val credits = p.getInt(KEY_CREDITS, 0)
        val totalCost = amount + TRANSACTION_FEE
        if (credits < totalCost) return Result.InsufficientCredits

        val savings = p.getInt(KEY_SAVINGS, 0)
        p.edit()
            .putInt(KEY_CREDITS, credits - totalCost)
            .putInt(KEY_SAVINGS, savings + amount)
            .putInt(KEY_DEPOSITED_TODAY, depositedToday + amount)
            .apply()
        return Result.Success
    }

    /** Výber kreditov z vkladnej knižky. amount = suma BEZ poplatku; poplatok 5 kreditov sa strhne navyše z výberu. */
    fun withdraw(ctx: Context, amount: Int): Result {
        checkMidnightReset(ctx)
        val p = prefs(ctx)
        if (!p.getBoolean(KEY_SAVINGS_UNLOCKED, false)) return Result.NotUnlocked
        if (amount <= 0) return Result.Success

        val withdrawnToday = p.getInt(KEY_WITHDRAWN_TODAY, 0)
        if (withdrawnToday + amount > DAILY_LIMIT) return Result.DailyLimitExceeded

        val savings = p.getInt(KEY_SAVINGS, 0)
        if (savings < amount) return Result.InsufficientSavings

        // poplatok sa strháva z vyberanej sumy
        if (amount <= TRANSACTION_FEE) return Result.InsufficientCredits
        val creditsGained = amount - TRANSACTION_FEE

        val credits = p.getInt(KEY_CREDITS, 0)
        p.edit()
            .putInt(KEY_SAVINGS, savings - amount)
            .putInt(KEY_CREDITS, credits + creditsGained)
            .putInt(KEY_WITHDRAWN_TODAY, withdrawnToday + amount)
            .apply()
        return Result.Success
    }
}
