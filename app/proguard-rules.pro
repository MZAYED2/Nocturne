# Nocturne Phone App Proguard Rules

# Keep data models used with JSON serialization
-keep class com.mohamedzayed.nocturne.model.** { *; }
-keep class com.mohamedzayed.nocturne.data.** { *; }
-keep class com.mohamedzayed.nocturne.wear.ConnectedBridge { *; }
-keep class com.mohamedzayed.nocturne.wear.WatchSlot { *; }

# Google Play Services Wearable
-keep class com.google.android.gms.wearable.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class com.mohamedzayed.nocturne.rotation.WatchFaceRotationWorker { *; }
