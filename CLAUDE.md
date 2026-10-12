# CLAUDE.md

Guidance for working in this repository.

## What this is

**AndroidSensors** is a single-module Android app (`:app`, package `com.akshayAshokCode.androidsensors`) that turns the phone's built-in sensors into interactive tools: four sensor tools plus **Wi-Fi Pulse**, an internet-is-back alert. It runs offline and has no ads. The sensor tools need only `VIBRATE`. Wi-Fi Pulse adds network-state permissions plus `POST_NOTIFICATIONS`, which is requested only when the user arms the alert. The AD_ID permission is explicitly removed in the manifest. Published on Google Play.

- Kotlin, minSdk 24 (Play automatic protection requires it), target/compileSdk 36, Java 11
- UI: Jetpack Compose hosted inside Fragments (`ComposeView`), with an XML activity shell (DrawerLayout + MaterialToolbar + NavHostFragment, ViewBinding)
- Navigation: Jetpack Navigation + Safe Args ([nav_graph.xml](app/src/main/res/navigation/nav_graph.xml))
- Firebase Analytics, Play In-App Update (flexible), Play In-App Review
- Version lives in [app/build.gradle](app/build.gradle) (`versionCode`/`versionName`). The README badge can lag behind it.

## Build & run

```bash
./gradlew assembleDebug          # debug APK
./gradlew installDebug           # install on connected device
./gradlew bundleRelease          # release AAB (R8 minify on, full native debug symbols)
./gradlew test                   # unit tests (only the template ExampleUnitTest exists)
```

`google-services.json` is required for the Firebase plugin. Sensors don't work meaningfully on the emulator, so test on a real device.

## Architecture

```
presentation/
  onboarding/OnboardingActivity   ← LAUNCHER. First launch shows a 2-page pager; later launches go straight to MainActivity
  MainActivity                    ← drawer + toolbar + NavHost, forces dark mode, in-app update/review
  fragments/                      ← one Fragment per screen; owns SensorManager + state
  views/                          ← stateless Compose UI per feature (*Components.kt)
data/SensorRepository             ← static list of the 5 SensorModels (name, tagline, icon) shown on the dashboard
network/                          ← Wi-Fi Pulse: status monitor, alert foreground service, notifications, QS tile
utils/SensorUtils                 ← orientation enum, smoothing, bubble/grid math
utils/AnalyticsManager            ← all Firebase event names + log helpers
utils/PreferencesManager          ← SharedPreferences: first launch, first feature, review trigger
Constants                         ← feature IDs (0–3)
```

The README says MVVM, but the code doesn't use ViewModels. Each feature **Fragment is the controller**:
- It implements `SensorEventListener`, registers in `onResume`, and unregisters in `onPause` and `onDestroyView`.
- It holds UI state as `mutableStateOf` fields and passes them to a stateless composable.
- **Threading rule (follow it):** do the math in `onSensorChanged` using only plain/local variables, then `mainHandler.post { if (!isAdded) return@post; ... }` to mutate Compose state. Earlier crashes, including a `ConcurrentModificationException` on SnapshotStateLists, came from touching Compose state on the sensor thread. `DecimalFormat` is also formatted on the main thread because it isn't thread-safe.
- Each tool adds a toolbar ⓘ via `MenuProvider` (`sensor_info_menu`). The ⓘ and the in-screen "TAP ↑ FOR SENSOR DETAILS" text both open the shared `SensorDetailsBottomSheet`, which shows title, instruction and details from string resources.
- When the required sensor is missing, set `isAvailable = false`, call `AnalyticsManager.logSensorUnavailable(...)`, and the screen shows a centered "not available" message.

**Edge-to-edge:** Android 15+ draws apps behind the system bars. Both activities call `enableEdgeToEdge()` with dark bars.
- In `MainActivity`, the toolbar handles the status bar (`fitsSystemWindows`), and a window-insets listener pads the nav host by the navigation-bar and cutout insets. Fragment screens therefore never need their own bottom inset handling.
- The dashboard pads itself for the status bar, because its toolbar is hidden.
- Don't set `android:statusBarColor` / `navigationBarColor` in the theme. They are deprecated and ignored on Android 15+, and Play Console flags them.

`GravityMeterScreen` and `BubbleLevelToolScreen` are declared at the bottom of their fragment files. The other screens are in `views/`.

## Features: sensors and UX

