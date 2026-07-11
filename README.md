# Taskora Home

Taskora Home is a native Android, fully offline home maintenance tracker built with Kotlin and Jetpack Compose. It helps you manually organize recurring household maintenance tasks — replacing filters, checking light bulbs, cleaning appliances, reviewing household equipment, scheduling minor maintenance, and keeping a clear local history — all on a simple room-based maintenance map.

> Organize recurring home maintenance tasks and keep a clear local history.

## Main features

- Manual home maintenance tracking organized by room and by "Whole Home".
- A **Home Maintenance Map**: a simplified cutaway house drawn with Compose shapes, with each room zone showing its status (Good, Soon, Overdue, or No Tasks).
- Recurring and one-time maintenance tasks with safe, calendar-based scheduling.
- Manual completion recording with a local completion history journal.
- An in-app monthly maintenance calendar with due, overdue, and completion markers.
- A household maintenance shopping checklist.
- Notes on homes, rooms, tasks, completions, and shopping items.
- In-app reminders (no push notifications, no background work).
- Multiple home profiles, each with separate rooms and history.

### Manual home maintenance tracking

Every home, room, task, date, interval, status, note, and shopping item is created or confirmed manually by you. Taskora Home never detects, monitors, diagnoses, or completes anything automatically. It is an organizer and a journal.

## Safety disclaimer

**Taskora Home is a manual household maintenance organizer. Tasks, dates, intervals, statuses, shopping items, and notes are entered by the user. The app does not provide construction, electrical, plumbing, appliance repair, engineering, inspection, or professional safety advice.**

**For hazardous, technical, structural, electrical, gas, plumbing, or appliance-related work, contact an appropriately qualified professional.**

Taskora Home does **not**:

- provide construction advice;
- provide electrical advice;
- provide plumbing advice;
- provide appliance repair guidance or procedures;
- provide structural, engineering, or professional inspection results;
- provide legal or insurance advice;
- create emergency workflows.

"Inspection Reminder" and "Safety Check Reminder" categories are neutral, user-created reminders only. The app performs no inspections and reaches no professional conclusions.

## Architecture

- **Offline-only.** No internet access, no backend, no remote APIs, no cloud sync, no Firebase.
- **Local storage.** All data lives on the device via **DataStore Preferences**, serialized as JSON strings using kotlinx.serialization.
- **Simple MVVM.** A single `TaskoraRepository` owns all data and exposes it as a `Flow<AppData>`. A single shared `TaskoraViewModel` exposes `StateFlow` and action methods. Screens are stateless with respect to persistence.
- **Pure utilities.** Focused, side-effect-free functions handle date parsing, next-due-date calculation, status, calendar markers, reminders, history grouping, and validation. These never throw into Compose.

### DataStore

Data is stored under these DataStore keys, each holding a JSON string:

- `homes_json`
- `rooms_json`
- `maintenance_tasks_json`
- `maintenance_completions_json`
- `shopping_items_json`
- `settings_json`

Deserialization is defensive: missing keys, empty strings, corrupted JSON, and newly added fields are all handled with safe fallbacks. Each collection is parsed independently, so one corrupted collection recovers to empty without discarding the others. The app never logs full stored JSON or private notes.

### Home profiles

Support for one or more homes (Apartment, House, Townhouse, Studio, Rental, Other). A home name and type are required; description is optional; no address or location is ever requested. Deleting a home (after explicit confirmation) also removes its rooms, tasks, completion history, and linked shopping items. If the active home is deleted, another home becomes active, or the empty setup state is shown.

### Rooms and maintenance zones

Rooms (Kitchen, Bathroom, Bedroom, Living Room, Hallway, Laundry, Garage, Basement, Attic, Outdoor, Utility, Custom) can be added, edited, reordered, and deleted. The first-version approach uses fixed layout templates plus a deterministic two-column grid, with simple up/down reordering — no fragile drag-and-drop floor-plan editor.

### Home maintenance map

The Home screen centers on a simplified cutaway house drawn with Compose `Canvas` and containers — never an external floor-plan image. A navy roof sits above a sand house body containing a Whole-Home strip and a grid of room zones. Each room zone shows its name, task counts, status (as text and color), and nearest due date; the most urgent zone receives a subtle outline.

