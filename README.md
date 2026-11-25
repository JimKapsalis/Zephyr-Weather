<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.png" width="118" height="118" alt="Zephyr Weather Icon" />

# 🌤️ Zephyr Weather for Android
### **4-Model Super-Ensemble Forecasting • HyperOS Zero-Wakelock Battery Engine • Live Doppler Radar • Bilingual (EN / ΕΛ)**

[![Latest Release](https://img.shields.io/badge/GitHub_Release-v2.7.0-00E5FF?style=for-the-badge&logo=github&logoColor=white)](../../releases/latest)
[![Direct APK Download](https://img.shields.io/badge/Download_APK-ZephyrWeather.apk-10B981?style=for-the-badge&logo=android&logoColor=white)](../../releases/latest/download/ZephyrWeather.apk)
[![HyperOS Battery Guard](https://img.shields.io/badge/Battery_Keep--Awake-0_Minutes-22C55E?style=for-the-badge&logo=xiaomi&logoColor=white)](#-hyperos--miui-zero-wakelock-battery-engine)
[![4-Model Ensemble](https://img.shields.io/badge/NWP_Ensemble-ECMWF_%2B_ICON_%2B_GFS_%2B_HR--LAM-F59E0B?style=for-the-badge)](#-4-model-super-ensemble-meteorological-engine)

---

### 📲 **[⬇️ Download Latest `ZephyrWeather.apk` from GitHub Releases](../../releases/latest/download/ZephyrWeather.apk)**
*(Or download [`ZephyrWeather.apk`](./ZephyrWeather.apk) directly from the repository root)*

</div>

---

## ✨ Highlights & Key Features

| Feature | Description |
| :--- | :--- |
| 🛰️ **4-Model Super-Ensemble Engine** | Fuses **HR-LAM Best-Match (1–2 km)**, **ECMWF IFS 0.25° (9 km)**, **DWD ICON Seamless (2–7 km)**, and **NOAA GFS Seamless (13–25 km)** in parallel with outlier dampening, altitude lapse-rate calibration (`-6.5°C / 1000m`), and wet-bulb snow physics. |
| 🔋 **HyperOS / MIUI Zero-Wakelock Battery Engine** | Specifically engineered for **Redmi Note 15 Pro 5G** and 120Hz AMOLED phones: **0 minutes background keep-awake**, `~70 MB RAM`, zero `ELAPSED_REALTIME_WAKEUP` alarms, and automatic lifecycle pausing of all 120Hz canvas & clock animations when the screen turns off. |
| 🔄 **Built-In GitHub Releases App Updater** | Checks the **GitHub Releases API** (`/repos/{owner}/{repo}/releases/latest`) directly from **Settings → GitHub Releases App Updater**, compares semantic version tags, displays release notes, and lets you download new APK releases in one tap. |
| 🏔️ **Precision Mountain, Village & Snow Telemetry** | Covers all cities, towns, islands, and mountain villages across Greece and worldwide with live elevation meters, freezing level (`0°C` isotherm), snow depth, and snow accumulation alerts. |
| 🧭 **Photorealistic Sky & 3D Glass Widgets** | Photorealistic multi-layered atmospheric sky canvas (`WeatherSkyBackdrop`), 3D navigation bar icons, **Helios 24h Chrono-Dial**, **Smart Comfort & Outfit Advisor**, **Analog Barometer**, and a 4×2 Home Screen AppWidget. |
| 🇬🇷 **Bilingual English & Greek (`EN` / `ΕΛ`)** | Instant one-tap switching between English and Greek with localized Beaufort wind scales, EMY severe weather bulletins, and metric/imperial units (`°C / °F`, `km/h / mph`, `hPa / inHg`). |

---

## 🔋 HyperOS / MIUI Zero-Wakelock Battery Engine

Tested and optimized for **Xiaomi Redmi Note 15 Pro 5G** (HyperOS / 120Hz display):
- **Zero Wakeup Alarms (`0m Keep-Awake`)**: Uses Android's Doze-batched `JobScheduler` (`setRequiresBatteryNotLow(true)`) without `AlarmManager.ELAPSED_REALTIME_WAKEUP` or recursive job rescheduling, allowing the CPU to stay in deep sleep when the screen is off.
- **Zero Per-Frame List Recompositions**: Forecast list icons render in the Compose draw/layer phase without spawning per-item `InfiniteTransition` tickers.
- **Instant `ON_STOP` Teardown**: All foreground refresh timers, search jobs, minute-aligned clocks, and 120Hz particle canvases pause the instant the app leaves `Lifecycle.State.RESUMED`.
- **Bounded GNSS Hardware Access**: `FusedLocationProviderClient` tokens are automatically cancelled upon completion and default to balanced power accuracy when Battery Saver is enabled.

---

## 🛰️ 4-Model Super-Ensemble Meteorological Engine

When **Multi-Model Ensemble (`BEST_MATCH`)** is selected, Zephyr Weather queries 4 independent Numerical Weather Prediction (NWP) models simultaneously and blends their outputs:

1. **HR-LAM Best-Match (1–2 km micro-grid)** — `38%` weight
2. **ECMWF IFS HRES 0.25° (9 km European Centre)** — `27%` weight
3. **DWD ICON-EU / Seamless (2–7 km German Weather Service)** — `22%` weight
4. **NOAA GFS Seamless (13–25 km Global Forecast System)** — `13%` weight

- **Outlier Dampening**: Automatically down-weights any model that deviates by `> 2.2°C` from the 4-model median.
- **Orographic Lapse-Rate Calibration**: Adjusts 2m temperature and apparent temperature for high-altitude villages and mountain passes (`-6.5°C / 1000m`).
- **Wet-Bulb & Freezing-Level Snow Detection**: Cross-checks relative humidity, dew point, and the `0°C` freezing level height against station elevation to prevent false rain/snow classifications.

---

## 📦 How to Install & Use the GitHub Releases App Updater

### Option 1 — Download from GitHub Releases (Recommended)
1. Open the [**Releases**](../../releases/latest) page of this repository on your Android phone.
2. Tap **`ZephyrWeather.apk`** under **Assets** to download and install.

### Option 2 — In-App GitHub Releases Updater
1. Open **Zephyr Weather** and go to the **Settings (Ρυθμίσεις)** tab.
2. In the **GitHub Releases App Updater** card at the top:
   - Enter your GitHub repository slug (`owner/repo`) if you forked or published your own repository.
   - Tap **Check GitHub Releases for New Update** to query `/releases/latest`.
   - Tap **Download Latest APK** to download the newest `ZephyrWeather.apk` directly from GitHub Releases.

### Option 3 — Automatic GitHub Actions Release Pipeline
Every push to `main`/`master` or tag matching `v*` triggers `.github/workflows/build-apk.yml`, which:
1. Compiles `ZephyrWeather.apk` and `ZephyrWeather-v2.7.0.apk` with JDK 17 and Gradle.
2. Commits the updated `ZephyrWeather.apk` to the repository root.
3. Publishes a new **GitHub Release (`v2.7.0`)** with the installable APK attached under **Assets**.

---

## 🛠️ Project Structure

```text
.
├── .github/workflows/build-apk.yml   # Automated APK Build & GitHub Releases Publisher
├── ZephyrWeather.apk                 # Direct-download Android APK package (v2.7.0)
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/
    │   ├── MainActivity.kt           # Edge-to-edge Compose shell & GPS permission flow
    │   ├── data/                     # Room Database, Preferences & 4-Model WeatherRepository
    │   ├── model/                    # Domain models, WMO mapper & bilingual localization
    │   ├── network/                  # Open-Meteo NWP, Air Quality, Nominatim & GitHub Releases APIs
    │   ├── ui/                       # WeatherViewModel, Forecast/Radar/Cities/Settings screens & 3D widgets
    │   ├── util/                     # Zero-Wakelock JobScheduler sync & SevereWeatherNotifier
    │   └── widget/                   # 4x2 Glance-style RemoteViews Home Screen Widget
    └── res/                          # Custom adaptive launcher icon & Material 3 vector drawables
```
