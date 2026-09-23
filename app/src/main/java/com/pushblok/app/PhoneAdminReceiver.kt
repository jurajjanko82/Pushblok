package com.pushblok.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PhoneAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "PushBlok môže teraz zamykať telefón", Toast.LENGTH_SHORT).show()
    }
}
