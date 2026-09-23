package com.pushblok.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pushblok.app.databinding.ActivitySavingsBinding

class SavingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUnlock.setOnClickListener {
            when (CreditManager.unlockSavings(this)) {
                CreditManager.Result.Success -> toast("Vkladná knižka odomknutá!")
                CreditManager.Result.InsufficientCredits -> toast("Potrebuješ ${CreditManager.UNLOCK_SAVINGS_COST} kreditov.")
                else -> {}
            }
            refresh()
        }

        binding.btnDeposit.setOnClickListener {
            val amount = binding.etAmount.text.toString().toIntOrNull() ?: 0
            when (val r = CreditManager.deposit(this, amount)) {
                CreditManager.Result.Success -> toast("Vložených $amount kreditov (poplatok ${CreditManager.TRANSACTION_FEE}).")
                CreditManager.Result.NotUnlocked -> toast("Najprv odomkni vkladnú knižku za ${CreditManager.UNLOCK_SAVINGS_COST} kreditov.")
                CreditManager.Result.DailyLimitExceeded -> toast("Denný limit vkladu je ${CreditManager.DAILY_LIMIT} kreditov.")
                CreditManager.Result.InsufficientCredits -> toast("Nemáš dosť kreditov (suma + poplatok ${CreditManager.TRANSACTION_FEE}).")
                else -> toast(r.toString())
            }
            refresh()
        }

        binding.btnWithdraw.setOnClickListener {
            val amount = binding.etAmount.text.toString().toIntOrNull() ?: 0
            when (val r = CreditManager.withdraw(this, amount)) {
                CreditManager.Result.Success -> toast("Vybraných $amount kreditov (poplatok ${CreditManager.TRANSACTION_FEE} strhnutý z výberu).")
                CreditManager.Result.NotUnlocked -> toast("Najprv odomkni vkladnú knižku za ${CreditManager.UNLOCK_SAVINGS_COST} kreditov.")
                CreditManager.Result.DailyLimitExceeded -> toast("Denný limit výberu je ${CreditManager.DAILY_LIMIT} kreditov.")
                CreditManager.Result.InsufficientSavings -> toast("Vo vkladnej knižke nemáš dosť kreditov.")
                CreditManager.Result.InsufficientCredits -> toast("Suma musí byť väčšia ako poplatok ${CreditManager.TRANSACTION_FEE}.")
                else -> toast(r.toString())
            }
            refresh()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val unlocked = CreditManager.isSavingsUnlocked(this)
        binding.tvStatus.text = if (unlocked) "Vkladná knižka: ODOMKNUTÁ" else "Vkladná knižka: ZAMKNUTÁ"
        binding.btnUnlock.isEnabled = !unlocked
        binding.btnDeposit.isEnabled = unlocked
        binding.btnWithdraw.isEnabled = unlocked

        binding.tvCredits.text = "Kredity: ${CreditManager.getCredits(this)}"
        binding.tvSavingsBalance.text = "Vo vkladnej knižke: ${CreditManager.getSavings(this)}"
        binding.tvLimits.text = "Dnes vložené: ${CreditManager.getDepositedToday(this)}/${CreditManager.DAILY_LIMIT}   " +
                "Dnes vybraté: ${CreditManager.getWithdrawnToday(this)}/${CreditManager.DAILY_LIMIT}"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
