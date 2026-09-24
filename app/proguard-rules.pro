# ProGuard rules for ArrowFlow

# Keep Gson models
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class uz.kmax.arrowflow.model.** { *; }

# Keep DataStore
-dontwarn androidx.datastore.**

# Keep BaseLibrary components
-keep class uz.kmax.base.** { *; }
