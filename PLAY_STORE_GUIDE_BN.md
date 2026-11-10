# NG Browser — লোগো ও Google Play প্রস্তুতি গাইড

NayaGram Platform · প্যাকেজ: `org.nayagram.ngbrowser`

---

## ১. আসল অবস্থা (জানা জরুরি)

এই রিপোজিটরিতে **দুটি আলাদা জিনিস** আছে:

| অংশ | ফোল্ডার | এখন কী হয় |
|---|---|---|
| **NG Browser Android অ্যাপ** (WebView ভিত্তিক) | `app/src/main/` | GitHub Actions এটাই বিল্ড করে → এই **AAB ফাইলটাই Play Store-এ যাবে** |
| **Brave-core / Chromium কোড** (C++) | বাকি সব ফোল্ডার | কোথাও বিল্ড হয় না। এটা বিল্ড করতে Chromium সোর্স (~১০০ জিবি), শক্তিশালী Linux মেশিন ও কয়েক ঘণ্টা লাগে |

তাই এখন যা আপলোড করবেন, সেটা Android-এর নিজস্ব WebView ইঞ্জিনে চলা একটি ব্রাউজার।
Play Store লিস্টিং-এ **"Brave-core powered" বা "Chromium engine" লিখবেন না**, যতক্ষণ না সত্যিই সেটা বিল্ড করছেন।
না হলে Google-এর "Misleading claims" নীতিতে অ্যাপ বাতিল হতে পারে।

---

## ২. লোগো কোথায় কোথায় বসানো হয়েছে

### Android অ্যাপ (Play Store-এ যা যাবে)
| ফাইল | সাইজ | কাজ |
|---|---|---|
| `app/src/main/res/mipmap-*/ic_launcher.png` | 48 / 72 / 96 / 144 / 192 px | পুরনো Android-এর আইকন |
| `app/src/main/res/mipmap-*/ic_launcher_round.png` | একই | গোল আইকন |
| `app/src/main/res/mipmap-*/ic_launcher_foreground.png` | 108 / 162 / 216 / 324 / 432 px | Android 8+ adaptive আইকন |
| `app/src/main/res/mipmap-*/ic_launcher_monochrome.png` | একই | Android 13+ থিমড (এক রঙের) আইকন |
| `app/src/main/res/drawable/ic_launcher_background.xml` | — | আইকনের পেছনে নীল-সায়ান রঙ |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | — | উপরের সবগুলো একসাথে জোড়া দেয় |

আগে প্রতিটি ফোল্ডারে একই 1254×1254 px-এর বড় ছবি (১.৩ MB) রাখা ছিল, আর রাউন্ড আইকন ভুল ফাইলের দিকে দেখাচ্ছিল। এখন প্রতিটি সাইজ ঠিক মাপে আছে।

### Play Store লিস্টিং (Play Console-এ নিজে আপলোড করবেন)
| ফাইল | সাইজ |
|---|---|
| `store/play_store_icon_512.png` | 512×512 — App icon |
| Feature graphic | 1024×500 — **এখনো বানানো হয়নি** |
| ফোনের স্ক্রিনশট | কমপক্ষে ২টি — **এখনো বানানো হয়নি** |

### Brave-core অংশ (ভবিষ্যতে Chromium বিল্ড করলে কাজে লাগবে)
ZIP-এর `app/theme/brave/` ফোল্ডারে সব ফাইল রিপোজিটরির একই জায়গার জন্য বানানো আছে:
- `android/res_brave_*_base/mipmap-*/app_icon.png`, `layered_app_icon.png`, `layered_app_icon_background.png`
- `android/res_brave_*/drawable-*/fre_product_logo.png` (প্রথমবার খোলার স্ক্রিন)
- `product_logo_24/48/64/128/256*.png`, `product_logo_22_mono.png`, `linux/product_logo_*.png`
- `win/*.ico`, `win/tiles/Logo.png`, `win/tiles/SmallLogo.png`
- `mac/**/app.icns`

যেগুলো বানানো যায়নি, আপনাকে নিজে করতে হবে:
- `mac/**/Assets.car`: শুধু Mac-এর Xcode দিয়ে বানানো যায়
- `product_logo.svg`, `product_logo_animation.svg`: লোগোর ভেক্টর (SVG) ফাইল লাগবে
- `ios/brave-ios/.../AppIcon`: iOS অংশ

---

## ৩. যা ঠিক করা হয়েছে