**The home map is a simple organizational layout and is not an architectural plan.**

### Maintenance task system

Tasks belong to a room or to "Whole Home". Fields include title, category, schedule type, interval, start date, selected months, yearly month/day, priority (Normal/High), calculation mode, notes, and an optional shopping label. Titles are required; other fields are validated with friendly messages. Tasks can be enabled or disabled, and disabling excludes a task from active status calculations without deleting it. Priority affects sorting and emphasis only — never status.

### Recurrence types

- **OneTime** — a single due date.
- **EveryNumberOfDays / EveryNumberOfWeeks / EveryNumberOfMonths** — interval-based.
- **SelectedMonths** — recurs in chosen months.
- **Yearly** — recurs on a chosen month and day.
- **ManualOnly** — no calculated due date; complete it whenever you do it.

All calculations use `java.time.LocalDate` (with core library desugaring on older devices). No millisecond arithmetic is used for months or years, so month lengths and leap years are handled correctly.

### Good, Soon, and Overdue logic

- **Good** — next due date is after the Soon threshold.
- **Soon** — next due date is today or within the configured number of upcoming days (default 7; options 3, 7, 14, 30).
- **Overdue** — next due date is before today.
- **Unscheduled** — ManualOnly or no calculable date.
- **Disabled** — excluded from active calculations.

Room and whole-home zones aggregate their tasks: any overdue → Overdue; else any soon → Soon; else Good; no enabled tasks → No Tasks. Status is always shown as both text and color and never relies on color alone. No health/safety severity wording is used.

### Next due date calculation

The next due date is derived from the start date, the schedule, and the latest valid completion. After a completion, the next date is calculated from the completion date **by default** (per-task option to calculate from the scheduled date instead). Completing a task early or late does not silently change history. If no completion exists, calculation starts from the start date. If stored dates are invalid, the app shows "Schedule unavailable", never crashes, and allows editing.

### Completion recording

Marking a task complete saves a completion record (date, time, optional note), recalculates the next due date, and updates the room and home map immediately. Completions are worded as "Recorded as complete" / "Completion saved manually" — never implying professional verification. A completion can be undone only by deleting the record (after confirmation), which safely recalculates the schedule from the previous completion or the start date.

### Completion history

A reverse-chronological journal, groupable by date, room, task, or category, and filterable by room and category. Each row shows the completion date, task, room, category, and any note. Deleting a completion recalculates the affected task's schedule and never crashes.

### Calendar

An in-app monthly calendar (previous/next navigation) with an amber dot for due-soon, a red marker for overdue relative to today, and a green check for completions. Markers are accessible (text labels), and selecting a date lists tasks due, tasks completed, rooms involved, and note counts. **No device or Google Calendar integration and no calendar permission are used.**

### In-app reminders

Reminders appear only inside the app, evaluated when the app opens, the active home changes, a task is created or edited, a completion is saved, or the date changes while the app is active. They cover overdue tasks, tasks due today, tasks due within the Soon threshold, and unchecked high-priority shopping items, with "View Tasks" and "Not Now" actions.

**Taskora Home reminders appear inside the app. The app does not send push notifications or run in the background.**

### Shopping list

A local household maintenance shopping checklist with categories, optional quantity text, priority, optional room and linked task, and notes. Items can be added, edited, checked, unchecked, deleted, filtered (room/category/priority), and cleared (checked items, after confirmation). There are no store links, prices, affiliate links, product recommendations, or part-compatibility claims.

### Notes

Plain-text notes are supported on homes, rooms, tasks, completions, and shopping items (short notes up to 300 characters; detailed notes up to 1000). Notes are never interpreted, classified, or used to generate advice.

### Templates

Optional local task templates (Kitchen, Bathroom, Laundry, Seasonal routines) provide editable starting points.

**Templates are editable organizational examples, not professional maintenance or repair advice.** They contain no repair steps and no electrical or plumbing instructions; you review and customize everything.

## Privacy

