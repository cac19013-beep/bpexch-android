package com.example.simpleapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.simpleapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isPasswordVisible = false
    private val rawPassword = "demo@pass123"
    private var activeLanguage = "EN"

    // Auth screen state
    private var isAuthPasswordVisible = false
    private var isCreateAccountMode = false

    // Withdraw flow state
    private var selectedWithdrawMethod = "JazzCash"
    private var selectedWithdrawAmount = 1000

    // Chat support state
    private var isChatFullscreen = false
    private var selectedReceiptUri: Uri? = null

    // Real Photo / Receipt Pickers (Android SAF - No runtime permission denials)
    private val pickReceiptLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedReceiptUri = uri
            binding.tvReceiptStatus.text = "Payment proof attached ✓"
            binding.tvReceiptStatus.setTextColor(ContextCompat.getColor(this, R.color.deposit_green_dark))
            binding.ivReceiptPreview.setImageURI(uri)
            binding.ivReceiptPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            binding.ivReceiptPreview.setPadding(0, 0, 0, 0)
            Toast.makeText(this, "Receipt photo selected successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickChatAttachmentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            addWhatsAppImageMessage(uri, "Payment Screenshot Proof")
            Handler(Looper.getMainLooper()).postDelayed({
                addWhatsAppBotMessage("Receipt screenshot received! Our finance officer is verifying your payment details.")
            }, 800)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAuthScreen()
        setupBottomNavigation()
        setupHomeScreen()
        setupDepositTab()
        setupWithdrawTab()
        setupHistoryTab()
        setupSupportChat()
        setupAnnouncementBar()
    }

    // ========================================================
    // 0. AUTHENTICATION & LOGIN LOGIC
    // ========================================================
    private fun setupAuthScreen() {
        // Toggle Password Visibility
        binding.btnAuthTogglePassword.setOnClickListener {
            isAuthPasswordVisible = !isAuthPasswordVisible
            if (isAuthPasswordVisible) {
                binding.etAuthPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.btnAuthTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etAuthPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.btnAuthTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            binding.etAuthPassword.setSelection(binding.etAuthPassword.text.length)
        }

        // Login Button
        binding.btnAuthLogin.setOnClickListener {
            val username = binding.etAuthUsername.text.toString().trim()
            val password = binding.etAuthPassword.text.toString().trim()

            if (username.isEmpty()) {
                binding.etAuthUsername.error = "Please enter username or WhatsApp number"
                binding.etAuthUsername.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.etAuthPassword.error = "Please enter password"
                binding.etAuthPassword.requestFocus()
                return@setOnClickListener
            }

            // Successfully Authenticated: reveal Main Layout
            binding.tvUsernameValue.text = username
            binding.containerAuth.visibility = View.GONE
            binding.layoutMain.visibility = View.VISIBLE
            switchTab(0)
            Toast.makeText(this, "Welcome back, $username! Logged in successfully.", Toast.LENGTH_SHORT).show()
        }

        // Create Account Box
        binding.boxCreateAccount.setOnClickListener {
            isCreateAccountMode = !isCreateAccountMode
            if (isCreateAccountMode) {
                binding.tvAuthSubtitle.visibility = View.VISIBLE
                binding.tvAuthSubtitle.text = "Create your new account in 1 minute"
                binding.tvCreateAccountSubtitle.visibility = View.VISIBLE
                binding.btnAuthLogin.text = "Register Account"
                binding.tvCreateAccountTitle.text = "Already have an account?"
                binding.tvCreateAccountSubtitle.text = "Switch back to member login"
            } else {
                binding.tvAuthSubtitle.visibility = View.GONE
                binding.tvCreateAccountSubtitle.visibility = View.GONE
                binding.btnAuthLogin.text = "Login"
                binding.tvCreateAccountTitle.text = "Create Account"
            }
        }

        // WhatsApp Help Box
        binding.boxWhatsApp.setOnClickListener {
            openSupportChat()
        }

        // Dummy Video Link Box
        binding.boxDummyVideo.setOnClickListener {
            showVideoGuideDialog()
        }
    }

    // ========================================================
    // 1. BOTTOM NAVIGATION (HOME, DEPOSIT, WITHDRAWAL, HISTORY)
    // ========================================================
    private fun setupBottomNavigation() {
        binding.tabHome.setOnClickListener { switchTab(0) }
        binding.tabDeposit.setOnClickListener { switchTab(1) }
        binding.tabWithdrawal.setOnClickListener { switchTab(2) }
        binding.tabHistory.setOnClickListener { switchTab(3) }
    }

    private fun switchTab(tabIndex: Int) {
        binding.containerHome.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.containerDepositTab.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.containerWithdrawTab.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        binding.containerHistoryTab.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE

        binding.mainScrollView.scrollTo(0, 0)

        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_active_blue)
        val activeColor = ContextCompat.getColor(this, R.color.primary_blue)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        // Tab 0: Home
        binding.tabHome.background = if (tabIndex == 0) activeBg else null
        binding.ivTabHome.setColorFilter(if (tabIndex == 0) activeColor else inactiveColor)
        binding.tvTabHome.setTextColor(if (tabIndex == 0) activeColor else inactiveColor)

        // Tab 1: Deposit
        binding.tabDeposit.background = if (tabIndex == 1) activeBg else null
        binding.ivTabDeposit.setColorFilter(if (tabIndex == 1) activeColor else inactiveColor)
        binding.tvTabDeposit.setTextColor(if (tabIndex == 1) activeColor else inactiveColor)

        // Tab 2: Withdrawal
        binding.tabWithdrawal.background = if (tabIndex == 2) activeBg else null
        binding.ivTabWithdrawal.setColorFilter(if (tabIndex == 2) activeColor else inactiveColor)
        binding.tvTabWithdrawal.setTextColor(if (tabIndex == 2) activeColor else inactiveColor)

        // Tab 3: History
        binding.tabHistory.background = if (tabIndex == 3) activeBg else null
        binding.ivTabHistory.setColorFilter(if (tabIndex == 3) activeColor else inactiveColor)
        binding.tvTabHistory.setTextColor(if (tabIndex == 3) activeColor else inactiveColor)
    }

    // ========================================================
    // 2. HOME SCREEN LOGIC
    // ========================================================
    private fun setupHomeScreen() {
        // Copy Username
        binding.btnCopyUsername.setOnClickListener {
            copyToClipboard("Username", binding.tvUsernameValue.text.toString())
        }

        // Toggle Password visibility
        binding.btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.tvPasswordValue.text = rawPassword
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.tvPasswordValue.text = "••••••"
                binding.btnTogglePassword.setImageResource(R.drawable.ic_eye)
            }
        }

        // Copy Password
        binding.btnCopyPassword.setOnClickListener {
            copyToClipboard("Password", rawPassword)
        }

        // Open Website Button
        binding.btnOpenWebsite.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bpexch.com"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Opening Bpexch Website...", Toast.LENGTH_SHORT).show()
            }
        }

        // Quick Actions: Deposit & Withdraw Cards
        binding.cardHomeDeposit.setOnClickListener {
            openDepositFlow()
        }

        binding.cardHomeWithdraw.setOnClickListener {
            openWithdrawFlow()
        }

        // WhatsApp / Help Card
        binding.cardHomeWhatsApp.setOnClickListener {
            openSupportChat()
        }

        // Language Pill Toggle
        binding.pillLanguage.setOnClickListener {
            toggleLanguage()
        }

        // Top Icons: Bottom Sheets instead of Toasts
        binding.btnNotifications.setOnClickListener {
            showNotificationsBottomSheet()
        }

        binding.btnProfile.setOnClickListener {
            showProfileBottomSheet()
        }
    }

    private fun toggleLanguage() {
        if (activeLanguage == "EN") {
            activeLanguage = "UR"
            binding.tvLangEn.background = null
            binding.tvLangEn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvLangUr.background = ContextCompat.getDrawable(this, R.drawable.bg_pill_lang_selected)
            binding.tvLangUr.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            Toast.makeText(this, "زبان تبدیل کردی گئی ہے", Toast.LENGTH_SHORT).show()
        } else {
            activeLanguage = "EN"
            binding.tvLangUr.background = null
            binding.tvLangUr.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvLangEn.background = ContextCompat.getDrawable(this, R.drawable.bg_pill_lang_selected)
            binding.tvLangEn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            Toast.makeText(this, "Language switched to English", Toast.LENGTH_SHORT).show()
        }
    }

    // ========================================================
    // 3. DEPOSIT FLOW / HELPERS
    // ========================================================
    private fun openDepositFlow() {
        switchTab(1)
        setDepositStep(1)
    }

    private fun closeDepositFlow() {
        setDepositStep(1)
        switchTab(0)
    }

    private fun setupDepositTab() {
        setDepositStep(1)

        binding.btnBackDepositFlow.setOnClickListener {
            if (binding.layoutDepositStep2.visibility == View.VISIBLE) {
                setDepositStep(1)
            } else {
                switchTab(0)
            }
        }

        binding.btnDepositCancel1.setOnClickListener { switchTab(0) }
        binding.btnDepositCancel2.setOnClickListener { setDepositStep(1) }

        // Bank selection
        binding.cardBankAlfalah.setOnClickListener {
            binding.rbBankAlfalah.isChecked = true
        }

        // Step 1 -> Next
        binding.btnDepositNext.setOnClickListener {
            setDepositStep(2)
        }

        // Step 2: Copy buttons
        binding.btnCopyDepositAcc.setOnClickListener {
            copyToClipboard("Account Number", binding.tvDepositAccNumber.text.toString())
        }

        binding.btnCopyDepositIban.setOnClickListener {
            copyToClipboard("IBAN", binding.tvDepositIban.text.toString())
        }

        // Attach receipt via real photo picker
        binding.boxAttachReceipt.setOnClickListener {
            try {
                pickReceiptLauncher.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(this, "Opening gallery...", Toast.LENGTH_SHORT).show()
            }
        }

        // Submit request
        binding.btnDepositSubmit.setOnClickListener {
            val amountStr = binding.etDepositAmountFlow.text.toString().trim()
            val amount = amountStr.toIntOrNull() ?: 500

            if (amount < 500) {
                Toast.makeText(this, "Minimum deposit is Rs 500", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Deposit request of Rs $amount submitted successfully!", Toast.LENGTH_LONG).show()
            setDepositStep(1)
            switchTab(3) // Switch to History tab
            switchHistorySection(0) // Show Deposit History
        }
    }

    private fun setDepositStep(step: Int) {
        if (step == 1) {
            binding.layoutDepositStep1.visibility = View.VISIBLE
            binding.layoutDepositStep2.visibility = View.GONE

            binding.tvDepositStep1Icon.text = "1"
            binding.tvDepositStep1Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deposit_green_light)
            binding.tvDepositStep1Icon.setTextColor(ContextCompat.getColor(this, R.color.deposit_green_dark))

            binding.lineDepositStep.setBackgroundColor(ContextCompat.getColor(this, R.color.border_subtle))

            binding.tvDepositStep2Icon.text = "2"
            binding.tvDepositStep2Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.bg_input)
            binding.tvDepositStep2Icon.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvDepositStep2Label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.layoutDepositStep1.visibility = View.GONE
            binding.layoutDepositStep2.visibility = View.VISIBLE

            binding.tvDepositStep1Icon.text = "✓"
            binding.tvDepositStep1Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deposit_green)
            binding.tvDepositStep1Icon.setTextColor(ContextCompat.getColor(this, R.color.text_white))

            binding.lineDepositStep.setBackgroundColor(ContextCompat.getColor(this, R.color.deposit_green))

            binding.tvDepositStep2Icon.text = "2"
            binding.tvDepositStep2Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deposit_green_light)
            binding.tvDepositStep2Icon.setTextColor(ContextCompat.getColor(this, R.color.deposit_green_dark))
            binding.tvDepositStep2Label.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
        binding.mainScrollView.scrollTo(0, 0)
    }

    // ========================================================
    // 4. WITHDRAW FLOW / HELPERS
    // ========================================================
    private fun openWithdrawFlow() {
        switchTab(2)
        setWithdrawStep(1)
    }

    private fun closeWithdrawFlow() {
        setWithdrawStep(1)
        switchTab(0)
    }

    private fun setupWithdrawTab() {
        setWithdrawStep(1)

        binding.btnBackWithdrawFlow.setOnClickListener {
            if (binding.layoutWithdrawStep2.visibility == View.VISIBLE) {
                setWithdrawStep(1)
            } else {
                switchTab(0)
            }
        }

        binding.btnWithdrawCancel1.setOnClickListener { switchTab(0) }
        binding.btnWithdrawCancel2.setOnClickListener { setWithdrawStep(1) }

        // Presets: 1,000 / 5,000 / 10,000
        binding.chipWithdraw1000.setOnClickListener {
            binding.etWithdrawAmountFlow.setText("1000")
            selectedWithdrawAmount = 1000
        }

        binding.chipWithdraw5000.setOnClickListener {
            binding.etWithdrawAmountFlow.setText("5000")
            selectedWithdrawAmount = 5000
        }

        binding.chipWithdraw10000.setOnClickListener {
            binding.etWithdrawAmountFlow.setText("10000")
            selectedWithdrawAmount = 10000
        }

        // Method selection: JazzCash, Easypaisa & SadaPay
        binding.rowJazzCash.setOnClickListener {
            selectedWithdrawMethod = "JazzCash"
            binding.rbJazzCash.isChecked = true
            binding.rbEasypaisa.isChecked = false
            binding.rbSadaPay.isChecked = false
        }

        binding.rowEasypaisa.setOnClickListener {
            selectedWithdrawMethod = "Easypaisa"
            binding.rbJazzCash.isChecked = false
            binding.rbEasypaisa.isChecked = true
            binding.rbSadaPay.isChecked = false
        }

        binding.rowSadaPay.setOnClickListener {
            selectedWithdrawMethod = "SadaPay"
            binding.rbJazzCash.isChecked = false
            binding.rbEasypaisa.isChecked = false
            binding.rbSadaPay.isChecked = true
        }

        // Step 1 -> Next
        binding.btnWithdrawNext.setOnClickListener {
            val amountStr = binding.etWithdrawAmountFlow.text.toString().trim()
            val amount = amountStr.toIntOrNull() ?: 1000

            if (amount < 500) {
                Toast.makeText(this, "Minimum withdrawal is Rs 500", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedWithdrawAmount = amount

            // Populate Step 2
            binding.tvWithdrawStep2Amount.text = "Rs $selectedWithdrawAmount"
            binding.tvWithdrawStep2Method.text = selectedWithdrawMethod
            binding.tvWithdrawWalletNameBox.text = selectedWithdrawMethod

            when (selectedWithdrawMethod) {
                "JazzCash" -> {
                    binding.tvWithdrawStep2MethodBadge.text = "JC"
                    binding.tvWithdrawStep2MethodBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.jazzcash_pink)
                    binding.tvWithdrawStep2MethodName.text = "JazzCash"
                    binding.tvWithdrawNumberLabel.text = "JazzCash Number"
                }
                "Easypaisa" -> {
                    binding.tvWithdrawStep2MethodBadge.text = "EP"
                    binding.tvWithdrawStep2MethodBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.easypaisa_green)
                    binding.tvWithdrawStep2MethodName.text = "Easypaisa"
                    binding.tvWithdrawNumberLabel.text = "Easypaisa Number"
                }
                else -> {
                    binding.tvWithdrawStep2MethodBadge.text = "SP"
                    binding.tvWithdrawStep2MethodBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.deposit_green_dark)
                    binding.tvWithdrawStep2MethodName.text = "SadaPay"
                    binding.tvWithdrawNumberLabel.text = "SadaPay Mobile / IBAN"
                }
            }

            setWithdrawStep(2)
        }

        // Step 2 -> Submit
        binding.btnWithdrawSubmit.setOnClickListener {
            val title = binding.etWithdrawAccountTitle.text.toString().trim()
            val number = binding.etWithdrawNumber.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter your Account Title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (number.isEmpty()) {
                Toast.makeText(this, "Please enter your $selectedWithdrawMethod Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Withdrawal request of Rs $selectedWithdrawAmount submitted for $title ($number)!", Toast.LENGTH_LONG).show()
            setWithdrawStep(1)
            switchTab(3) // Switch to History tab
            switchHistorySection(1) // Show Withdraw History
        }
    }

    private fun setWithdrawStep(step: Int) {
        if (step == 1) {
            binding.layoutWithdrawStep1.visibility = View.VISIBLE
            binding.layoutWithdrawStep2.visibility = View.GONE

            binding.tvWithdrawStep1Icon.text = "1"
            binding.tvWithdrawStep1Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.withdraw_orange_border)
            binding.tvWithdrawStep1Icon.setTextColor(ContextCompat.getColor(this, R.color.withdraw_orange_dark))

            binding.lineWithdrawStep.setBackgroundColor(ContextCompat.getColor(this, R.color.border_subtle))

            binding.tvWithdrawStep2Icon.text = "2"
            binding.tvWithdrawStep2Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.bg_input)
            binding.tvWithdrawStep2Icon.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvWithdrawStep2Label.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            binding.layoutWithdrawStep1.visibility = View.GONE
            binding.layoutWithdrawStep2.visibility = View.VISIBLE

            binding.tvWithdrawStep1Icon.text = "✓"
            binding.tvWithdrawStep1Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.withdraw_orange)
            binding.tvWithdrawStep1Icon.setTextColor(ContextCompat.getColor(this, R.color.text_white))

            binding.lineWithdrawStep.setBackgroundColor(ContextCompat.getColor(this, R.color.withdraw_orange))

            binding.tvWithdrawStep2Icon.text = "2"
            binding.tvWithdrawStep2Icon.backgroundTintList = ContextCompat.getColorStateList(this, R.color.withdraw_orange_border)
            binding.tvWithdrawStep2Icon.setTextColor(ContextCompat.getColor(this, R.color.withdraw_orange_dark))
            binding.tvWithdrawStep2Label.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }
        binding.mainScrollView.scrollTo(0, 0)
    }

    // ========================================================
    // 5. HISTORY TAB (DEPOSIT & WITHDRAWAL)
    // ========================================================
    private fun switchHistorySection(sectionIndex: Int) {
        val activePillBg = ContextCompat.getDrawable(this, R.drawable.bg_segmented_item_active)
        val activeTextColor = ContextCompat.getColor(this, R.color.text_white)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        binding.layoutHistoryDeposit.visibility = if (sectionIndex == 0) View.VISIBLE else View.GONE
        binding.layoutHistoryWithdraw.visibility = if (sectionIndex == 1) View.VISIBLE else View.GONE

        // Button 1: Deposit History
        binding.btnHistoryDeposit.background = if (sectionIndex == 0) activePillBg else null
        binding.btnHistoryDeposit.setTextColor(if (sectionIndex == 0) activeTextColor else inactiveTextColor)

        // Button 2: Withdraw History
        binding.btnHistoryWithdraw.background = if (sectionIndex == 1) activePillBg else null
        binding.btnHistoryWithdraw.setTextColor(if (sectionIndex == 1) activeTextColor else inactiveTextColor)
    }

    private fun setupHistoryTab() {
        binding.btnHistoryDeposit.setOnClickListener { switchHistorySection(0) }
        binding.btnHistoryWithdraw.setOnClickListener { switchHistorySection(1) }

        // Setup Deposit Filter Chips
        val depositChips = listOf(
            binding.chipDepositAll,
            binding.chipDepositApproved,
            binding.chipDepositPending,
            binding.chipDepositRejected
        )
        fun updateDepositFilter(filterIndex: Int) {
            depositChips.forEachIndexed { i, chip ->
                val isActive = (i == filterIndex)
                chip.background = ContextCompat.getDrawable(
                    this,
                    if (isActive) R.drawable.bg_chip_filter_active_blue else R.drawable.bg_chip_filter_white
                )
                chip.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (isActive) R.color.text_white else R.color.text_secondary
                    )
                )
            }
            when (filterIndex) {
                0 -> {
                    binding.cardDepositApprovedItem.visibility = View.VISIBLE
                    binding.cardDepositPendingItem.visibility = View.VISIBLE
                    binding.cardDepositRejectedItem.visibility = View.VISIBLE
                    binding.cardDepositEmpty.visibility = View.GONE
                }
                1 -> {
                    binding.cardDepositApprovedItem.visibility = View.VISIBLE
                    binding.cardDepositPendingItem.visibility = View.GONE
                    binding.cardDepositRejectedItem.visibility = View.GONE
                    binding.cardDepositEmpty.visibility = View.GONE
                }
                2 -> {
                    binding.cardDepositApprovedItem.visibility = View.GONE
                    binding.cardDepositPendingItem.visibility = View.VISIBLE
                    binding.cardDepositRejectedItem.visibility = View.GONE
                    binding.cardDepositEmpty.visibility = View.GONE
                }
                3 -> {
                    binding.cardDepositApprovedItem.visibility = View.GONE
                    binding.cardDepositPendingItem.visibility = View.GONE
                    binding.cardDepositRejectedItem.visibility = View.VISIBLE
                    binding.cardDepositEmpty.visibility = View.GONE
                }
            }
        }
        depositChips.forEachIndexed { index, chip ->
            chip.setOnClickListener { updateDepositFilter(index) }
        }
        updateDepositFilter(0)

        // Setup Withdraw Filter Chips
        val withdrawChips = listOf(
            binding.chipWithdrawAll,
            binding.chipWithdrawApproved,
            binding.chipWithdrawPending,
            binding.chipWithdrawRejected
        )
        fun updateWithdrawFilter(filterIndex: Int) {
            withdrawChips.forEachIndexed { i, chip ->
                val isActive = (i == filterIndex)
                chip.background = ContextCompat.getDrawable(
                    this,
                    if (isActive) R.drawable.bg_chip_filter_active_blue else R.drawable.bg_chip_filter_white
                )
                chip.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (isActive) R.color.text_white else R.color.text_secondary
                    )
                )
            }
            when (filterIndex) {
                0 -> {
                    binding.cardWithdrawApprovedItem.visibility = View.VISIBLE
                    binding.cardWithdrawPendingItem.visibility = View.VISIBLE
                    binding.cardWithdrawEmpty.visibility = View.GONE
                }
                1 -> {
                    binding.cardWithdrawApprovedItem.visibility = View.VISIBLE
                    binding.cardWithdrawPendingItem.visibility = View.GONE
                    binding.cardWithdrawEmpty.visibility = View.GONE
                }
                2 -> {
                    binding.cardWithdrawApprovedItem.visibility = View.GONE
                    binding.cardWithdrawPendingItem.visibility = View.VISIBLE
                    binding.cardWithdrawEmpty.visibility = View.GONE
                }
                3 -> {
                    binding.cardWithdrawApprovedItem.visibility = View.GONE
                    binding.cardWithdrawPendingItem.visibility = View.GONE
                    binding.cardWithdrawEmpty.visibility = View.VISIBLE
                }
            }
        }
        withdrawChips.forEachIndexed { index, chip ->
            chip.setOnClickListener { updateWithdrawFilter(index) }
        }
        updateWithdrawFilter(0)

        // Default to Deposit History
        switchHistorySection(0)
    }

    // ========================================================
    // 6. IMPORTANT UPDATE BANNER
    // ========================================================
    private fun setupAnnouncementBar() {
        binding.tvAnnouncementTag.text = "Important Update"
        binding.tvAnnouncementHeadline.text = "Check the official website for current account and payment information."
        binding.tvAnnouncementSub.text = "Confirm details through official channels before making a transaction."
    }

    // ========================================================
    // 7. FLOATING LIVE SUPPORT CHAT (WHATSAPP UI/UX & QUICK REPLIES)
    // ========================================================
    private fun setupSupportChat() {
        binding.fabSupportChat.setOnClickListener {
            openSupportChat()
        }

        binding.btnChatBack.setOnClickListener {
            closeSupportChat()
        }

        binding.btnChatClose.setOnClickListener {
            closeSupportChat()
        }

        binding.viewChatScrim.setOnClickListener {
            closeSupportChat()
        }

        binding.btnChatFullscreen.setOnClickListener {
            toggleChatFullscreen()
        }

        // WhatsApp Quick Questions (Triggered with clean text)
        binding.chipQuickDeposit.setOnClickListener {
            val reply = SupportContent.quickReplies.first { it.question == "How to deposit funds?" }
            handleQuickQuestion(reply.question, reply.answer)
        }

        binding.chipQuickWithdraw.setOnClickListener {
            val reply = SupportContent.quickReplies.first { it.question == "What is the withdrawal processing speed?" }
            handleQuickQuestion(reply.question, reply.answer)
        }

        binding.chipQuickAccount.setOnClickListener {
            val reply = SupportContent.quickReplies.first { it.question == "How can I get account help?" }
            handleQuickQuestion(reply.question, reply.answer)
        }

        binding.chipQuickPassword.setOnClickListener {
            val reply = SupportContent.quickReplies.first { it.question == "How do I change my account password?" }
            handleQuickQuestion(reply.question, reply.answer)
        }

        binding.chipQuickWhatsApp.setOnClickListener {
            openWhatsApp("923001234567", "Hello Bpexch Official Support, I am contacting you from the app regarding account: demo@3322")
        }

        // Real Screenshot / Slip Attachment via system picker
        binding.btnChatAttach.setOnClickListener {
            try {
                pickChatAttachmentLauncher.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(this, "Opening gallery...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChatCamera.setOnClickListener {
            try {
                pickChatAttachmentLauncher.launch("image/*")
            } catch (e: Exception) {
                Toast.makeText(this, "Opening gallery...", Toast.LENGTH_SHORT).show()
            }
        }

        // WhatsApp Send Button
        binding.btnChatSend.setOnClickListener {
            sendUserTypedMessage()
        }

        binding.etChatMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendUserTypedMessage()
                true
            } else false
        }
    }

    private fun openSupportChat() {
        binding.layoutSupportChatModal.visibility = View.VISIBLE
    }

    private fun closeSupportChat() {
        binding.layoutSupportChatModal.visibility = View.GONE
    }

    private fun toggleChatFullscreen() {
        isChatFullscreen = !isChatFullscreen
        val density = resources.displayMetrics.density

        if (isChatFullscreen) {
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(0, 0, 0, 0)
            }
            binding.cardChatContainer.layoutParams = params
            binding.btnChatFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            val heightPx = (540 * density).toInt()
            val marginH = (8 * density).toInt()
            val marginB = (8 * density).toInt()
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(marginH, 0, marginH, marginB)
            }
            binding.cardChatContainer.layoutParams = params
            binding.btnChatFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    private fun handleQuickQuestion(question: String, answer: String) {
        addWhatsAppUserMessage(question)
        Handler(Looper.getMainLooper()).postDelayed({
            addWhatsAppBotMessage(answer)
        }, 500)
    }

    private fun sendUserTypedMessage() {
        val text = binding.etChatMessage.text.toString().trim()
        if (text.isEmpty()) return
        binding.etChatMessage.setText("")
        addWhatsAppUserMessage(text)

        Handler(Looper.getMainLooper()).postDelayed({
            val reply = SupportReplyHelper.getReply(text)
            addWhatsAppBotMessage(reply)
        }, 600)
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    private fun chatBubbleWidth(textView: TextView, text: String, padding: Int, sideMargin: Int): Int {
        val density = resources.displayMetrics.density
        val scrollView = binding.chatScrollView
        val availableWidth = scrollView.width - scrollView.paddingLeft - scrollView.paddingRight - sideMargin
        val minimumWidth = (72 * density).toInt()
        val maximumWidth = (availableWidth * 0.9f).toInt().coerceAtLeast(minimumWidth)
        val textWidth = text.lineSequence().maxOfOrNull { textView.paint.measureText(it).toInt() } ?: 0
        return (textWidth + padding * 2).coerceIn(minimumWidth, maximumWidth)
    }

    private fun addWhatsAppUserMessage(text: String) {
        val density = resources.displayMetrics.density
        val userBubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_user)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 1f * density
            val sideMargin = (44 * density).toInt()
            val tv = TextView(this@MainActivity).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 13.5f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val lp = LinearLayout.LayoutParams(
                chatBubbleWidth(tv, text, pad, sideMargin),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginStart = sideMargin
                bottomMargin = (8 * density).toInt()
            }
            layoutParams = lp
            addView(tv)

            // Bottom row with timestamp and double checkmarks
            val metaRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                val metaLp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END
                    topMargin = (3 * density).toInt()
                }
                layoutParams = metaLp

                val timeTv = TextView(this@MainActivity).apply {
                    this.text = getCurrentTimeString()
                    setTextColor(android.graphics.Color.parseColor("#667781"))
                    textSize = 10f
                }
                addView(timeTv)

                val checkIv = ImageView(this@MainActivity).apply {
                    setImageResource(R.drawable.ic_double_check)
                    val checkLp = LinearLayout.LayoutParams(
                        (14 * density).toInt(),
                        (14 * density).toInt()
                    ).apply {
                        marginStart = (4 * density).toInt()
                    }
                    layoutParams = checkLp
                }
                addView(checkIv)
            }
            addView(metaRow)
        }
        binding.layoutChatMessages.addView(userBubble)
        binding.chatScrollView.post {
            binding.chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun addWhatsAppImageMessage(uri: Uri, caption: String) {
        val density = resources.displayMetrics.density
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_user)
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 1f * density
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                marginStart = (44 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
            layoutParams = lp

            // Image Preview
            val iv = ImageView(this@MainActivity).apply {
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                val imgLp = LinearLayout.LayoutParams(
                    (200 * density).toInt(),
                    (180 * density).toInt()
                ).apply {
                    bottomMargin = (4 * density).toInt()
                }
                layoutParams = imgLp
            }
            addView(iv)

            val tv = TextView(this@MainActivity).apply {
                this.text = caption
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 12.5f
            }
            addView(tv)

            // Meta row
            val metaRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                val metaLp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END
                    topMargin = (2 * density).toInt()
                }
                layoutParams = metaLp

                val timeTv = TextView(this@MainActivity).apply {
                    this.text = getCurrentTimeString()
                    setTextColor(android.graphics.Color.parseColor("#667781"))
                    textSize = 10f
                }
                addView(timeTv)

                val checkIv = ImageView(this@MainActivity).apply {
                    setImageResource(R.drawable.ic_double_check)
                    val checkLp = LinearLayout.LayoutParams(
                        (14 * density).toInt(),
                        (14 * density).toInt()
                    ).apply {
                        marginStart = (4 * density).toInt()
                    }
                    layoutParams = checkLp
                }
                addView(checkIv)
            }
            addView(metaRow)
        }
        binding.layoutChatMessages.addView(bubble)
        binding.chatScrollView.post {
            binding.chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun addWhatsAppBotMessage(text: String) {
        val density = resources.displayMetrics.density
        val botBubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_bot)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 1f * density
            val sideMargin = (44 * density).toInt()
            val tv = TextView(this@MainActivity).apply {
                this.text = text
                setTextColor(android.graphics.Color.parseColor("#111B21"))
                textSize = 13.5f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val lp = LinearLayout.LayoutParams(
                chatBubbleWidth(tv, text, pad, sideMargin),
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                marginEnd = sideMargin
                bottomMargin = (8 * density).toInt()
            }
            layoutParams = lp
            addView(tv)

            val timeTv = TextView(this@MainActivity).apply {
                this.text = getCurrentTimeString()
                setTextColor(android.graphics.Color.parseColor("#667781"))
                textSize = 10f
                val timeLp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.END
                    topMargin = (3 * density).toInt()
                }
                layoutParams = timeLp
            }
            addView(timeTv)
        }
        binding.layoutChatMessages.addView(botBubble)
        binding.chatScrollView.post {
            binding.chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    // ========================================================
    // HELPER UTILITIES
    // ========================================================
    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun openWhatsApp(phoneNumber: String, message: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Opening WhatsApp for $phoneNumber", Toast.LENGTH_SHORT).show()
        }
    }

    // ========================================================
    // RICH BOTTOM SHEETS & DIALOGS
    // ========================================================
    private fun showNotificationsBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.layout_notifications_sheet, null)
        dialog.setContentView(sheetView)

        val tvCount = sheetView.findViewById<TextView>(R.id.tvNotificationCount)
        val btnMarkRead = sheetView.findViewById<TextView>(R.id.btnMarkAllRead)
        val btnClose = sheetView.findViewById<View>(R.id.btnDismissNotifications)

        btnMarkRead?.setOnClickListener {
            tvCount?.text = "0 New"
            tvCount?.visibility = View.GONE
            btnMarkRead.visibility = View.GONE
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showProfileBottomSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.layout_profile_sheet, null)
        dialog.setContentView(sheetView)

        val tvUsername = sheetView.findViewById<TextView>(R.id.tvProfileSheetUsername)
        tvUsername?.text = binding.tvUsernameValue.text.toString()

        val btnWhatsApp = sheetView.findViewById<View>(R.id.btnProfileWhatsApp)
        btnWhatsApp?.setOnClickListener {
            dialog.dismiss()
            openSupportChat()
        }

        val btnLiveChat = sheetView.findViewById<View>(R.id.btnProfileLiveChat)
        btnLiveChat?.setOnClickListener {
            dialog.dismiss()
            openSupportChat()
        }

        val btnLogout = sheetView.findViewById<View>(R.id.btnProfileLogout)
        btnLogout?.setOnClickListener {
            dialog.dismiss()
            // Return back to Authentication / Login screen
            binding.layoutMain.visibility = View.GONE
            binding.containerAuth.visibility = View.VISIBLE
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showVideoGuideDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_video_guide, null)
        dialog.setContentView(sheetView)

        val btnWhatsApp = sheetView.findViewById<View>(R.id.btnVideoWhatsAppAgent)
        val btnDismiss = sheetView.findViewById<View>(R.id.btnDismissVideoGuide)
        val cardVideo = sheetView.findViewById<View>(R.id.cardVideoPlayer)

        btnWhatsApp?.setOnClickListener {
            dialog.dismiss()
            openSupportChat()
        }
        cardVideo?.setOnClickListener {
            Toast.makeText(this, "Playing video tutorial: How to Create Account & Deposit", Toast.LENGTH_SHORT).show()
        }
        btnDismiss?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
