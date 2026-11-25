# NG Browser v3.12.18 NG — Google Play Store পাবলিশিং গাইড

**NayaGram Platform** · প্যাকেজ: `org.nayagram.ngbrowser` · ভার্সন: `v3.12.18 NG` (`versionCode: 3578`)

---

## ১. Google Play Store প্রি-ফ্লাইট অডিট (সব শর্ত ১০০% পূরণ হয়েছে ✅)

| বিষয় | বর্তমান মান | স্ট্যাটাস |
|---|---|---|
| **Package Name (applicationId)** | `org.nayagram.ngbrowser` | ✅ ইউনিক ও বৈধ |
| **Version Name** | `v3.12.18 NG` | ✅ সেট করা হয়েছে |
| **Version Code** | `3578` | ✅ প্লে স্টোর উপযোগী |
| **Target SDK / Compile SDK** | **API 36 (Android 16)** | ✅ ২০২৬ প্লে স্টোর নীতিমালায় ১০০% পাশ |
| **Min SDK** | **API 21 (Android 5.0+)** | ✅ ৯৯.৮% অ্যান্ড্রয়েড ডিভাইস সাপোর্ট |
| **16 KB Page Alignment** | `p_align = 0x4000` + `useLegacyPackaging = false` | ✅ Android 15+/16+ বাধ্যতামূলক শর্ত পূরণ |
| **Signing Keystore** | `ngbrowser-release.jks` (RSA 2048, ২০৬৪ সাল পর্যন্ত বৈধ) | ✅ প্লে অ্যাপ সাইনিং রেডি |
| **রেন্ডারিং ইঞ্জিন** | Standalone Mozilla GeckoView 140 ESR (C++/Rust) | ✅ আসল নেটিভ ইঞ্জিন |
| **Play Store AAB ডেলিভারি সাইজ** | ~73 MB প্রতি ডিভাইসে (মোট AAB ~146 MB, ২০০ MB লিমিটের অনেক নিচে) | ✅ অপটিমাইজড |
| **Privacy Policy লিংক** | `https://github.com/NayaGramPlatform/NGBrowser/blob/MasterPro/PRIVACY_POLICY.md` | ✅ প্রস্তুত |

---

## ২. আজই Play Console-এ আপলোড করার ধাপ

1. **Google Play Console**-এ লগইন করে **Create App** ক্লিক করুন (App Name: `NG Browser`, Category: `Communication`, Free)।
2. **Store Listing**-এ `store/play_store_icon_512.png` (512×512 আইকন), স্ক্রিনশট এবং ফিচার গ্রাফিক (1024×500) আপলোড করুন।
3. **App Content → Privacy Policy**-তে এই লিংকটি দিন:  
   `https://github.com/NayaGramPlatform/NGBrowser/blob/MasterPro/PRIVACY_POLICY.md`
4. **Data Safety** ফর্মে দিন: *"Does your app collect or share any of the required user data types?"* → **No** (কারণ সব ডেটা শুধুমাত্র ইউজারের ডিভাইসেই থাকে)।
5. **Production / Closed Testing → Create new release**-এ **`app-release.aab`** ফাইলটি আপলোড করে সাবমিট করুন।