| Feature | Sensors actually used | Sampling |
|---|---|---|
| Metal Detector | `TYPE_MAGNETIC_FIELD` | `SENSOR_DELAY_NORMAL` |
| Gravity Meter | `TYPE_GRAVITY`, falling back to `TYPE_ACCELEROMETER` + low-pass (α=0.8) | `NORMAL` |
| Bubble Level | `TYPE_ACCELEROMETER` **+** `TYPE_MAGNETIC_FIELD` (rotation matrix → orientation) | `GAME` |
| Space Ball | `TYPE_GRAVITY`, falling back to `TYPE_ACCELEROMETER` + low-pass (x/y only) | `GAME` |
| Wi-Fi Pulse | No `SensorManager` sensor (`sensorType = Constants.NO_SENSOR_TYPE`). Uses `ConnectivityManager` network callbacks on Wi-Fi | event-driven |

### Dashboard (home)
- A 2-column grid of 4 sensor cards over a "deep-space" dot grid. Card borders pulse on staggered timers, and a boot scanline sweeps over the screen. A header has a hamburger button that opens the drawer.
- Availability is checked once in `onCreateView`. Gravity and Space Ball count as available if either the gravity sensor or the accelerometer exists. Other cards check the `sensorType` from `SensorRepository`.
- On the first view only, the cards animate in, with one `CLOCK_TICK` haptic per card (380 ms + i·100 ms).
- The toolbar is hidden on the dashboard and shown on tool screens. It reappears on the destination's `ON_RESUME`, not after a fixed delay. The drawer is locked closed outside the dashboard.
- The drawer has two items: **Write a review**, which triggers the Play review flow, and **Send feedback**, which opens a `mailto:` with device/OS/app-version prefilled.

### Metal Detector ([MetalDetector.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/fragments/MetalDetector.kt), [MetalDetectorComponents.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/views/MetalDetectorComponents.kt))
- Computes magnitude = √(x²+y²+z²) in µT.
- **Auto-calibration:** the first 20 samples are averaged into a baseline, and a calibration progress bar is shown during that time. Calibration runs on every `onResume`. **Recalibrate** unregisters the listener, waits 1 s, then collects samples again.
- **Deviation mode** (default) shows |magnitude − baseline|. **Raw mode** (the "⇄ RAW" toggle) shows absolute magnitude.
- Signal strength = deviation / 300 µT, clamped to 0–1. It drives a sonar radar (rings, sweep, blips) and a signal bar whose colour goes cyan → amber → orange → red. The status label moves through SCANNING / WEAK SIGNAL / ANOMALY DETECTED / STRONG SIGNATURE and flickers above 0.55.
- No audio or haptic feedback is implemented, even though the README mentions "audio alerts". The feedback is visual only.

### Gravity Meter ([GravityMeter.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/fragments/GravityMeter.kt), [GravityMeterComponents.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/views/GravityMeterComponents.kt))
- Shows the live gx/gy/gz vector as a `GravitySphere`, the total magnitude in m/s², per-axis `AxisMeters` (X pink, Y amber, Z blue), and an `OscilloscopeGraph` of the last 50 samples.
- `OrientationBadge` shows the orientation: LANDSCAPE / PORTRAIT / UPSIDE_DOWN / FACE_UP / FACE_DOWN. It changes when an axis exceeds ±7 m/s², and otherwise keeps the previous value.

### Bubble Level ([BubbleLevelTool.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/fragments/BubbleLevelTool.kt), [BubbleLevelComponents.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/views/BubbleLevelComponents.kt))
- Pitch and roll come from `SensorManager.getRotationMatrix` + `getOrientation` and are smoothed with an EMA (factor 0.1). Calibration-offset fields exist, but no UI sets them.
- Sensitivity modes: **PRECISION ±0.5°**, **STANDARD ±2°** (default), **ROUGH ±5°**, chosen with a 3-way toggle.
- A `TargetReticle` crosshair has a diamond indicator that tracks the tilt. The status reads "✓ LEVEL LOCKED" (green, pulsing) or "◈ ADJUSTING..." (amber), and HUD cards show pitch and roll.
- A light vibration (25 ms, amplitude 50) fires on the transition into level.
- Because it needs the magnetometer, it is unavailable on devices without one. The dashboard card only checks the accelerometer, so it can show as available and then report "not available" once opened.

### Space Ball ([SpaceBall.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/fragments/SpaceBall.kt), [SpaceBallComponents.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/views/SpaceBallComponents.kt))
- A 2D physics toy with a glowing "energy orb" on a starfield. Tilting rolls the ball (sensitivity 0.15, friction 0.98, bounce damping 0.7, normalized 0–1 coordinates). Dragging the ball and releasing throws it, using 15% of the drag velocity.
- Wall impacts produce 3–8 short-lived yellow sparks (capped at 50) and a haptic: `CONTEXT_CLICK` for hard hits, `CLOCK_TICK` for soft ones. A 20-point comet trail is sampled every 50 ms. 80 background stars drift with gravity at half sensitivity, wrapping at the edges, for a parallax effect.
- The screen orientation is locked while the ball is visible and restored in `onPause`.
- The "DRAG TO LAUNCH / TILT TO ROLL" hint disappears after the first drag.

