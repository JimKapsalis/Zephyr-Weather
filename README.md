<div align="center">

<img src="app/src/main/res/drawable/ic_launcher_foreground.png" width="124" height="124" alt="Zephyr Weather Icon" />

# 🌤️ Zephyr Weather for Android (`v2.9.0`)
### **4-Model Super-Ensemble Forecasting • Instant GPS • Best Performance for All Android Devices • Live Doppler Radar • Bilingual (EN / ΕΛ)**

---

# 📲 ⬇️ DOWNLOAD ANDROID APK HERE ⬇️

<table>
  <tr>
    <td align="center" width="540">
      <br/>
      <h2>🚀 <b>OFFICIAL ANDROID APK INSTALLER (v2.9.0 • Build 28)</b></h2>
      <p><b>Tap either button below on your Android phone or tablet to download & install <code>ZephyrWeather.apk</code> immediately:</b></p>
      <p>
        <a href="../../releases/latest/download/ZephyrWeather.apk">
          <img src="https://img.shields.io/badge/%E2%AC%87%EF%B8%8F_DOWNLOAD_APK_NOW-ZephyrWeather.apk_(v2.9.0)-10B981?style=for-the-badge&logo=android&logoColor=white" height="54" alt="Download APK from Releases" />
        </a>
      </p>
      <p>
        <a href="./ZephyrWeather.apk">
          <img src="https://img.shields.io/badge/%F0%9F%93%A6_DIRECT_REPO_DOWNLOAD-Click_Here_%E2%86%92_Download_Raw_APK-00E5FF?style=for-the-badge&logo=github&logoColor=black" height="46" alt="Direct Repo APK Download" />
        </a>
      </p>
      <p>
        🔗 <a href="../../releases/latest"><b>Open GitHub Releases Page (v2.9.0)</b></a> &nbsp;•&nbsp;
        📥 <a href="./ZephyrWeather.apk"><b>Direct File: <code>/ZephyrWeather.apk</code></b></a>
      </p>
      <br/>
    </td>
  </tr>
</table>

---

