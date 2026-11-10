# NG Browser 🚀
> Next-Generation Privacy Web Browser Engine Powered by Brave-Core and NayaGram Platform

![Build Status](https://github.com/NayaGramPlatform/NGBrowser/actions/workflows/build-ng-browser.yml/badge.svg?branch=main)
[![Latest Release](https://img.shields.io/github/v/release/NayaGramPlatform/NGBrowser?color=emerald&label=Version)](https://github.com/NayaGramPlatform/NGBrowser/releases/tag/v1.0.0)
[![Package Identifier](https://img.shields.io/badge/Package-org.nayagram.ngbrowser-orange.svg)](https://github.com/NayaGramPlatform/NGBrowser)
[![License](https://img.shields.io/github/license/NayaGramPlatform/NGBrowser?color=blue)](LICENSE)

---

## 📱 Official Downloads & Releases

| Target | Format | Download Link | Description |
|---|---|---|---|
| **Android Phone (Direct Install)** | `.apk` | [📥 NGBrowser-Release-APK.apk](https://github.com/NayaGramPlatform/NGBrowser/releases/download/v1.0.0/NGBrowser-Release-APK.apk) | Direct 100% installable APK for Android 7.0 - 15 |
| **Google Play Console** | `.aab` | [🌐 NGBrowser-Release-AAB.aab](https://github.com/NayaGramPlatform/NGBrowser/releases/download/v1.0.0/NGBrowser-Release-AAB.aab) | Signed Google Play App Bundle (`org.nayagram.ngbrowser`) |
| **Tagged Version Release** | `.apk` | [🏷️ NGBrowser-v1.0.0-Release.apk](https://github.com/NayaGramPlatform/NGBrowser/releases/download/v1.0.0/NGBrowser-v1.0.0-Release.apk) | Official v1.0.0 Release Binary |

---

## ✨ Key Architectural Features

- **Engine Foundation**: Powered by Brave-Core `v1.97.60` and Chromium `124+` base engine.
- **NG Shields**: Built-in Rust & C++ ad-block, tracker protection, and cosmetic filter engine (`filters.dat`).
- **Official Package Identifier**: `org.nayagram.ngbrowser`
- **Default Search Engine**: `NG Search`
- **Security & Privacy**: Strict HTTPS enforcement, fingerprinting resistance, cleartext traffic protection.

---

## 🛠️ Repository Architecture & Quick Start

```bash
# Clone permanent main branch with submodules
git clone --recursive -b main https://github.com/NayaGramPlatform/NGBrowser.git
cd NGBrowser

# Install dependencies and apply Chromium patches
npm install
npm run apply_patches
```

---

## 🏢 Platform & Credits

- **Organization**: NayaGram Platform
- **Lead Developer**: CEO NayaGram (`nghuzayfa@gmail.com`)
- **Upstream Engine**: [brave/brave-core](https://github.com/brave/brave-core)