### Wi-Fi Pulse ([WifiPulse.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/fragments/WifiPulse.kt), [WifiPulseComponents.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/views/WifiPulseComponents.kt), [network/](app/src/main/java/com/akshayAshokCode/androidsensors/network/))
- **Purpose:** the user taps "Notify me when it is back" once and stops checking their phone. It is a one-shot alarm, not continuous monitoring. Test every change against "the user never has to look at the phone until the alert fires".
- **Which connection is watched** is decided automatically, with no setting (`InternetStatusMonitor.targetOf`):
  - **Wi-Fi switched on:** it watches Wi-Fi, even while not connected, for example when the router has no power.
  - **Wi-Fi switched off:** it watches mobile data.
  - The rule is re-applied on every Wi-Fi or airplane-mode change, including during a wait.
  - Don't change it to "on **and connected**". In a router power cut the phone falls back to mobile data, so that version would say "online" and the user couldn't arm the alert.
- `InternetStatusMonitor` emits `InternetState(target, status, blocker)`:
  - **status:** ONLINE (`NET_CAPABILITY_VALIDATED`), NO_INTERNET (connected but not validated), DISCONNECTED.
  - **blocker:** `WIFI_OFF` (only on devices without mobile data) or `AIRPLANE_MODE` (when watching mobile data).
  - While NO_INTERNET, it calls `reportNetworkConnectivity(network, true)` every 30 s to force Android to re-check. Without that, Android's re-check backoff can delay the alert by minutes. Doze still pauses this timer.
  - `snapshot()` gives a synchronous answer, used by the tile and for the first frame.
- `InternetAlertService` is a foreground service of type `specialUse`. It runs only while armed.
  - It alerts **immediately** when the status becomes ONLINE, with no stability delay, because Doze would stretch a delay.
  - It then disarms and stops. It gives up after 12 h with a silent timeout notification.
  - `armedAt` is persisted in `PreferencesManager`, so a `START_STICKY` restart resumes the same wait.
  - `InternetAlertService.armedSince` (a StateFlow) is the source of truth for the UI and the tile.
  - **When the phone kills the wait:**
    - If Android refuses to put a restarted service back in the foreground (possible on Android 12+), the service gives up cleanly and posts "open Wi-Fi Pulse to turn it on again".
    - If a battery saver killed it with no restart, opening the Wi-Fi Pulse screen notices (stored `armedAt` set but no service running). It resumes the wait, or says the alert was missed if the internet is already back, and links to App info.
    - Every case is logged as `internet_alert_stopped_by_system` with `outcome` = `restart_blocked` / `resumed` / `missed`. Watch this event to see how often phone makers kill the service in the field.
- The **waiting notification** is low-importance, ongoing and visible on the lock screen. It shows a factual status line and rotates a "take a break" line every 5 min, switching to the long-outage lines after 1 h. It has a **Stop waiting** action.
- **Alert style** is chosen next to the arm button and stored in `PreferencesManager.isAlertRing`. The default is **Ring**. The tile uses the last choice.
  - **Ring:**
    - The service stays in the foreground and plays the alarm sound (`USAGE_ALARM`, so it plays on silent), looping and getting louder, with vibration.
    - The silent `wifi_pulse_ringing` channel shows a heads-up with **Stop**. Swiping it away also stops the ring.
    - It stops on Stop, when the app is opened (`MainActivity.onResume` calls `stopRinging`), or after 60 s. A quiet "internet is back" notification is left behind.
    - Under Do Not Disturb it doesn't ring and falls back to Notification. It uses `CATEGORY_STATUS`, not `CATEGORY_ALARM`, so it doesn't bypass DND.
    - There is no full-screen intent, because Android 14+ limits that to alarm and calling apps.
  - **Notification:** a single post on the high-importance `wifi_pulse_alert` channel.
  - Both styles show "Back after 42 minutes" (`InternetAlertNotifications.backAfter`, plural resources). It's counted from when the alert was armed, not from the start of the outage.
  - A delivered alert counts as a review-prompt win.
- **Quick Settings tile** ("Internet alert"):
  - It arms or cancels the alert.
  - If arming isn't possible (already online, Wi-Fi off, notifications blocked, or Android 12+ refusing to start the foreground service), it opens the Wi-Fi Pulse screen through a `NavDeepLinkBuilder` link instead.
