# ---------------------------------------------------------------------------
# Taskora Home — ProGuard / R8 rules
# ---------------------------------------------------------------------------

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization -------------------------------------------------
# Keep the generated serializers and annotations so JSON (de)serialization
# survives R8. This is essential because all local data is stored as JSON.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep every serializable model in the app package and its nested classes.
-keep,includedescriptorclasses class com.taskora.home.**$$serializer { *; }
-keepclassmembers class com.taskora.home.** {
    *** Companion;
}
-keepclasseswithmembers class com.taskora.home.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep enum entries used by serialization by name.
-keepclassmembers enum com.taskora.home.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Compose ---------------------------------------------------------------
# The Compose compiler + AndroidX ship consumer rules; nothing extra needed
# here for a standard app, but keep composable metadata to be safe.
-keep class androidx.compose.runtime.** { *; }

# --- Coroutines ------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
