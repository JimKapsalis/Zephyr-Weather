<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.png" width="128" height="128" alt="Zephyr Weather Icon" />

# 🌤️ Zephyr Weather for Android (`v1.2`)
### **4-Model Super-Ensemble Forecasting • Instant GPS • Live Doppler Radar • 3D Atmospheric UI • Bilingual (EN / ΕΛ)**

[![Latest Release](https://img.shields.io/badge/Latest_Release-v1.2_(Build_12)-00E5FF?style=for-the-badge&logo=github&logoColor=white)](../../releases/latest)
[![Total APK Downloads](https://img.shields.io/badge/Total_APK_Downloads-123%2B-10B981?style=for-the-badge&logo=android&logoColor=white)](../../releases)
[![GitHub Stars](https://img.shields.io/github/stars/JimKapsalis/Zephyr-Weather?style=for-the-badge&logo=github&logoColor=white&label=GitHub_Stars&color=F59E0B)](../../stargazers)
[![Android Platform](https://img.shields.io/badge/Android-7.0%2B_(60Hz_%2F_90Hz_%2F_120Hz)-22C55E?style=for-the-badge&logo=android&logoColor=white)](#-whats-new-in-v12--changelog-tap-here-to-download-zephyrweather12apk)
[![4-Model Ensemble](https://img.shields.io/badge/NWP_Models-ECMWF_%2B_ICON_%2B_GFS_%2B_HR--LAM-F59E0B?style=for-the-badge)](#-4-model-super-ensemble-meteorological-engine)

---

# 📲 ⬇️ DOWNLOAD ANDROID APK HERE ⬇️

<table>
  <tr>
    <td align="center" width="600">
      <br/>
      <h2>🚀 <b>OFFICIAL ANDROID APK INSTALLER — <code>v1.2</code> (Build 12)</b></h2>
      <p><b>Tap either button below on your Android phone or tablet to download & install <code>ZephyrWeather.1.2.apk</code> immediately:</b></p>
      <p>
        <a href="../../releases/latest/download/ZephyrWeather.1.2.apk">
          <img src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_DOWNLOAD_APK_FROM_RELEASES-ZephyrWeather.1.2.apk_(v1.2)-10B981?style=for-the-badge&logo=android&logoColor=white" height="56" alt="Download APK from Releases" />
        </a>
      </p>
      <p>
        <a href="./ZephyrWeather.1.2.apk">
          <img src="https://img.shields.io/badge/%F0%9F%93%A6_DIRECT_REPO_APK-Click_Here_%E2%86%92_Download_ZephyrWeather.1.2.apk-00E5FF?style=for-the-badge&logo=github&logoColor=black" height="48" alt="Direct Repo APK Download" />
        </a>
      </p>
      <p>
        🔗 <a href="../../releases/latest"><b>Open GitHub Releases Page (v1.2)</b></a> &nbsp;•&nbsp;
        📥 <a href="./ZephyrWeather.1.2.apk"><b>Direct Repository File: <code>/ZephyrWeather.1.2.apk</code></b></a> &nbsp;•&nbsp;
        📂 <a href="./public/assets/ZephyrWeather.1.2.apk"><b>Assets APK: <code>/public/assets/ZephyrWeather.1.2.apk</code></b></a>
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

## 🚀 [What's New in `v1.2` — Changelog (Tap Here to Download `ZephyrWeather.1.2.apk`)](../../releases/latest/download/ZephyrWeather.1.2.apk)

<div align="center">
  <a href="../../releases/latest/download/ZephyrWeather.1.2.apk">
    <img src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_CHANGELOG_DIRECT_APK_DOWNLOAD-ZephyrWeather.1.2.apk_(v1.2)-10B981?style=for-the-badge&logo=android&logoColor=white" height="48" alt="Download ZephyrWeather.1.2.apk from Changelog" />
  </a>
</div>

### 🔋 [Zero-Drain CPU & Battery Fix (Download APK ⬇)](../../releases/latest/download/ZephyrWeather.1.2.apk)
- **Eliminated Background GPS Listener CPU Drain**: Fixed background `LocationManager` cleanup using one-shot `CancellationSignal` requests and guaranteed `finally` listener removal so GPS never runs in the background after acquiring a fix.
- **0% Idle CPU Animations**: Replaced 20+ continuous 120Hz infinite canvas animation loops with bounded entrance animations that settle to **0.0% idle CPU** once loaded, plus Doze-aware `JobScheduler` background sync without wakeup alarms.

### 🎨 [High-Contrast Live Weather Alerts, Lightning Distance, Clothing Advisor & Radar Zoom (Download APK ⬇)](../../releases/latest/download/ZephyrWeather.1.2.apk)
- **High-Contrast Live Weather Alerts & Direct Radar View Switch**: Enlarged the Live Weather Alerts card title, description copy, and metric pills (`Rain %`, `mm/h`, `Gusts`) so they are effortless to read even at thumbnail/Reddit feed size, with a direct **"Radar View →"** button to jump straight to the dedicated Radar tab.
- **Live Lightning Strike Distance & Storm ETA**: Displays real-time distance (`km`) to the nearest thunderstorm or convective rain cell, exact arrival countdown (`ETA`), lightning risk %, and storm drift vector with one-tap jump to Live Doppler Radar.
- **Bottom Clothing & Outdoor Activity Advisor**: Positioned at the bottom of the Forecast tab — recommends the ideal outfit (`Winter Coat`, `Light Jacket`, `Breathable T-Shirt`), umbrella need (`%`), UV/wind protection gear, and the best upcoming hour for a walk or run.
- **Interactive Doppler Radar Zoom (`1.0×` – `5.0×`)**: Supports two-finger pinch-to-zoom, double-tap zoom, drag-to-pan, floating `+` / `−` / `1×` zoom buttons, a smooth zoom slider, 4 satellite tile levels (`16x` Street to `9x` Region), and real-time Marshall-Palmer dBZ reflectivity telemetry.
- **Night Sky Visible Star Count & Stargazing Observatory**: Calculates real-time naked-eye visible star count (`0` – `3,500` stars), sky clarity %, limiting magnitude (`Mag`), synodic lunar illumination %, and optimal stargazing window from cloud cover, humidity, PM2.5 aerosols, visibility, and elevation.
- **Transparent Liquid-Glass UI & Smart Anti-Spam Alerts**: Frosted translucent glass cards scrolling smoothly behind the transparent glass bottom navigation bar (hidden during Location Setup) plus strict 6-hour anti-spam cooldown so notifications only fire for critical weather.

### ⚡ [Instant Startup & Multi-Provider Fast-Path GPS (Download APK ⬇)](../../releases/latest/download/ZephyrWeather.1.2.apk)
- **Instant Saved-City Startup (`0ms` Wait)**: Opens directly into your saved city's forecast on the very first frame while refreshing live data silently in the background.
- **Multi-Provider Fast-Path GPS + Automatic IP Fallback**: Immediately resolves cached OS `LocationManager` and `FusedLocationProviderClient.lastLocation` coordinates before racing High/Balanced GNSS and Network providers, with automatic network IP geolocation fallback indoors.

### 🔄 [Built-In GitHub Releases App Updater (Download `ZephyrWeather.1.2.apk` ⬇)](../../releases/latest/download/ZephyrWeather.1.2.apk)
- **One-Tap In-App Update Checker & Clickable Changelog**: Located at the bottom of the **Settings** tab — queries the GitHub Releases API (`/releases/latest`), tracks cumulative APK downloads (`123+`), compares semantic version tags across the release title, asset filename, and tag, and lets you download `ZephyrWeather.1.2.apk` in one tap.

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
├── .github/workflows/build-apk.yml   # Automated APK Build & GitHub Releases Publisher (v1.2)
├── ZephyrWeather.1.2.apk             # Direct-download Android APK package (v1.2, Build 12)
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
