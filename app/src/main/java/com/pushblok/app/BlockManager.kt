package com.pushblok.app

import android.content.Context

/**
 * Drží zoznam blokovaných appiek + info, či je práve aktívne "odblokovanie na X minút"
 * zaplatené kreditmi (1 kredit = 1 minúta).
 */
object BlockManager {

    private const val PREFS = "pushblok_block_prefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_BLOCK_WHOLE_PHONE = "block_whole_phone"
    private const val KEY_UNLOCK_UNTIL = "unlock_until" // per-app: packageName -> timestamp, uložené ako String

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBlockedApps(ctx: Context): MutableSet<String> =
        HashSet(prefs(ctx).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet())

    fun setBlockedApps(ctx: Context, packages: Set<String>) {
        prefs(ctx).edit().putStringSet(KEY_BLOCKED_APPS, packages).apply()
    }

    fun toggleApp(ctx: Context, packageName: String, blocked: Boolean) {
        val current = getBlockedApps(ctx)
        if (blocked) current.add(packageName) else current.remove(packageName)
        setBlockedApps(ctx, current)
    }

    fun isWholePhoneBlockEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_WHOLE_PHONE, false)

    fun setWholePhoneBlock(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_BLOCK_WHOLE_PHONE, enabled).apply()
    }

    /** Časová pečiatka (ms), do kedy je konkrétna appka odomknutá. 0 = nie je odomknutá. */
    fun getUnlockUntil(ctx: Context, packageName: String): Long =
        prefs(ctx).getLong(KEY_UNLOCK_UNTIL + "_" + packageName, 0L)

    fun isAppCurrentlyUnlocked(ctx: Context, packageName: String): Boolean =
        System.currentTimeMillis() < getUnlockUntil(ctx, packageName)

    /**
     * Odblokuje appku na daný počet minút tak, že minie kredity (1 kredit = 1 minúta).
     * Ak je appka práve odomknutá, čas sa predĺži o ďalšie minúty.
     */
    fun unlockAppWithCredits(ctx: Context, packageName: String, minutes: Int): Boolean {
        if (minutes <= 0) return false
        if (!CreditManager.spendCredits(ctx, minutes)) return false
        val now = System.currentTimeMillis()
        val currentUntil = getUnlockUntil(ctx, packageName).coerceAtLeast(now)
        val newUntil = currentUntil + minutes * 60_000L
        prefs(ctx).edit().putLong(KEY_UNLOCK_UNTIL + "_" + packageName, newUntil).apply()
        return true
    }

    /** Odblokuje CELÝ telefón (odloží zamknutie obrazovky) na X minút za kredity. */
    fun unlockWholePhoneWithCredits(ctx: Context, minutes: Int): Boolean =
        unlockAppWithCredits(ctx, WHOLE_PHONE_KEY, minutes)

    fun isWholePhoneCurrentlyUnlocked(ctx: Context): Boolean =
        isAppCurrentlyUnlocked(ctx, WHOLE_PHONE_KEY)

    const val WHOLE_PHONE_KEY = "__WHOLE_PHONE__"
}
