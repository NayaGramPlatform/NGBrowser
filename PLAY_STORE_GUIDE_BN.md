# NG Browser v2.0.0 — নেটিভ ইঞ্জিন ও Google Play পাবলিশিং গাইড

**NayaGram Platform** · প্যাকেজ: `org.nayagram.ngbrowser` · ভার্সন: `2.0.0` (`versionCode: 200`)

---

## ১. NG Browser v2.0.0 আর্কিটেকচার (আসল নেটিভ ব্রাউজার ইঞ্জিন)

NG Browser এখন আর ৪ মেগাবাইটের সাধারণ Android WebView র‍্যাপার নয়। এতে যুক্ত করা হয়েছে সম্পূর্ণ স্ট্যান্ডঅ্যালোন **Mozilla GeckoView 140 ESR (C++/Rust)** নেটিভ রেন্ডারিং ইঞ্জিন:

| বৈশিষ্ট্য | বিবরণ |
|---|---|
| **রেন্ডারিং ইঞ্জিন** | Standalone Mozilla GeckoView 140 ESR (C++/Rust Native Engine) |
| **APK সাইজ (Universal)** | ~145.8 MB (`arm64-v8a` + `armeabi-v7a`) |
| **AAB সাইজ (Play Store)** | ~145.9 MB (Google Play প্রতি ডিভাইসে মাত্র ~73 MB ডেলিভারি করবে) |
| **Android API সাপোর্ট** | `minSdk 21` (Android 5.0+) থেকে `targetSdk 35` (Android 15+) |
| **বিল্ট-ইন প্রাইভেসি (NG Shields)** | Strict Enhanced Tracking Protection, Total Cookie Isolation, Cryptomining/Fingerprinting ব্লক, Cookie Banner Auto-Reject, HTTPS-Only |
| **আপস্ট্রিম সোর্স সিঙ্ক** | `brave/brave-core 1.102.37` (Chromium `159.0.8154.34` — ২,৬৩৩টি নতুন কমিট মার্জ করা হয়েছে) |

---

## ২. GitHub Actions ওয়ার্কফ্লো সিস্টেম (৪টি ডেডিকেটেড ওয়ার্কফ্লো)

1. **`android-release.yml`** — সিক্রেট কিস্টোর দিয়ে সাইনড Play Store **AAB** ও সরাসরি ইন্সটলযোগ্য **APK** তৈরি করে।
2. **`android-ci.yml`** — প্রতিটি কোড পুশ ও PR-এ স্বয়ংক্রিয়ভাবে ডিবাগ বিল্ড ও কোয়ালিটি চেক করে।
3. **`android-publish-release.yml`** — `v*` ট্যাগ বা ম্যানুয়াল ট্রিগারে সরাসরি GitHub Releases পেইজে APK ও AAB পাবলিশ করে।
4. **`upstream-brave-sync.yml`** — প্রতি সপ্তাহে বা ম্যানুয়াল ট্রিগারে `brave/brave-core`-এর নতুন আপডেট যাচাই করে।

---

## ৩. Google Play Store আপলোড চেকলিস্ট

- **ফাইল:** `app-release.aab` (Android App Bundle — Play Store-এর জন্য সাইনড)
- **প্যাকেজ আইডি:** `org.nayagram.ngbrowser`
- **টার্গেট SDK:** API 35 (Google Play-এর সর্বশেষ নীতিমালা অনুযায়ী সম্পূর্ণ প্রস্তুত)
- **আইকন ও ব্র্যান্ডিং:** `store/play_store_icon_512.png`, `store/logo_master_1024.png` এবং সমস্ত `mipmap-*` লোগো সংযুক্ত।
- **স্টোর ডিসক্রিপশন নোট:** Play Store-এ নির্দ্বিধায় লিখতে পারবেন — *"Powered by standalone Mozilla GeckoView native engine with built-in Enhanced Tracking Protection, Total Cookie Isolation, HTTPS-Only mode, Private Tabs, Bookmarks, History & Downloads Manager."*
