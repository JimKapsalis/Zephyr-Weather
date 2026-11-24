# 🌦️ Zephyr Weather — Multi-Model Android Weather & Live Radar

[![Build & Release APK](https://img.shields.io/badge/Android-APK%20Ready-38BDF8?style=for-the-badge&logo=android&logoColor=white)](./ZephyrWeather.apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Models](https://img.shields.io/badge/Forecast-ECMWF%20%2B%20ICON%20%2B%20GFS-10B981?style=for-the-badge)](https://open-meteo.com/)

**Zephyr Weather** is a bilingual (**English / Ελληνικά**) Android weather application built with **Kotlin** and **Jetpack Compose**. It fuses the world's top numerical weather prediction (NWP) models with 15-minute live Doppler nowcast data, hardware GPS location detection (`FusedLocationProviderClient`), and high-precision elevation (`Υψόμετρο`) telemetry for every city, town, and mountain village worldwide.

---

## 📲 Direct APK Download (GitHub)

You can download and install the Android APK directly from this repository in two ways:

1. **Direct Repository File**:
   - Click **[`ZephyrWeather.apk`](./ZephyrWeather.apk)** in the root of this repository and tap **"Download raw file"** (or **"View raw"**) to download the installable `.apk` directly to your Android phone or PC.
2. **GitHub Releases**:
   - Open the **[Releases](../../releases)** tab on the right side of GitHub and tap **`ZephyrWeather.apk`** under **Assets**.

---

## ✨ Key Features

- **🛰️ 3-Model Ensemble Consensus + 15-Min Nowcast**:
  - Automatically blends **Open-Meteo High-Resolution Best-Match**, **ECMWF IFS (0.25°)**, and **DWD ICON Seamless (2–7 km Europe & Terrain)** in parallel, plus **15-minute Nowcast (`minutely_15`)** precipitation and snowfall assimilation.
  - Or switch manually in **Settings** between:
    - **Multi-Model + Live Radar (Best)** (`1–2 km Radar + 3-Model Ensemble`)
    - **ECMWF IFS (European Centre)** (`0.25° Global #1`)
    - **DWD ICON (German Weather Service)** (`2–7 km Europe & Mountain Terrain HD`)
    - **Météo-France AROME / ARPEGE** (`1.3–10 km Mediterranean HD`)
    - **NOAA GFS + HRRR (US Weather Service)** (`3–13 km Seamless`)
    - **UK Met Office (UKMO Seamless)** (`1.5–10 km Global`)
- **📍 True Hardware GPS Location (`FusedLocationProviderClient`)**:
  - Requests runtime `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` permissions and resolves live GPS coordinates + altitude without any hardcoded default city.
- **⛰️ Global City, Town & Mountain Village Search with Elevation (`Υψόμετρο`)**:
  - Supports Greek, English, and Greeklish search across global cities and small mountain villages (e.g., *Κάτω Παλαιοκαρυά*, *Άνω Παλαιοκαρυά*, *Περτούλι*, *Ελάτη*, *Μέτσοβο*, *Αράχωβα*) with exact elevation in meters (`m`) / feet (`ft`) and freezing-level snow detection.
- **📊 Compact 24-Hour & 10-Day Forecast**:
  - Interactive 24-hour temperature, feels-like, precipitation, wind, and UV charts.
  - Clean 10-day forecast cards that stay compact by default and expand into an hourly day breakdown only when tapped.
- **🗺️ Live Interactive Rain, Wind, Cloud & Thermal Radar**:
  - Real-time RainViewer Doppler radar tiles + atmospheric wind particle streamlines.
- **🔄 Built-in App & Weather Data Updater**:
  - One-tap updater in **Settings** (`Check & Install Updates`) to sync the latest village catalog, elevation telemetry, and multi-model forecasts.

---

## 🗂️ Clean Repository Structure

```text
├── .github/workflows/build-apk.yml   # Automated GitHub Actions APK builder & release publisher
├── app/
│   ├── build.gradle.kts              # Android app module Gradle configuration
│   └── src/main/
│       ├── AndroidManifest.xml       # Permissions, MainActivity, Widget & Boot receivers
│       ├── java/com/example/
│       │   ├── MainActivity.kt       # FusedLocationProviderClient GPS + App shell + Setup screen
│       │   ├── data/                 # Room Database (v8), WeatherRepository 3-model ensemble engine
│       │   ├── model/                # WeatherModels, WMO weather mapper, multi-model providers
│       │   ├── network/              # Retrofit + Moshi Open-Meteo, AQI & Geocoding services
│       │   ├── ui/                   # WeatherViewModel + Forecast, Radar, Cities & Settings screens
│       │   ├── util/                 # Severe weather notifier & WorkManager background sync
│       │   └── widget/               # Home screen Glance / AppWidget provider
│       └── res/                      # Adaptive launcher icons, widgets & bilingual strings
├── gradle/libs.versions.toml         # Version catalog
├── ZephyrWeather.apk                 # Direct-downloadable Android APK
├── build.gradle.kts                  # Root Gradle configuration
└── settings.gradle.kts               # Project settings
```

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 17**
- **Android SDK** (API 35 / compileSdk 35, minSdk 26)

### Build Commands
```bash
# Build Debug APK
gradle assembleDebug

# Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