**Taskora Home stores home profiles, rooms, maintenance tasks, due dates, completion records, notes, shopping items, and settings locally on this device. The app has no account, no cloud sync, no internet access, no ads, no analytics, no payments, no camera access, no location tracking, no smart home connection, and no background monitoring.**

Specifically, Taskora Home has:

- **no camera** and requests no camera permission;
- **no internet** access and declares no INTERNET permission;
- **no smart home integration**, Bluetooth, Wi-Fi device discovery, or NFC;
- **no account** and no authentication;
- **no backend** and **no cloud sync**;
- **no Firebase**, ads, analytics, or payments;
- **no location tracking**;
- **no external API** usage;
- **no runtime permissions** of any kind.

## Visual concept

The design is a calm, organized "Cutaway House Service Map" — a household maintenance notebook rather than a contractor tool, smart-home panel, marketplace, game, or generic task manager.

- **Layout uniqueness.** The Home screen deliberately avoids the "mascot → title → subtitle → stats card → button stack" pattern. It uses a compact home selector and date strip, a large house map, a narrow "Next Maintenance" shelf, a horizontal status legend, an upcoming-task rail, a recent-completion strip, a shopping preview, bottom navigation (Home, Tasks, Calendar, Shopping, Settings), and a floating Add Task button that does not cover the map.
- **House map drawing approach.** The house is drawn with Compose `Canvas` (roof triangle) and structured containers (room cells), with room accent colors and status borders. No external floor-plan images are used.

### App icon concept

A custom adaptive icon: navy background, a simplified cutaway house outline with three room zones, and one small amber maintenance dot. No wrench/electrical symbols, no helmet, no contractor or appliance branding, and no text. A vector fallback is provided for devices below API 26.

### Splash screen concept

A stable static splash: warm sand background, a centered navy cutaway house with one subtle amber status dot, and the app name shown as the launcher label. No photographs, tools, or heavy animation.

## Technology stack

Kotlin, Jetpack Compose, Material 3, Navigation Compose, Android ViewModel, Kotlin Coroutines, Kotlin Flow, DataStore Preferences, kotlinx.serialization, and Gradle Kotlin DSL. No networking, image-loading, chart, database, DI, smart-home, or Bluetooth libraries are used.

## Building the project

### Open in Android Studio

1. Use a recent stable Android Studio.
2. Choose **Open** and select this project folder.
3. Let Gradle sync. **JDK 17** is required.

> **Gradle wrapper JAR:** `gradle/wrapper/gradle-wrapper.jar` is a binary and may not be present in this distribution. Android Studio regenerates it automatically on first sync. To create it manually from a local Gradle install, run `gradle wrapper --gradle-version 8.9` in the project root. The CI workflow regenerates it automatically if missing.

### Android configuration

- `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`.
- Android Gradle Plugin 8.5.2, Kotlin 1.9.24, Compose Compiler 1.5.14.
- Portrait orientation is locked; edge-to-edge is enabled with system bars kept visible.
- **16 KB page-size compatibility:** the app bundles no native third-party binaries (pure Kotlin/Compose/DataStore), so 16 KB memory-page compatibility is inherent. Still verify the final bundle.

### Debug build

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

### Release build — staged R8 enablement

The project ships with a two-stage release process for stability.

1. **Stage 1 (non-minified):** temporarily set both flags in `app/build.gradle.kts` to `false`:

   ```kotlin
   isMinifyEnabled = false
   isShrinkResources = false
   ```

   Build, install, launch, and test the non-minified release against the functional checklist below.

2. **Stage 2 (minified):** restore the shipped configuration:

   ```kotlin
   isMinifyEnabled = true
   isShrinkResources = true
   proguardFiles(
       getDefaultProguardFile("proguard-android-optimize.txt"),
       "proguard-rules.pro"
   )
   ```

   Rebuild, reinstall, and re-test kotlinx.serialization, DataStore, navigation, and recurrence calculations. The provided `proguard-rules.pro` keeps the serializers required by DataStore JSON.

## Signing

Release APK and AAB are signed with a real **PKCS12** keystore. The build **never** falls back to the Android debug key for a release; if credentials are missing when assembling a release, the build fails clearly.