[![Latest Release](https://img.shields.io/badge/GitHub_Release-v2.9.0-00E5FF?style=for-the-badge&logo=github&logoColor=white)](../../releases/latest)
[![Universal Performance](https://img.shields.io/badge/All_Android_Devices-60Hz_%2F_90Hz_%2F_120Hz_Optimized-22C55E?style=for-the-badge&logo=android&logoColor=white)](#-changelog--all-device-performance-engine-v280)
[![4-Model Ensemble](https://img.shields.io/badge/NWP_Ensemble-ECMWF_%2B_ICON_%2B_GFS_%2B_HR--LAM-F59E0B?style=for-the-badge)](#-4-model-super-ensemble-meteorological-engine)

</div>

---

## 📋 Changelog & All-Device Performance Engine (`v2.9.0`)

- **Universal Best Performance Across All Android Devices**: Engineered for smooth 60Hz, 90Hz, and 120Hz rendering on all Android phones and tablets with draw-phase Compose animations, zero per-frame list recompositions, and a lightweight `~70 MB` memory footprint.
- **Ultra-Low Battery Consumption**: Uses Android's Doze-aware `JobScheduler` and automatic `ON_STOP` lifecycle pausing so all particle canvases, clocks, and background tasks sleep completely when the screen is off.
- **Instant Sub-100ms GPS Location Finding**: Resolves cached OS `LocationManager` and `FusedLocationProviderClient.lastLocation` coordinates first before racing live providers, paired with fast parallel 1.2s reverse-geocoding.
- **Crystal-Transparent Navigation Bar & 3D Animated Icons**: Full-color 3D navigation bar icons over a transparent glass navigation bar and smooth animated weather condition icons.
- **Built-In GitHub Releases App Updater**: Located at the bottom of the **Settings** tab for one-tap update checking and direct APK downloads from GitHub Releases.

---

## ✨ Highlights & Key Features

| Feature | Description |
| :--- | :--- |
| ⚡ **Instant Sub-100ms GPS Location** | Checks cached OS `LocationManager` & `FusedLocationProviderClient.lastLocation` first for near-instant city detection, backed by a parallel live GNSS/Network race and 1.2s reverse-geocoding. |
| 🛰️ **4-Model Super-Ensemble Engine** | Fuses **HR-LAM Best-Match (1–2 km)**, **ECMWF IFS 0.25° (9 km)**, **DWD ICON Seamless (2–7 km)**, and **NOAA GFS Seamless (13–25 km)** in parallel with outlier dampening, altitude lapse-rate calibration (`-6.5°C / 1000m`), and wet-bulb snow physics. |
| 🚀 **Universal All-Device Performance Engine** | Smooth 60Hz/90Hz/120Hz GPU draw-phase rendering, `~70 MB RAM`, zero background wake alarms, and instant lifecycle suspension when the screen turns off across all Android phones and tablets. |
| 🔄 **Built-In GitHub Releases App Updater** | Checks the **GitHub Releases API** (`/repos/{owner}/{repo}/releases/latest`) from the bottom of **Settings**, compares semantic version tags, displays the release changelog, and downloads new APK builds in one tap. |
| 🏔️ **Precision Mountain, Village & Snow Telemetry** | Covers all cities, towns, islands, and mountain villages across Greece and worldwide with live elevation meters, freezing level (`0°C` isotherm), snow depth, and snow accumulation alerts. |
| 🧭 **Photorealistic Sky, Transparent Nav Bar & 3D Glass Widgets** | Photorealistic multi-layered atmospheric sky canvas (`WeatherSkyBackdrop`), crystal-transparent bottom navigation bar with 3D icons, **Helios 24h Chrono-Dial**, **Smart Comfort & Outfit Advisor**, **Analog Barometer**, and a 4×2 Home Screen AppWidget. |
| 🇬🇷 **Bilingual English & Greek (`EN` / `ΕΛ`)** | Instant one-tap switching between English and Greek with localized Beaufort wind scales, EMY severe weather bulletins, and metric/imperial units (`°C / °F`, `km/h / mph`, `hPa / inHg`). |

---

## 🛰️ 4-Model Super-Ensemble Meteorological Engine

When **Multi-Model Ensemble (`BEST_MATCH`)** is selected, Zephyr Weather queries 4 independent Numerical Weather Prediction (NWP) models simultaneously and blends their outputs:

1. **HR-LAM Best-Match (1–2 km micro-grid)** — `38%` weight
2. **ECMWF IFS HRES 0.25° (9 km European Centre)** — `27%` weight
3. **DWD ICON-EU / Seamless (2–7 km German Weather Service)** — `22%` weight
4. **NOAA GFS Seamless (13–25 km Global Forecast System)** — `13%` weight

---

## 🛠️ Project Structure

```text
.
├── .github/workflows/build-apk.yml   # Automated APK Build & GitHub Releases Publisher (v2.9.0)
├── ZephyrWeather.apk                 # Direct-download Android APK package (v2.9.0, Build 28)
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/example/
    │   ├── MainActivity.kt           # Edge-to-edge Compose shell, transparent nav bar & instant GPS fast-path
    │   ├── data/                     # Room Database, Preferences & 4-Model WeatherRepository
    │   ├── model/                    # Domain models, WMO mapper & bilingual localization
    │   ├── network/                  # Open-Meteo NWP, Air Quality, Nominatim & GitHub Releases APIs
    │   ├── ui/                       # WeatherViewModel, Forecast/Radar/Cities/Settings screens & 3D widgets
    │   ├── util/                     # Doze-safe JobScheduler sync & SevereWeatherNotifier
    │   └── widget/                   # 4x2 Glance-style RemoteViews Home Screen Widget
    └── res/                          # Custom adaptive launcher icon & Material 3 vector drawables
```
