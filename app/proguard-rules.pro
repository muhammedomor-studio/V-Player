# Preserve Media3 extension discovery and Android component metadata.
-keep class androidx.media3.** { *; }
-keep class studio.muhammedomor.vplayer.** extends androidx.room.RoomDatabase { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
