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

        setupBottomNavigation()
        setupHomeScreen()
        setupDepositTab()
        setupWithdrawTab()
        setupDepositFlow()
        setupWithdrawFlow()
        setupSupportChat()
        setupAnnouncementBar()
    }

    // ========================================================
    // 1. BOTTOM NAVIGATION (HOME, DEPOSIT, WITHDRAWAL)
    // ========================================================
    private fun setupBottomNavigation() {
        binding.tabHome.setOnClickListener { switchTab(0) }
        binding.tabDeposit.setOnClickListener { switchTab(1) }
        binding.tabWithdrawal.setOnClickListener { switchTab(2) }
    }

    private fun switchTab(tabIndex: Int) {
        binding.containerHome.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.containerDepositTab.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.containerWithdrawTab.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE

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

        // Top Icons
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(this, "1 Notification: Welcome to Bpexch! Your account is active.", Toast.LENGTH_LONG).show()
        }

        binding.btnProfile.setOnClickListener {
            Toast.makeText(this, "Account: demo@3322 • Status: Active", Toast.LENGTH_SHORT).show()
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
    // 3. DEPOSIT TAB
    // ========================================================
    private fun setupDepositTab() {
        binding.btnOpenDepositFlow.setOnClickListener {
            openDepositFlow()
        }

        // Filter chips
        val chips = listOf(
            binding.chipDepositAll,
            binding.chipDepositApproved,
            binding.chipDepositPending,
            binding.chipDepositRejected
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                chips.forEach { c ->
                    c.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_filter_white)
                    c.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
                chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_filter_active_blue)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            }
        }
    }

    // ========================================================
    // 4. WITHDRAW TAB
    // ========================================================
    private fun setupWithdrawTab() {
        binding.btnOpenWithdrawFlow.setOnClickListener {
            openWithdrawFlow()
        }

        // Filter chips
        val chips = listOf(
            binding.chipWithdrawAll,
            binding.chipWithdrawApproved,
            binding.chipWithdrawPending,
            binding.chipWithdrawRejected
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                chips.forEach { c ->
                    c.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_filter_white)
                    c.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                }
                chip.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_filter_active_blue)
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            }
        }
    }

    // ========================================================
    // 5. DEPOSIT FLOW SCREEN (STEPS 1 & 2)
    // ========================================================
    private fun openDepositFlow() {
        binding.layoutMain.visibility = View.GONE
        binding.viewWithdrawFlow.visibility = View.GONE
        binding.viewDepositFlow.visibility = View.VISIBLE
        setDepositStep(1)
    }

    private fun closeDepositFlow() {
        binding.viewDepositFlow.visibility = View.GONE
        binding.layoutMain.visibility = View.VISIBLE
    }

    private fun setupDepositFlow() {
        binding.btnBackDepositFlow.setOnClickListener {
            if (binding.layoutDepositStep2.visibility == View.VISIBLE) {
                setDepositStep(1)
            } else {
                closeDepositFlow()
            }
        }

        binding.btnDepositCancel1.setOnClickListener { closeDepositFlow() }
        binding.btnDepositCancel2.setOnClickListener { closeDepositFlow() }

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
            closeDepositFlow()
            switchTab(1) // Return to Deposit tab
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
        binding.viewDepositFlow.scrollTo(0, 0)
    }

    // ========================================================
    // 6. WITHDRAW FLOW SCREEN (STEPS 1 & 2)
    // ========================================================
    private fun openWithdrawFlow() {
        binding.layoutMain.visibility = View.GONE
        binding.viewDepositFlow.visibility = View.GONE
        binding.viewWithdrawFlow.visibility = View.VISIBLE
        setWithdrawStep(1)
    }

    private fun closeWithdrawFlow() {
        binding.viewWithdrawFlow.visibility = View.GONE
        binding.layoutMain.visibility = View.VISIBLE
    }

    private fun setupWithdrawFlow() {
        binding.btnBackWithdrawFlow.setOnClickListener {
            if (binding.layoutWithdrawStep2.visibility == View.VISIBLE) {
                setWithdrawStep(1)
            } else {
                closeWithdrawFlow()
            }
        }

        binding.btnWithdrawCancel1.setOnClickListener { closeWithdrawFlow() }
        binding.btnWithdrawCancel2.setOnClickListener { closeWithdrawFlow() }

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
            closeWithdrawFlow()
            switchTab(2) // Return to Withdrawal tab
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
        binding.viewWithdrawFlow.scrollTo(0, 0)
    }

    // ========================================================
    // 6. ANNOUNCEMENT BAR (ENGAGEMENT & RETENTION)
    // ========================================================
    private fun setupAnnouncementBar() {
        val promos = listOf(
            Triple("100% WELCOME BONUS", "Double your first deposit up to Rs 25,000 today!", "3,420 users online • Instant 2-min JazzCash & Easypaisa approval"),
            Triple("INSTANT CASH-OUT", "JazzCash & Easypaisa withdrawals sent in under 2 minutes!", "Over Rs 4.8M paid out today • 0% withdrawal fee"),
            Triple("DAILY CASHBACK", "Get up to 10% weekly loss cashback automatically every Monday!", "VIP Player rewards program now active • Claim yours now")
        )
        var promoIndex = 0
        val handler = Handler(Looper.getMainLooper())
        val promoRunnable = object : Runnable {
            override fun run() {
                promoIndex = (promoIndex + 1) % promos.size
                val (tag, headline, sub) = promos[promoIndex]
                binding.tvAnnouncementTag.text = tag
                binding.tvAnnouncementHeadline.text = headline
                binding.tvAnnouncementSub.text = sub
                handler.postDelayed(this, 5000)
            }
        }
        handler.postDelayed(promoRunnable, 5000)

        binding.bannerAnnouncement.setOnClickListener {
            openDepositFlow()
        }
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
            handleQuickQuestion(
                "How to deposit funds?",
                "To make a deposit on Bpexch:\n\n1. Tap '+ Make a Deposit' from Home or Deposit tab\n2. Select your payment channel (Bank Alfalah, JazzCash, Easypaisa)\n3. Transfer to the official account number\n4. Attach your payment slip screenshot\n\nDeposits are verified automatically within 2 to 5 minutes."
            )
        }

        binding.chipQuickWithdraw.setOnClickListener {
            handleQuickQuestion(
                "What is the withdrawal processing speed?",
                "Bpexch cashouts are active 24/7/365:\n\n• JazzCash & Easypaisa: Under 5 minutes\n• Bank Transfer: 10 - 20 minutes\n• Payout fee: 0% Free\n\nSubmit your payout request directly from the 'Withdrawal' tab anytime."
            )
        }

        binding.chipQuickBonus.setOnClickListener {
            handleQuickQuestion(
                "How do I claim the 100% Welcome Bonus?",
                "You qualify for our 100% First Deposit Bonus:\n\n• Min Deposit: Rs 500\n• Max Bonus: Rs 25,000\n• Bonus Code: FIRST100\n\nMake your first deposit and contact support with your transaction slip to receive your matching bonus immediately."
            )
        }

        binding.chipQuickPassword.setOnClickListener {
            handleQuickQuestion(
                "How do I change my account password?",
                "For the security of account demo@3322, password updates are verified by our security desk.\n\nPlease confirm your registered phone number or message us on WhatsApp for an instant reset code."
            )
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
            val reply = when {
                text.contains("deposit", ignoreCase = true) ->
                    "To make a deposit, tap the '+ Make a Deposit' button on the Deposit tab or the Deposit card on the Home screen. Our verified company accounts are updated live."
                text.contains("withdraw", ignoreCase = true) || text.contains("cashout", ignoreCase = true) ->
                    "Withdrawals are processed 24/7 with zero fees. Average payout time is 2 to 5 minutes via JazzCash or Easypaisa."
                text.contains("bonus", ignoreCase = true) || text.contains("promo", ignoreCase = true) ->
                    "You have a 100% First Deposit bonus waiting on your account demo@3322! Double your deposit up to Rs 25,000 today."
                text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) || text.contains("hey", ignoreCase = true) ->
                    "Hello! Welcome to Bpexch Live Support. How can we help you today with your wallet or account?"
                else ->
                    "Thank you for contacting Bpexch Live Support. A representative has received your message regarding \"$text\" and will assist you momentarily."
            }
            addWhatsAppBotMessage(reply)
        }, 600)
    }

    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    private fun addWhatsAppUserMessage(text: String) {
        val density = resources.displayMetrics.density
        val userBubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_user)
            val pad = (10 * density).toInt()
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

            val tv = TextView(this@MainActivity).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                textSize = 13.5f
            }
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
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                marginEnd = (44 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
            layoutParams = lp

            val tv = TextView(this@MainActivity).apply {
                this.text = text
                setTextColor(android.graphics.Color.parseColor("#111B21"))
                textSize = 13.5f
            }
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
}
