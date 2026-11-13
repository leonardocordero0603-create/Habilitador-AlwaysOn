# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Preserve SourceFile and line numbers for readable deobfuscated crash reports in Google Play Console.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep AndroidViewModel(Application) constructors instantiated via reflection by ViewModelProvider.
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}

# Suppress R8 notes/warnings for guarded OEM reflection (Samsung One UI SystemProperties check).
-dontnote android.os.SystemProperties
-dontwarn android.os.SystemProperties
