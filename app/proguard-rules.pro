-keepattributes *Annotation*
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.kaliki.browser.models.** { *; }
-dontwarn okhttp3.**
