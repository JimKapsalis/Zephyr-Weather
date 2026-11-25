<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.png" width="128" height="128" alt="Zephyr Weather Icon" />

# 🌤️ Zephyr Weather for Android (`v3.0.0`)
### **4-Model Super-Ensemble Forecasting • Instant GPS • Live Doppler Radar • 3D Atmospheric UI • Bilingual (EN / ΕΛ)**

[![Latest Release](https://img.shields.io/badge/Latest_Release-v3.0.0_(Build_30)-00E5FF?style=for-the-badge&logo=github&logoColor=white)](../../releases/latest)
[![Total APK Downloads](https://img.shields.io/github/downloads/JimKapsalis/Zephyr-Weather/total?style=for-the-badge&logo=android&logoColor=white&label=Total_APK_Downloads&color=10B981)](../../releases)
[![GitHub Stars](https://img.shields.io/github/stars/JimKapsalis/Zephyr-Weather?style=for-the-badge&logo=github&logoColor=white&label=GitHub_Stars&color=F59E0B)](../../stargazers)
[![Android Platform](https://img.shields.io/badge/Android-7.0%2B_(60Hz_%2F_90Hz_%2F_120Hz)-22C55E?style=for-the-badge&logo=android&logoColor=white)](#-whats-new-in-v300--changelog)
[![4-Model Ensemble](https://img.shields.io/badge/NWP_Models-ECMWF_%2B_ICON_%2B_GFS_%2B_HR--LAM-F59E0B?style=for-the-badge)](#-4-model-super-ensemble-meteorological-engine)

---

# 📲 ⬇️ DOWNLOAD ANDROID APK HERE ⬇️

<table>
  <tr>
    <td align="center" width="600">
      <br/>
      <h2>🚀 <b>OFFICIAL ANDROID APK INSTALLER — <code>v3.0.0</code> (Build 30)</b></h2>
      <p><b>Tap either button below on your Android phone or tablet to download & install <code>ZephyrWeather.1.apk</code> immediately:</b></p>
      <p>
        <a href="../../releases/latest/download/ZephyrWeather.1.apk">
          <img src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_DOWNLOAD_APK_FROM_RELEASES-ZephyrWeather.1.apk_(v3.0.0)-10B981?style=for-the-badge&logo=android&logoColor=white" height="56" alt="Download APK from Releases" />
        </a>
      </p>
      <p>
        <a href="./ZephyrWeather.1.apk">
          <img src="https://img.shields.io/badge/%F0%9F%93%A6_DIRECT_REPO_APK-Click_Here_%E2%86%92_Download_ZephyrWeather.1.apk-00E5FF?style=for-the-badge&logo=github&logoColor=black" height="48" alt="Direct Repo APK Download" />
        </a>
      </p>
      <p>
        🔗 <a href="../../releases/latest"><b>Open GitHub Releases Page (v3.0.0)</b></a> &nbsp;•&nbsp;
        📥 <a href="./ZephyrWeather.1.apk"><b>Direct Repository File: <code>/ZephyrWeather.1.apk</code></b></a>
      </p>
      <br/>
    </td>
  </tr>
</table>

</div>

---

## 📸 App Screenshots Gallery

<div align="center">
  <table>
    <tr>
      <td align="center" width="33%">
        <a href="https://iili.io/ofDBecB.jpg">
          <img src="https://iili.io/ofDBecB.jpg" width="260" alt="Zephyr Weather 10-Day & Hourly Forecast" />
        </a>
        <br/>
        <b>🌤️ 10-Day & Hourly Forecast</b>
        <br/>
        <sub>Photorealistic sky backdrop, 3D weather icons & temperature range bars</sub>
      </td>
      <td align="center" width="33%">
        <a href="https://iili.io/ofDoz8B.jpg">
          <img src="https://iili.io/ofDoz8B.jpg" width="260" alt="Zephyr Weather Live Doppler Radar" />
        </a>
        <br/>
        <b>📡 Live Doppler Radar</b>
        <br/>
        <sub>High-resolution satellite tiles, precipitation sweep & storm tracking</sub>
      </td>
      <td align="center" width="33%">
        <a href="https://iili.io/ofDoG9I.jpg">
          <img src="https://iili.io/ofDoG9I.jpg" width="260" alt="Zephyr Weather 3D Meteorological Widgets" />
        </a>
        <br/>
        <b>🧭 3D Telemetry Widgets</b>
        <br/>
        <sub>Helios 24h sun/moon chrono-dial, analog barometer, UV & AQI</sub>
      </td>
    </tr>
  </table>
</div>

---

## 🚀 [What's New in `v3.0.0` — Changelog (Tap Here to Download `ZephyrWeather.1.apk`)](../../releases/latest/download/ZephyrWeather.1.apk)

<div align="center">
  <a href="../../releases/latest/download/ZephyrWeather.1.apk">
    <img src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_CHANGELOG_DIRECT_APK_DOWNLOAD-ZephyrWeather.1.apk_(v3.0.0)-10B981?style=for-the-badge&logo=android&logoColor=white" height="48" alt="Download ZephyrWeather.1.apk from Changelog" />
  </a>
</div>

### ⚡ [Instant Startup & Fast-Path GPS (Download APK ⬇)](../../releases/latest/download/ZephyrWeather.1.apk)
- **Instant Saved-City Startup (`0ms` Wait)**: Opens directly into your saved city's forecast on the very first frame while refreshing live data silently in the background — never shows loading skeletons or repeated location prompts when reopening the app.
- **Sub-100ms Fast-Path GPS Location**: Immediately resolves cached OS `LocationManager` and `FusedLocationProviderClient.lastLocation` coordinates before racing live GNSS/Network providers, paired with parallel `1.2s` reverse-geocoding.

### 🎨 [Transparent Glass UI, Interactive Radar Zoom & Night Sky Star Count (Download APK ⬇)](../../releases/latest/download/ZephyrWeather.1.apk)
- **Interactive Doppler Radar Zoom (`1.0×` – `5.0×`)**: Supports two-finger pinch-to-zoom, double-tap zoom, drag-to-pan, floating `+` / `−` / `1×` zoom buttons, a smooth zoom slider, 4 satellite tile levels (`16x` Street to `9x` Region), and real-time Marshall-Palmer dBZ reflectivity telemetry.
- **Night Sky Visible Star Count & Stargazing Observatory**: Calculates real-time naked-eye visible star count (`0` – `3,500` stars), sky clarity %, limiting magnitude (`Mag`), synodic lunar illumination %, and optimal stargazing window from cloud cover, humidity, PM2.5 aerosols, visibility, and elevation.
- **Crystal-Transparent Bottom Navigation Bar**: Floating transparent glass navigation bar with full-color 3D icons across all four tabs (**Forecast**, **Radar**, **Cities**, **Settings**) and edge-to-edge system bar transparency.
- **Dynamic Draw-Phase Weather Animations**: Smooth Compose canvas animations for sun rays, drifting clouds, falling rain, snow storms, and thunderstorms that render directly in the GPU draw phase.

### 🔋 [Universal High Performance & Ultra-Low Battery Usage (Download APK ⬇)](../../releases/latest/download/ZephyrWeather.1.apk)
- **Smooth 60Hz / 90Hz / 120Hz Rendering**: Optimized for all Android phones and tablets with zero per-frame list recompositions and a lightweight `~70 MB` RAM footprint.
- **Zero Background Keep-Awake Drain**: Uses Android's Doze-aware `JobScheduler` without wakeup alarms and automatically pauses all canvas animations, timers, and clocks the moment the screen turns off or the app enters the background.

### 🔄 [Built-In GitHub Releases App Updater (Download `ZephyrWeather.1.apk` ⬇)](../../releases/latest/download/ZephyrWeather.1.apk)
- **One-Tap In-App Update Checker & Clickable Changelog**: Located at the bottom of the **Settings** tab — queries the GitHub Releases API (`/releases/latest`), compares version tags, and lets you tap the release changelog or download button to download `ZephyrWeather.1.apk` in one tap.

---

## ✨ Core Features

| Feature | Description |
| :--- | :--- |
| 🛰️ **4-Model Super-Ensemble Engine** | Fuses **HR-LAM Best-Match (1–2 km)**, **ECMWF IFS 0.25° (9 km)**, **DWD ICON Seamless (2–7 km)**, and **NOAA GFS Seamless (13–25 km)** in parallel with outlier dampening, altitude lapse-rate calibration (`-6.5°C / 1000m`), and wet-bulb snow physics. |
| ⚡ **Instant Sub-100ms GPS & Global Search** | Instant GPS coordinate resolution plus fuzzy search across all cities, islands, towns, and mountain villages in Greece and worldwide (supports English, Greek, and Greeklish). |
| 📡 **Live Doppler Radar & Satellite Map** | Interactive Web Mercator satellite map with real-time precipitation, wind stream particles, cloud cover, and temperature layers. |
| 🏔️ **Mountain, Village & Snow Telemetry** | Displays live station elevation (`m`), `0°C` freezing level height, snow depth, and snowfall accumulation alerts for high-altitude villages and mountain passes. |
| 🧭 **3D Meteorological Instruments & Widget** | Includes the **Helios 24h Sun/Moon Chrono-Dial**, **Smart Comfort & Outfit Advisor**, **Analog Barometer**, **Wind Compass Rose**, **Air Quality (US/EU AQI)**, and a **4×2 Home Screen AppWidget**. |
| 🇬🇷 **Bilingual English & Greek (`EN` / `ΕΛ`)** | Instant one-tap switching between English and Greek with localized Beaufort wind scales, EMY severe weather warnings, and metric/imperial units (`°C / °F`, `km/h / mph`, `hPa / inHg`). |

---

## 🛰️ 4-Model Super-Ensemble Meteorological Engine

When **Multi-Model Ensemble (`BEST_MATCH`)** is active, Zephyr Weather queries 4 independent Numerical Weather Prediction (NWP) models simultaneously and blends their forecasts:

1. **HR-LAM Best-Match (1–2 km micro-grid)** — `38%` weight
2. **ECMWF IFS HRES 0.25° (9 km European Centre)** — `27%` weight
3. **DWD ICON-EU / Seamless (2–7 km German Weather Service)** — `22%` weight
4. **NOAA GFS Seamless (13–25 km Global Forecast System)** — `13%` weight

- **Outlier Dampening**: Automatically down-weights any model deviating by `> 2.2°C` from the 4-model median.
- **Orographic Lapse-Rate Calibration**: Corrects 2m temperature and apparent temperature for elevation differences (`-6.5°C / 1000m`).
- **Wet-Bulb & Freezing-Level Snow Detection**: Cross-checks relative humidity, dew point, and the `0°C` freezing level against station altitude for accurate rain/snow classification.

---

## 🛠️ Project Structure

```text
.
├── .github/workflows/build-apk.yml   # Automated APK Build & GitHub Releases Publisher (v3.0.0)
├── ZephyrWeather.1.apk               # Direct-download Android APK package (v3.0.0, Build 30)
├── public/assets/                    # High-resolution store screenshots (screenshot_1..3.jpg)
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/
    │   ├── MainActivity.kt           # Edge-to-edge Compose shell, transparent nav bar & instant GPS fast-path
    │   ├── data/                     # Room Database, Preferences & 4-Model WeatherRepository
    │   ├── model/                    # Domain models, WMO mapper & bilingual localization
    │   ├── network/                  # Open-Meteo NWP, Air Quality, Nominatim & GitHub Releases APIs
    │   ├── ui/                       # WeatherViewModel, Forecast/Radar/Cities/Settings screens & 3D widgets
    │   ├── util/                     # Doze-safe JobScheduler sync & SevereWeatherNotifier
    │   └── widget/                   # 4x2 Frosted-Glass Home Screen AppWidget
    └── res/                          # Custom adaptive launcher icon & Material 3 vector drawables
```
