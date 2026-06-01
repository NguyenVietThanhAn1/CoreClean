# CoreClean ProGuard rules
# Keep stack traces readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ──────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static **$serializer serializer(...);
}

# ── Jetpack Navigation type-safe routes ─────────────────────────────────────
-keep @kotlinx.serialization.Serializable class com.coreclean.app.presentation.navigation.** { *; }

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# ── Hilt ─────────────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }

# ── Coil ─────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Sentry ───────────────────────────────────────────────────────────────────
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
-keep class io.sentry.android.core.** { *; }

# ── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── DataStore ─────────────────────────────────────────────────────────────────
-dontwarn androidx.datastore.**
