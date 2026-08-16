# Proguard rules for app layer

# Keep Native JNI classes & interfaces
-keep class com.m5dev.arcx.data.ndk.** { *; }

# Keep WorkManager workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(...);
}

# Keep Hilt generated components & viewmodels
-keep class * extends androidx.lifecycle.ViewModel
