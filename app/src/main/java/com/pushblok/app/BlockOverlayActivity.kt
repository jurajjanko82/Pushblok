package com.pushblok.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pushblok.app.databinding.ActivityBlockOverlayBinding

class BlockOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE = "extra_package"
        const val EXTRA_WHOLE_PHONE = "extra_whole_phone"
    }

    private lateinit var binding: ActivityBlockOverlayBinding
    private var targetPackage: String? = null
    private var isWholePhone: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)
        isWholePhone = intent.getBooleanExtra(EXTRA_WHOLE_PHONE, false)

        val label = if (isWholePhone) "celý telefón" else appLabel(targetPackage)
        binding.tvTitle.text = "Zablokované: $label"

        refreshCredits()

        binding.btnUnlock5.setOnClickListener { tryUnlock(5) }
        binding.btnUnlock15.setOnClickListener { tryUnlock(15) }
        binding.btnUnlock30.setOnClickListener { tryUnlock(30) }

        binding.btnClose.setOnClickListener {
            // Odmietnutie -> späť na plochu (appku nechávame zablokovanú)
            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_HOME)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }
    }

    private fun appLabel(pkg: String?): String {
        if (pkg == null) return "appka"
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    private fun refreshCredits() {
        binding.tvCredits.text = "Máš ${CreditManager.getCredits(this)} kreditov (1 kredit = 1 minúta)"
    }

    private fun tryUnlock(minutes: Int) {
        val pkg = if (isWholePhone) BlockManager.WHOLE_PHONE_KEY else targetPackage
        if (pkg == null) return

        val success = BlockManager.unlockAppWithCredits(this, pkg, minutes)
        if (success) {
            Toast.makeText(this, "Odomknuté na $minutes minút", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Nemáš dosť kreditov ($minutes potrebných)", Toast.LENGTH_SHORT).show()
            refreshCredits()
        }
    }

    override fun onBackPressed() {
        // Zablokujeme tlačidlo späť, aby sa nedalo len tak obísť blokovanie appky.
        // (Používateľ musí buď odomknúť kreditmi, alebo ísť na plochu cez btnClose.)
    }
}
