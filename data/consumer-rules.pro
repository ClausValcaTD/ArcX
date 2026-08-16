# Consumer rules for data layer

# Keep native JNI bindings
-keep class com.m5dev.arcx.data.ndk.** { *; }

# Keep Room database & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface *

# Keep DataStore models
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
}
