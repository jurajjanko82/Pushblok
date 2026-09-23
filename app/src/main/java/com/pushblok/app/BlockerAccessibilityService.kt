package com.pushblok.app

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.os.Handler
import android.os.Looper

class BlockerAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var phoneLockChecker: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        startWholePhoneWatcher()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return // vlastná appka nikdy neblokujeme

        val blocked = BlockManager.getBlockedApps(this)
        if (pkg in blocked && !BlockManager.isAppCurrentlyUnlocked(this, pkg)) {
            launchBlockScreen(pkg)
        }
    }

    private fun launchBlockScreen(pkg: String) {
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockOverlayActivity.EXTRA_PACKAGE, pkg)
            putExtra(BlockOverlayActivity.EXTRA_WHOLE_PHONE, false)
        }
        startActivity(intent)
    }

    /** Ak je zapnuté "zablokovať celý telefón", pravidelne kontroluje a zamyká obrazovku. */
    private fun startWholePhoneWatcher() {
        phoneLockChecker = object : Runnable {
            override fun run() {
                if (BlockManager.isWholePhoneBlockEnabled(this@BlockerAccessibilityService) &&
                    !BlockManager.isWholePhoneCurrentlyUnlocked(this@BlockerAccessibilityService)
                ) {
                    lockWholePhone()
                }
                handler.postDelayed(this, 5_000)
            }
        }
        handler.post(phoneLockChecker!!)
    }

    private fun lockWholePhone() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, PhoneAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
        } else {
            // Admin nie je aktívny -> aspoň ukáž obrazovku s výzvou na odomknutie kreditmi
            val intent = Intent(this, BlockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(BlockOverlayActivity.EXTRA_WHOLE_PHONE, true)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        phoneLockChecker?.let { handler.removeCallbacks(it) }
    }
}
