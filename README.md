# 🌐 NG Browser (NGBrowser)

> **Next-Generation Privacy-Focused Web Browser Engine**  
> Powered by `Brave-Core` (Chromium 124+ Architecture), Native Rust Ad-Blocking (`NG Shields`), Gemini AI Assistant, and Web3 Integration.  
> Developed & Maintained by **NayaGram Platform**.

---

## 💎 Features & Architecture

- 🛡️ **NG Shields**: Built-in Rust-based ad-blocker, tracker defender, and fingerprint randomized shielding.
- ⚡ **Brave-Core Engine (v1.97.60)**: Over 1,000+ custom Chromium git patches applied directly via `npm run apply_patches`.
- 🤖 **NG AI Assistant**: Deep integration with Gemini models for instant summarization, contextual translation, and page intelligence.
- 🔒 **Zero Telemetry / Strict Privacy**: Enforces HTTPS upgrades, anti-fingerprinting, and script whitelisting.
- 📱 **Multi-Platform Support**: Ready for Windows, macOS, Linux, Android, and iOS.

---

## 🚀 Quick Start & Development

### 1. Clone with all Submodules
```bash
git clone --recursive -b main https://github.com/NayaGramPlatform/NGBrowser.git
cd NGBrowser
```

### 2. Install Tools & Apply Chromium Patches
```bash
npm install
npm run apply_patches
```

### 3. Generate Build Configuration (args.gn)
```bash
gn gen out/Release --args='is_official_build=true brave_project_name="NG Browser" enable_brave_shields=true'
```

### 4. Compile Installer Binaries
```bash
autoninja -C out/Release brave
```

---

## 🏛️ Repository Branch Policy

- **`main`**: Permanent, production-grade default branch containing the latest stable engine, 1,000+ patches, and full browser assets.
- **`1.97.x`**: Brave-Core release tracking branch.

---

## 📄 License
This project is licensed under the Mozilla Public License 2.0 (MPL-2.0).
