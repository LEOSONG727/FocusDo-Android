# ── Focus Do — ProGuard Rules ───────────────────────────────────────────────

# ── Room ─────────────────────────────────────────────────────────────────────
# Keep all entity/DAO classes so Room can reflect on them
-keep class com.focusdo.app.data.** { *; }

# Room generated implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ── Kotlin Coroutines / Flow ──────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Kotlin Serialization (if added later) ────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ── ViewModel ────────────────────────────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ── ViewBinding ───────────────────────────────────────────────────────────────
-keep class com.focusdo.app.databinding.** { *; }

# ── Service ────────────────────────────────────────────────────────────────────
-keep class com.focusdo.app.service.PomodoroService { *; }

# ── General Android ──────────────────────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
