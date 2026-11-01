package com.example.simpleapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.simpleapp.databinding.ActivityMainBinding
import com.example.simpleapp.databinding.ItemTransactionBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentBalance: Double = 14850.50
    private var isBalanceHidden = false
    private var isAuthPasswordVisible = false
    private val currencyFormatter = DecimalFormat("#,##0.00")

    private val transactions = mutableListOf(
        Transaction(
            title = "Deposit - Bank Transfer",
            date = "Nov 02, 01:25 AM • #TX-9842",
            amount = 500.00,
            isDeposit = true,
            status = "Completed"
        ),
        Transaction(
            title = "Withdrawal - Chase Bank",
            date = "Nov 01, 09:14 PM • #TX-9821",
            amount = 150.00,
            isDeposit = false,
            status = "Completed"
        ),
        Transaction(
            title = "Deposit - Crypto USDT (TRC20)",
            date = "Nov 01, 04:30 PM • #TX-9804",
            amount = 1200.00,
            isDeposit = true,
            status = "Completed"
        ),
        Transaction(
            title = "Withdrawal - Instant Payout",
            date = "Oct 31, 11:15 AM • #TX-9755",
            amount = 320.00,
            isDeposit = false,
            status = "Completed"
        ),
        Transaction(
            title = "Deposit - Bank Transfer",
            date = "Oct 30, 08:20 PM • #TX-9712",
            amount = 800.00,
            isDeposit = true,
            status = "Completed"
        )
    )

    private var currentFilter = "All"
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAuthScreen()
        setupDashboard()
    }

    // ========================================================
    // AUTHENTICATION SCREEN LOGIC (Sign Up & Login)
    // ========================================================
    private fun setupAuthScreen() {
        // Toggle Password Visibility
        binding.btnAuthTogglePassword.setOnClickListener {
            isAuthPasswordVisible = !isAuthPasswordVisible
            if (isAuthPasswordVisible) {
                binding.etAuthPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.btnAuthTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etAuthPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.btnAuthTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            binding.etAuthPassword.setSelection(binding.etAuthPassword.text.length)
        }

        // Login Button
        binding.btnAuthLogin.setOnClickListener {
            val username = binding.etAuthUsername.text.toString().trim()
            val password = binding.etAuthPassword.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Please enter your username or WhatsApp number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Successful Login
            binding.tvUserName.text = username
            Toast.makeText(this, "Welcome, $username! Login Successful", Toast.LENGTH_SHORT).show()
            transitionToDashboard()
        }

        // Create Account Icon Box
        binding.boxCreateAccount.setOnClickListener {
            val username = binding.etAuthUsername.text.toString().trim()
            val finalName = if (username.isNotEmpty()) username else "Sultan"
            binding.tvUserName.text = finalName

            Toast.makeText(this, "Account created successfully for $finalName!", Toast.LENGTH_LONG).show()
            transitionToDashboard()
        }

        // WhatsApp Icon Box
        binding.boxWhatsApp.setOnClickListener {
            openWhatsApp("923001234567", "Hello BP Exch Support, I need help with my account / ID creation.")
        }

        // Dummy Video Link Button / Box
        binding.boxDummyVideo.setOnClickListener {
            openDummyVideoGuide()
        }
    }

    private fun transitionToDashboard() {
        binding.containerAuth.visibility = View.GONE
        binding.layoutDashboard.visibility = View.VISIBLE
        selectNavTab(0)
    }

    private fun transitionToAuth() {
        binding.layoutDashboard.visibility = View.GONE
        binding.containerAuth.visibility = View.VISIBLE
        binding.etAuthPassword.text.clear()
    }

    private fun openWhatsApp(phoneNumber: String, message: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Opening WhatsApp for +$phoneNumber: $message", Toast.LENGTH_LONG).show()
        }
    }

    private fun openDummyVideoGuide() {
        try {
            val videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Playing dummy video tutorial: How to Create an Account", Toast.LENGTH_LONG).show()
        }
    }

    // ========================================================
    // MAIN DASHBOARD LOGIC
    // ========================================================
    private fun setupDashboard() {
        updateBalanceDisplay()
        renderTransactions()
        updateActivityLedger()

        // Eye Toggle for Balance
        binding.btnToggleBalance.setOnClickListener {
            isBalanceHidden = !isBalanceHidden
            updateBalanceDisplay()
        }

        // Action Buttons on Home
        binding.btnActionDeposit.setOnClickListener { showDepositSheet() }
        binding.btnActionWithdraw.setOnClickListener { showWithdrawSheet() }

        binding.btnActionTransfer.setOnClickListener {
            Toast.makeText(this, "Exchange Service: 0% fee rate locked for 30s", Toast.LENGTH_SHORT).show()
        }

        binding.btnActionAccounts.setOnClickListener {
            selectNavTab(2) // Jump directly to Accounts tab
        }

        // Top Bar Icons
        binding.btnSupport.setOnClickListener {
            Toast.makeText(this, "Connecting to 24/7 Live Support Agent...", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotifications.setOnClickListener {
            Toast.makeText(this, "No unread alerts. Account is fully protected.", Toast.LENGTH_SHORT).show()
        }

        // Filter Chips on Home
        binding.chipAll.setOnClickListener { setFilter("All") }
        binding.chipDeposits.setOnClickListener { setFilter("Deposits") }
        binding.chipWithdrawals.setOnClickListener { setFilter("Withdrawals") }

        binding.tvViewAll.setOnClickListener {
            selectNavTab(1) // Jump to Activity tab
        }

        // Accounts Tab Actions
        binding.btnAddAccount.setOnClickListener {
            showAddAccountDialog()
        }

        // Profile Tab Actions
        binding.rowSecurity.setOnClickListener {
            Toast.makeText(this, "Security Center: 2FA is active with Google Authenticator", Toast.LENGTH_SHORT).show()
        }

        binding.rowSupport.setOnClickListener {
            Toast.makeText(this, "Opening Live Helpdesk & FAQ...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSignOut.setOnClickListener {
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            transitionToAuth()
        }

        // Sticky Footer Navigation Tabs
        binding.navHome.setOnClickListener { selectNavTab(0) }
        binding.navActivity.setOnClickListener { selectNavTab(1) }
        binding.navAccounts.setOnClickListener { selectNavTab(2) }
        binding.navProfile.setOnClickListener { selectNavTab(3) }
    }

    private fun selectNavTab(tabIndex: Int) {
        currentTab = tabIndex

        binding.containerHome.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.containerActivity.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.containerAccounts.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        binding.containerProfile.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE

        // Smooth scroll to top of screen
        binding.scrollView.scrollTo(0, 0)

        val activeColor = ContextCompat.getColor(this, R.color.accent_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_muted)

        // Tab 0: Home
        binding.ivNavHome.setColorFilter(if (tabIndex == 0) activeColor else inactiveColor)
        binding.tvNavHome.setTextColor(if (tabIndex == 0) activeColor else inactiveColor)

        // Tab 1: Activity
        binding.ivNavActivity.setColorFilter(if (tabIndex == 1) activeColor else inactiveColor)
        binding.tvNavActivity.setTextColor(if (tabIndex == 1) activeColor else inactiveColor)

        // Tab 2: Accounts
        binding.ivNavAccounts.setColorFilter(if (tabIndex == 2) activeColor else inactiveColor)
        binding.tvNavAccounts.setTextColor(if (tabIndex == 2) activeColor else inactiveColor)

        // Tab 3: Profile
        binding.ivNavProfile.setColorFilter(if (tabIndex == 3) activeColor else inactiveColor)
        binding.tvNavProfile.setTextColor(if (tabIndex == 3) activeColor else inactiveColor)

        if (tabIndex == 1) {
            updateActivityLedger()
        }
    }

    private fun updateBalanceDisplay() {
        if (isBalanceHidden) {
            binding.tvBalance.text = "$••••••"
            binding.btnToggleBalance.setImageResource(R.drawable.ic_eye_off)
        } else {
            binding.tvBalance.text = "$" + currencyFormatter.format(currentBalance)
            binding.btnToggleBalance.setImageResource(R.drawable.ic_eye)
        }
    }

    private fun setFilter(filter: String) {
        currentFilter = filter

        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_filter_active)
        val inactiveBg = ContextCompat.getDrawable(this, R.drawable.bg_chip_filter)
        val activeTextColor = ContextCompat.getColor(this, R.color.text_white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_muted)

        binding.chipAll.background = if (filter == "All") activeBg else inactiveBg
        binding.chipAll.setTextColor(if (filter == "All") activeTextColor else inactiveTextColor)

        binding.chipDeposits.background = if (filter == "Deposits") activeBg else inactiveBg
        binding.chipDeposits.setTextColor(if (filter == "Deposits") activeTextColor else inactiveTextColor)

        binding.chipWithdrawals.background = if (filter == "Withdrawals") activeBg else inactiveBg
        binding.chipWithdrawals.setTextColor(if (filter == "Withdrawals") activeTextColor else inactiveTextColor)

        renderTransactions()
    }

    private fun renderTransactions() {
        binding.layoutTransactions.removeAllViews()

        val filtered = when (currentFilter) {
            "Deposits" -> transactions.filter { it.isDeposit }
            "Withdrawals" -> transactions.filter { !it.isDeposit }
            else -> transactions
        }

        val inflater = LayoutInflater.from(this)
        for (tx in filtered) {
            val itemBinding = ItemTransactionBinding.inflate(inflater, binding.layoutTransactions, false)
            itemBinding.tvTxTitle.text = tx.title
            itemBinding.tvTxDate.text = tx.date

            if (tx.isDeposit) {
                itemBinding.ivTxIcon.setImageResource(R.drawable.ic_arrow_down_left)
                itemBinding.ivTxIcon.setBackgroundResource(R.drawable.bg_action_circle_emerald)
                itemBinding.tvTxAmount.text = "+$" + currencyFormatter.format(tx.amount)
                itemBinding.tvTxAmount.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))
            } else {
                itemBinding.ivTxIcon.setImageResource(R.drawable.ic_arrow_up_right)
                itemBinding.ivTxIcon.setBackgroundResource(R.drawable.bg_action_circle_rose)
                itemBinding.tvTxAmount.text = "-$" + currencyFormatter.format(tx.amount)
                itemBinding.tvTxAmount.setTextColor(ContextCompat.getColor(this, R.color.accent_rose))
            }

            itemBinding.tvTxStatus.text = tx.status
            binding.layoutTransactions.addView(itemBinding.root)
        }
    }

    private fun updateActivityLedger() {
        binding.layoutActivityList.removeAllViews()

        var totalIn = 0.0
        var totalOut = 0.0

        val inflater = LayoutInflater.from(this)
        for (tx in transactions) {
            if (tx.isDeposit) totalIn += tx.amount else totalOut += tx.amount

            val itemBinding = ItemTransactionBinding.inflate(inflater, binding.layoutActivityList, false)
            itemBinding.tvTxTitle.text = tx.title
            itemBinding.tvTxDate.text = tx.date

            if (tx.isDeposit) {
                itemBinding.ivTxIcon.setImageResource(R.drawable.ic_arrow_down_left)
                itemBinding.ivTxIcon.setBackgroundResource(R.drawable.bg_action_circle_emerald)
                itemBinding.tvTxAmount.text = "+$" + currencyFormatter.format(tx.amount)
                itemBinding.tvTxAmount.setTextColor(ContextCompat.getColor(this, R.color.accent_primary))
            } else {
                itemBinding.ivTxIcon.setImageResource(R.drawable.ic_arrow_up_right)
                itemBinding.ivTxIcon.setBackgroundResource(R.drawable.bg_action_circle_rose)
                itemBinding.tvTxAmount.text = "-$" + currencyFormatter.format(tx.amount)
                itemBinding.tvTxAmount.setTextColor(ContextCompat.getColor(this, R.color.accent_rose))
            }

            itemBinding.tvTxStatus.text = tx.status
            binding.layoutActivityList.addView(itemBinding.root)
        }

        binding.tvTotalInflow.text = "+$" + currencyFormatter.format(totalIn)
        binding.tvTotalOutflow.text = "-$" + currencyFormatter.format(totalOut)
    }

    private fun showDepositSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_deposit, null)
        dialog.setContentView(sheetView)

        val etAmount = sheetView.findViewById<EditText>(R.id.etDepositAmount)
        val btnSubmit = sheetView.findViewById<View>(R.id.btnSubmitDeposit)

        sheetView.findViewById<View>(R.id.chipPreset50).setOnClickListener { etAmount.setText("50") }
        sheetView.findViewById<View>(R.id.chipPreset100).setOnClickListener { etAmount.setText("100") }
        sheetView.findViewById<View>(R.id.chipPreset500).setOnClickListener { etAmount.setText("500") }
        sheetView.findViewById<View>(R.id.chipPreset1000).setOnClickListener { etAmount.setText("1000") }

        btnSubmit.setOnClickListener {
            val input = etAmount.text.toString().trim()
            val amount = input.toDoubleOrNull()
            if (amount != null && amount > 0) {
                currentBalance += amount
                updateBalanceDisplay()

                val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
                val newTx = Transaction(
                    title = "Deposit - Bank Transfer",
                    date = "$timeStr • #TX-${(1000..9999).random()}",
                    amount = amount,
                    isDeposit = true,
                    status = "Completed"
                )
                transactions.add(0, newTx)
                renderTransactions()
                updateActivityLedger()

                Toast.makeText(this, "Success: $${currencyFormatter.format(amount)} added to your wallet!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showWithdrawSheet() {
        val dialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_withdraw, null)
        dialog.setContentView(sheetView)

        val etAmount = sheetView.findViewById<EditText>(R.id.etWithdrawAmount)
        val tvAvailable = sheetView.findViewById<TextView>(R.id.tvAvailableWithdraw)
        val btnMax = sheetView.findViewById<View>(R.id.btnMaxAmount)
        val btnSubmit = sheetView.findViewById<View>(R.id.btnSubmitWithdraw)

        tvAvailable.text = "Available: $" + currencyFormatter.format(currentBalance)

        btnMax.setOnClickListener {
            etAmount.setText(currentBalance.toString())
        }

        btnSubmit.setOnClickListener {
            val input = etAmount.text.toString().trim()
            val amount = input.toDoubleOrNull()
            if (amount != null && amount > 0) {
                if (amount > currentBalance) {
                    Toast.makeText(this, "Insufficient funds in your wallet", Toast.LENGTH_SHORT).show()
                } else {
                    currentBalance -= amount
                    updateBalanceDisplay()

                    val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date())
                    val newTx = Transaction(
                        title = "Withdrawal - Chase Bank",
                        date = "$timeStr • #TX-${(1000..9999).random()}",
                        amount = amount,
                        isDeposit = false,
                        status = "Completed"
                    )
                    transactions.add(0, newTx)
                    renderTransactions()
                    updateActivityLedger()

                    Toast.makeText(this, "Withdrawal submitted: $${currencyFormatter.format(amount)} processing", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showAddAccountDialog() {
        Toast.makeText(this, "Select Account Type: Bank Wire • UPI • USDT Wallet", Toast.LENGTH_LONG).show()
    }

    data class Transaction(
        val title: String,
        val date: String,
        val amount: Double,
        val isDeposit: Boolean,
        val status: String
    )
}
