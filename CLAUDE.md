# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"BP Exch Wallet" — a single-module Android app (`:app`, package `com.example.bpexch`, Kotlin, minSdk 24 / compileSdk 34, ViewBinding, Material 3, no Compose). The git repo root is `bpexch-android/`; nearly all work happens under `app/src/main/`.

## Commands

Run from the repo root:

- Build debug APK: `./gradlew assembleDebug`
- Install on device/emulator: `./gradlew installDebug`
- Lint: `./gradlew lint`
- Unit / instrumented tests: `./gradlew test` / `./gradlew connectedAndroidTest` (no tests exist yet; there is no `src/test` or `src/androidTest`)

## Architecture

- **Single-Activity, feature controllers.** `MainActivity.kt` only inflates `activity_main.xml` (auth screen, Home/Deposit/Withdraw/History tab containers, support-chat modal), builds an `AppContext`, and calls each controller's `setup()`. `AppContext` holds the activity, ViewBinding, `ApiClient`, shared helpers (`toast`, `showError`, `color`, `dp`, `logout`…) and references to every controller, so features call each other via `ctx.history.load()`, `ctx.navigation.switchTab(n)`, etc. There are no fragments, ViewModels, or Navigation component; tabs are shown/hidden by `NavigationController.switchTab()`.
- **One controller per feature:** `AuthController`, `NavigationController`, `HomeController`, `DepositController`, `WithdrawController` (+ `WithdrawMethodPicker` for the wallet/bank sheets), `HistoryController` (+ `HistoryRows` card builder), `SupportChatController` (+ `ChatBubbles`, `EmojiPicker`), `AppSheets` (notifications/profile/video-guide). `StepIndicator` drives the shared two-step header + layouts of the deposit and withdraw flows. Rows/bubbles/sheets are built programmatically rather than from item layouts. Add new screens as a new controller registered in `AppContext` and `MainActivity.onCreate`.
- **Backend: `ApiClient.kt`.** A thin `HttpURLConnection` client for the Next.js + Supabase backend in the sibling repo `../bpexch-backend` (API contract: `../docs/api.md`; workspace overview: `../CLAUDE.md`). Calls run on a single-thread executor and deliver `Result<T>` callbacks on the main thread via `enqueue`. Auth tokens/username are stored in the `session` SharedPreferences; a 401 triggers one `refreshSession()` and retry. Endpoints covered: auth login/register/refresh, user profile (polled while pending), payment methods, deposit, withdraw, history, support messages (text only). Data classes (`TransactionItem`, `SupportMessage`, `PaymentMethod`) live in the same file. Note the backend's `held` status is shown as "pending" (`HistoryRows.displayStatus`).
- **Support chat** mixes real and canned behavior: messages sync with the backend and are polled every 8s while the modal is open (`SupportChatController`), while `SupportContent.kt` (quick-reply Q&A) and `SupportReplyHelper.kt` (keyword replies) supply local bot answers. Chat is text only (no chat images or voice notes). The deposit screen has a required payment screenshot picker (`DepositController`): the image is shrunk to 1600 px and sent as base64 JPEG in `receipt`.
- **Localization:** EN/Urdu-style language toggle is handled in code (`HomeController.toggleLanguage`) in addition to `res/values/strings.xml`; many UI strings are hardcoded in Kotlin.

## Configuration gotchas

- API base URL is `@string/api_base_url` in `res/values/strings.xml`, currently `http://localhost:3000`, which reaches the PC through `adb reverse tcp:3000 tcp:3000` on a phone. An emulator needs `http://10.0.2.2:3000`; production must be HTTPS.
- `res/xml/network_security_config.xml` forbids cleartext traffic except for `10.0.2.2` and `localhost`; a production URL must be HTTPS.
- The app expects the backend to be running for login, history, deposit/withdraw, and support to work.
