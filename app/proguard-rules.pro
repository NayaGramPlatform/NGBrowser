# NG Browser — ProGuard / R8 Rules
# Keep Mozilla GeckoView native JNI classes & delegates
-keep class org.mozilla.** { *; }
-dontwarn org.mozilla.**

# Keep NG Browser application, activities, and SQLite helpers
-keep class org.nayagram.ngbrowser.** { *; }

# Keep AndroidX & Material components
-keep class androidx.** { *; }
-dontwarn androidx.**
-dontwarn com.google.android.material.**
