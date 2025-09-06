# ProGuard rules for media codec handling

# Keep media codec classes
-keep class android.media.** { *; }
-keep class android.hardware.** { *; }

# Keep Google Ads SDK classes (already handling video ads)
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Keep Google Play Services classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Google API classes
-keep class com.google.api.** { *; }
-dontwarn com.google.api.**

# Keep codec2 client classes
-keep class android.hardware.media.c2.** { *; }
-keep class com.android.media.** { *; }

# Prevent obfuscation of native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep surface-related classes
-keep class android.view.Surface { *; }
-keep class android.view.SurfaceView { *; }
-keep class android.view.TextureView { *; }

# Keep Intent and Uri classes for video launching
-keep class android.content.Intent { *; }
-keep class android.net.Uri { *; }

# Spotify SDK ProGuard rules
# Keep all Spotify SDK classes and annotations
-keep class com.spotify.** { *; }
-keep class com.spotify.protocol.types.** { *; }
-keep class com.spotify.protocol.client.** { *; }
-keep class com.spotify.android.appremote.** { *; }

# Keep Spotify protocol annotations specifically
-keep @interface com.spotify.protocol.types.**
-keepattributes *Annotation*

# Suppress warnings for Spotify SDK classes
-dontwarn com.spotify.**
-dontwarn com.spotify.protocol.**

# Keep annotation classes that are causing the warnings
-keep class com.spotify.protocol.types.ImageIdentifier { *; }
-keep class com.spotify.protocol.types.Image { *; }
-keep class com.spotify.protocol.types.Track { *; }
-keep class com.spotify.protocol.types.PlayerState { *; }
-keep class com.spotify.protocol.types.ImageUri { *; }

# Keep all annotation-related attributes
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault