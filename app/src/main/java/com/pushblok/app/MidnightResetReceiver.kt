package com.pushblok.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.*

class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Samotný reset spraví CreditManager pri ďalšom čítaní kreditov,
        // ale tu to vynútime hneď, aj keby appka nebežala na popredí.
        CreditManager.checkMidnightReset(context)
        scheduleNext(context)
    }

    companion object {
        private const val REQUEST_CODE = 1001

        fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MidnightResetReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }

            // Používame NEpresný alarm (setAndAllowWhileIdle), aby appka nepotrebovala
            // špeciálne povolenie "Presné budíky" (SCHEDULE_EXACT_ALARM) a nespadla
            // pri štarte na telefónoch, kde toto povolenie nie je udelené (napr. Samsung).
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                // Ak by aj toto zlyhalo, appka nesmie spadnúť - reset sa aj tak
                // vykoná pri ďalšom otvorení appky (CreditManager.checkMidnightReset).
            }
        }
    }
}