1. **বিল্ড ফেল হওয়া বন্ধ**: `gradlew` ফাইলটা আসল Gradle wrapper ছিল না, তাই GitHub-এর নতুন Gradle-এর সাথে মিলছিল না। এখন আসল wrapper (Gradle 8.9) দেওয়া হয়েছে।
2. **Android 15 (targetSdk 35)**: Google Play এখন নতুন অ্যাপ ও আপডেটের জন্য এটা চায়। Android Gradle Plugin 8.7.3-এ আপডেট করা হয়েছে।
3. **ভার্সন নম্বর নিজে নিজে বাড়বে**: প্রতিটি GitHub বিল্ডে versionCode = 100 + রান নম্বর। Play Store একই নম্বর দ্বিতীয়বার নেয় না।
4. **পাসওয়ার্ড সরানো হয়েছে**: `NayaGram2026SecurePass!` পাসওয়ার্ডটা পাবলিক কোডে লেখা ছিল। এখন শুধু GitHub Secrets থেকে নেওয়া হয়।
5. **সাইনিং কী**: আগে Secret না থাকলে প্রতিটি বিল্ডে নতুন কী তৈরি হতো, ফলে Play Store পরের আপডেট নিত না। এখন Secret না থাকলে বিল্ড সতর্কবার্তা দেখাবে আর Release বানাবে না।
6. **Android 15-এ টুলবার**: স্ট্যাটাস বারের নিচে ঢেকে যাবে না।
7. **ব্র্যান্ডিং**: `app/theme/brave/BRANDING` আর সব `channel_constants.xml` ফাইলে "Brave" নাম বদলে "NG Browser" করা হয়েছে।

---

## ৪. আপনার করণীয় (ধাপে ধাপে)

### ক) স্থায়ী সাইনিং কী তৈরি (একবারই, আর কখনো হারাবেন না)
কম্পিউটারে (Java থাকলে):
```
keytool -genkeypair -v -keystore ngbrowser-upload.jks -alias ngbrowser -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 ngbrowser-upload.jks > keystore.txt
```
GitHub → Settings → Secrets and variables → Actions → New secret:
- `ANDROID_KEYSTORE_BASE64` = keystore.txt-এর লেখা
- `KEYSTORE_PASSWORD` = **নতুন** পাসওয়ার্ড (পুরনোটা সবাই দেখে ফেলেছে, ব্যবহার করবেন না)
- `KEY_PASSWORD` = একই বা আলাদা পাসওয়ার্ড
- `KEY_ALIAS` = `ngbrowser`

`.jks` ফাইল ও পাসওয়ার্ড Google Drive বা অন্য নিরাপদ জায়গায় ব্যাকআপ রাখুন। **রিপোজিটরিতে কখনো রাখবেন না।**

### খ) Google Play Console
1. Developer account ($25, একবার) → Create app → নাম "NG Browser"
2. **Play App Signing** চালু রাখুন (ডিফল্ট)
3. **Privacy Policy URL** দিন (ব্রাউজারের জন্য বাধ্যতামূলক)
4. **Data safety** ফর্ম পূরণ করুন: লোকেশন অনুমতি আছে, সেটা জানাতে হবে
5. **Content rating** আর **Target audience** পূরণ করুন
6. Store listing-এ 512 আইকন, 1024×500 feature graphic আর স্ক্রিনশট দিন
7. নতুন ব্যক্তিগত অ্যাকাউন্ট হলে প্রথমে **Closed testing**-এ কমপক্ষে ১২ জন টেস্টারকে ১৪ দিন চালাতে হবে, তারপর Production-এ দেওয়া যাবে
8. GitHub → Actions → সর্বশেষ সফল বিল্ড → `NGBrowser-AAB-…` ডাউনলোড → `.aab` ফাইলটা Play Console-এ আপলোড করুন

### গ) Brave-এর নিয়ম (লাইসেন্স ও ট্রেডমার্ক)
- Brave-core **MPL-2.0** লাইসেন্সে। `LICENSE` ফাইল রাখুন, আর বদলানো কোড পাবলিক রাখুন (এখন পাবলিক আছে ✅)
- "Brave" নাম, Brave-এর সিংহ লোগো আর "Leo" নাম Brave-এর ট্রেডমার্ক। নিজের অ্যাপে এগুলো দেখানো যাবে না
- Brave-এর সার্ভার (Rewards, Sync, Leo AI, News, VPN, Wallet API key) নিজের অ্যাপে ব্যবহার করতে Brave-এর অনুমতি লাগে। এগুলো বন্ধ রাখুন বা নিজের সার্ভার দিন
- Brave-core বিল্ড করলে বাকি "Brave" লেখা এখানে আছে: `app/brave_strings.grd`, `browser/ui/android/strings/android_brave_strings.grd`, `components/components_brave_strings.grd`
- `BRANDING`-এর `MAC_TEAM_ID` এখনো Brave-এর। Mac বিল্ড করলে নিজের Apple Team ID দিন