### Generate a keystore

```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore taskora-home-release-key.p12 \
  -alias taskora_home_key \
  -keyalg RSA -keysize 2048 -validity 10000
```

### Local signing setup

Signing values are read from environment variables or a git-ignored `keystore.properties` file at the project root:

```properties
storeFile=/absolute/path/to/taskora-home-release-key.p12
storePassword=your_store_password
keyAlias=taskora_home_key
keyPassword=your_key_password
```

Never commit the `.p12` file, passwords, a decoded keystore, or signing properties. These patterns are already in `.gitignore`.

### Required GitHub Secrets

- `ANDROID_KEYSTORE_BASE64` — base64 of the `.p12` file.
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Use the same password for the keystore and key unless you have configured separate values reliably.

### GitHub Actions

`.github/workflows/android-build.yml` runs on push to `main` and via `workflow_dispatch`. It checks out the repo, sets up JDK 17, installs Android SDK Platform 35 and Build Tools 35.0.0, restores the Gradle cache, decodes `ANDROID_KEYSTORE_BASE64` to a temporary PKCS12 file, exposes signing secrets only as environment variables, builds the signed release APK and AAB, locates the APK, runs `apksigner verify --print-certs`, fails if verification fails **or** if the certificate contains `CN=Android Debug`, and uploads the APK (test artifact) and AAB (Google Play artifact). Passwords and base64 values are never printed.

> CI is responsible for compilation, signing, certificate verification, and artifact generation. **CI is not proof that the app launches.**

## Build outputs

- **Signed release APK** — for local installation and verification.
- **Signed release AAB** — for Google Play. **Only the `.aab` is uploaded to Google Play.**

## Local release verification

```bash
# Build
./gradlew :app:assembleRelease :app:bundleRelease

# Verify certificate (must NOT contain CN=Android Debug)
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

# Install and launch on a device/emulator
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat
```

Repeat the verification after enabling R8 (Stage 2).

## Local functional test checklist

Test: first launch with empty storage; onboarding; skip onboarding (Explore First); create home; create multiple homes; switch active home; edit home; delete inactive home; delete active home; create room; create several rooms; edit room; reorder room; delete room; create Whole Home task; create room task; create one-time task; create daily/weekly/monthly interval tasks; create selected-month task; create yearly task; create ManualOnly task; edit recurrence; disable task; enable task; delete task; mark task complete; verify next due date; complete task early; complete task late; switch calculation mode; add completion note; edit completion note; delete completion; verify schedule recalculation; verify Good/Soon/Overdue status; verify room status; verify Whole Home status; open room with no tasks; open task-list filters; open overdue list; open calendar; navigate months; open empty date; open date with due tasks; open completion history; filter history; add shopping item; link shopping item to task; check shopping item; clear checked items; trigger in-app overdue reminder; dismiss reminder; disable reminders; reset active home history; reset all local data; relaunch app; launch in airplane mode; confirm all functionality remains available; confirm no INTERNET permission; confirm no runtime permission dialogs; confirm no camera control; confirm no device calendar integration; confirm no smart home connection.

Inspect `adb logcat` for: `ClassNotFoundException`, `NoSuchMethodError`, serialization crashes, DataStore parse crashes, navigation argument crashes, `LocalDate` calculation crashes, invalid recurrence crashes, missing home/room/task crashes, `Canvas` layout crashes, R8-related crashes, and signing misconfiguration. Also verify the release certificate, AAB generation, API 35, and 16 KB page-size compatibility.

## Data reset behavior

Settings offers: clear checked shopping items; delete history for the active home; delete the active home; and **Reset all local data**, which permanently removes every home, room, task, completion record, note, shopping item, and setting stored by Taskora Home on this device. All destructive actions require explicit confirmation.

## Manual-entry limitations

Taskora Home is a manual organizer. It does not detect completed work, monitor the home, or verify anything. Accuracy of statuses, due dates, and history depends entirely on the information you enter. For any hazardous, technical, structural, electrical, gas, plumbing, or appliance-related work, contact an appropriately qualified professional.
