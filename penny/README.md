# Penny — a private savings ledger for Android

<img src="branding/penny-wordmark.png" alt="Penny" width="420">

Penny is an offline personal savings tracker in Indian Rupees. You record what you put aside (and what you take out), assign savings to goals, and see clear, accurate figures. There is no account, no server and no internet permission. Everything stays on your phone.

| | |
|---|---|
| Platform | Android 8.0 (API 26) and newer, targets Android 15 (API 35) |
| Stack | Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Navigation Compose · MVVM |
| Currency | ₹ INR, Indian digit grouping (₹1,00,000.00), amounts stored as integer paise |

## Features

- **Home**: greeting with today's date, total savings, this month's net savings, entry count, monthly-target progress, goals summary, recent activity, plus *Add savings* and *Withdraw*.
- **Add / edit / delete entries**: Saving or Withdrawal, amount (numeric keypad, validated), date (no future dates), category (General, Emergency, Travel, Shopping, Other), optional goal, optional note. Save is debounced so repeated taps can't create duplicates. Deleting shows an *Undo* snackbar.
- **History**: every entry, newest first, grouped by day. Search covers notes, categories, goals and amounts. Filter by Day, Week, Month or Year, and step back and forward through periods.
- **Goals**: title, target, optional target date, progress %, remaining amount, and "save ₹X/month to finish on time". You can add to or withdraw from a specific goal, and edit or delete it. Deleting a goal keeps its money, which becomes unassigned.
- **Insights**: total, this month, this week, today and daily average. Also a 6- or 12-month trend chart (tap the bars), this month vs last month, your best savings month, and savings by category. All of it is computed from your stored entries.
- **Settings**: your name, monthly target, and light, dark or system theme. A daily reminder with a time picker, an option to skip it if you've already saved today, a custom message, and a test notification. CSV export, JSON backup and restore, a Privacy page, and *Clear all data* (you must type DELETE to confirm).
- **Branding**: original coin-and-seedling logo, an adaptive launcher icon with an Android 13+ themed (monochrome) layer, and a branded splash screen.

## How the numbers work

All money is stored as whole **paise** in a `Long`, so there is no floating-point rounding.

- **Total savings** = Σ deposits − Σ withdrawals
- **Monthly savings** = deposits − withdrawals dated in that calendar month
- **Weekly** = Monday to Sunday (ISO weeks). **Daily average** = this month's net ÷ days elapsed
- **Goal progress** = (deposits − withdrawals assigned to the goal) ÷ target × 100

**No double counting.** Each entry belongs to at most one goal, so *money in goals + unassigned money = total savings*. A withdrawal comes out of a specific "bucket" (a goal, or unassigned). Penny refuses any add, edit, delete or restore that would leave a bucket below zero. These checks run inside a database transaction.

Sample figures appear only on the onboarding screen, clearly labelled **SAMPLE DATA**. They are never written to your ledger.

## Download the APK (no build needed)

Every push that changes `penny/` runs the GitHub Actions workflow `.github/workflows/penny-android.yml`. It runs the unit tests and Android lint, builds both APKs, and publishes them:

1. On GitHub, open the repository → **Releases** → **Penny build N** (pre-release).
2. Download `penny-N-release.apk` on your phone.
3. Open it and allow *Install unknown apps* for your browser or file manager when Android asks.

> If the workflow doesn't run on a forked repository, open the **Actions** tab and click *"I understand my workflows, go ahead and enable them"*. Then run **Penny Android build** → *Run workflow*.

## Build it yourself

Requirements: **Android Studio Ladybug (2024.2) or newer**, or the command line with **JDK 17+** and the Android SDK (Platform 35).

```bash
cd penny
./gradlew testDebugUnitTest   # 58 unit tests for money, dates, goals, validation, CSV, backup
./gradlew assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # → app/build/outputs/apk/release/app-release.apk
```

In Android Studio: **File → Open → `penny/`**, wait for Gradle sync, then **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or press **Run** with a phone connected.

### Install on your phone

- **USB:** enable *Developer options → USB debugging*, then run `adb install -r app/build/outputs/apk/release/app-release.apk`.
- **Sideload:** copy the APK to the phone and open it.

`-r` reinstalls over an existing copy and **keeps your data**. That works because builds share the same signing key (see below).

### Signing

- **Debug key:** `keystore/debug.keystore` is a deliberately **non-secret** key (passwords `android`/`android`). It is committed so that every build, local or CI, has the same signature and updates install cleanly. Release builds fall back to this key when no release key is configured.
- **Your own release key:** recommended if you'll keep using the app long-term.
  ```bash
  keytool -genkeypair -v -keystore keystore/release.jks -alias penny -keyalg RSA -keysize 2048 -validity 10000
  ```
  Then create `penny/keystore.properties` (git-ignored):
  ```properties
  storeFile=keystore/release.jks
  storePassword=…
  keyAlias=penny
  keyPassword=…
  ```
  For CI, add the repository secrets `PENNY_KEYSTORE_BASE64` (`base64 -w0 keystore/release.jks`), `PENNY_KEYSTORE_PASSWORD`, `PENNY_KEY_ALIAS` and `PENNY_KEY_PASSWORD`.

  ⚠️ Android won't install an update signed with a different key over an existing install. If you switch keys, **back up first** (Settings → Back up data), uninstall, install the new build, and restore.

## Project structure

```
penny/
├─ app/src/main/java/app/penny/
│  ├─ core/            Money (INR parsing/formatting), Periods (date ranges), DeviceClock, ReminderTime
│  ├─ domain/          Models, SavingsCalculator (every figure), EntryValidator (bucket rules)
│  ├─ data/            Room (entities, DAOs, database), SavingsRepository, PreferencesRepository (DataStore)
│  │  └─ io/           CsvExporter, BackupCodec (JSON), DocumentIo (Storage Access Framework)
│  ├─ notifications/   ReminderScheduler (AlarmManager), reminder + reboot receivers
│  └─ ui/              theme, icons (custom line set), common components, and one package per screen
│                      (home, entry, history, goals, insights, settings, onboarding) with its ViewModel
├─ app/src/test/       JVM unit tests
├─ app/schemas/        Room schema JSON (generated on build, used for future migrations)
├─ branding/           Logo SVGs and PNG exports
└─ keystore/           Non-secret debug key
```

## Privacy

Penny requests only two permissions: **notifications** (for the reminder) and **run at startup** (to re-schedule the reminder after a reboot). It has no INTERNET permission, no analytics and no third-party SDKs. Android cloud backup and device transfer are disabled for its data. Use *Settings → Back up data* to keep your own copy.

## Fonts & licences

UI type is **Inter**, © The Inter Project Authors, under the SIL Open Font License 1.1 (`FONT_LICENSE_OFL.txt`).