- **Signal strength** (`SignalReading.read(context, target)`):
  - It is polled once a second, only while the screen is visible.
  - It shows as `-54 dBm · EXCELLENT · 5 GHz` (Wi-Fi) or `-95 dBm · GOOD · Carrier` (mobile data) under the status label.
  - In the orb, the glyph's bars light up by strength: Wi-Fi arcs, or rising mobile bars.
  - The label and bar count come from the system's status-bar level: `WifiManager.calculateSignalLevel` on API 30+, `SignalStrength.level` for mobile.
  - No new permission. The Wi-Fi name, network type (4G/5G) and cell info are never read, because they would need the location or Phone permission.
- **Screen:** a status label, a pulse orb (one slow ring when idle, a fast triple ping when armed), a context message, and one main button. The button changes between Notify me / Stop waiting / You are online / Turn on Wi-Fi / Allow notifications.
- **Copy rules:** users are in 150+ countries.
  - Keep the copy short and literal, with no idioms, puns, sarcasm or cultural references, and only universal emoji.
  - "Wi-Fi Pulse" is never translated. The translator note lives in `values/strings.xml`.
- **Not built yet:** Signal Hunter, room survey, "notify when it drops", outage log. The speed test is deliberately out of scope.

### Onboarding
- `HorizontalPager` with 2 pages: Welcome (radar animation), then Features (the 4 tools with use cases and sensor tags). It has Skip, Back and Next/Get Started controls.
- The `is_first_launch` pref is cleared when the user finishes or skips.

## Review prompt ("wins")

Each tool calls `PreferencesManager.recordWin()` at most once per visit, when the user hits a success moment:
- Metal Detector: signal strength above 0.6
- Gravity Meter: the first orientation detection
- Bubble Level: becoming level
- Space Ball: a hard wall impact

After **4 wins** a `pending_review` flag is set. The next time the Dashboard resumes, it consumes the flag and launches the Play review flow after 1.2 s. If you add a feature, wire up its win moment the same way and log it with `AnalyticsManager.logWinAchieved`.

## Design system

The app uses one dark "sci-fi HUD" look, forced to night mode. The tokens are in [DashboardComponents.kt](app/src/main/java/com/akshayAshokCode/androidsensors/presentation/views/DashboardComponents.kt):
- `DashVoid #050510` (bg), `DashSurface #0D0D2B`, `DashCyan #00D4FF` (primary), `DashPurple #7B2FFF`, `DashGreen #00FF88` (ok), `DashRed #FF3355`, `DashAmber #FFB800`, `DashOrange #FF6B00`, `DashGrid #1A1A3E`
- Feature files alias these as private vals (`MD*`, `GM*`, `L*`, `BS*`). Onboarding redefines the same hex values locally instead of importing them.
- Typography is monospace, uppercase and letter-spaced. Screens use a dot-grid background (28 dp spacing).

## Adding a new sensor tool

1. Add an ID to `Constants` and a `SensorModel` to `SensorRepository`. Add the name string, a `tagline_*` string and an icon drawable.
2. Create a Fragment that follows the existing pattern: listener lifecycle, `mainHandler.post` for state, MenuProvider ⓘ, `SensorDetailsBottomSheet`, the unavailable state, and a win hook.
3. Put the composables in `views/<Feature>Components.kt` using the `Dash*` tokens.
4. Add the fragment and a `dashboard → feature` action to `nav_graph.xml`. Then add a branch in `Dashboard.clickedSensor`, and in the availability check if there is a fallback.
5. Add `AnalyticsManager.Features.<NAME>` and an onboarding `FeaturesScreen` entry. Update the "N TOOLS" count there and add a `baN` border pulse in `DashboardScreen`.
6. Add strings to `values/strings.xml` **and** each locale: `values-es`, `values-hi`, `values-pt-rBR`, `values-zh-rCN`.

## Gotchas

- Many Compose labels in the older tools are hardcoded English (HUD labels, status text, Metal Detector and Space Ball "not available" text, onboarding feature rows), so they aren't localized even though 5 locales exist. The dashboard taglines (`SensorModel.taglineResId`) and all Wi-Fi Pulse text are in string resources; keep new UI text there.
- `InAppReview` fragment and `in_app_review_layout.xml` are unused. Review now goes through `MainActivity.triggerInAppReview()`.
- The theme is still named `Theme.MetalDetector`, and test sources live under the old `com.akshayAshokCode.metaldetector` package. Both are leftovers from the app's original name.
- The `SensorModel.sensorType` for Bubble Level is `TYPE_ACCELEROMETER`, but the tool also needs the magnetometer (see above).
