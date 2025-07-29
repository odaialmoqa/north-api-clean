# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlin Multiplatform classes
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }

# Keep Plaid SDK classes
-keep class com.plaid.** { *; }

# Keep serialization classes
-keep @kotlinx.serialization.Serializable class * {
    static **[] values();
    static ** valueOf(java.lang.String);
    *;
}

# Keep data classes
-keep class com.north.mobile.domain.** { *; }
-keep class com.north.mobile.data.** { *; }

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}