# kotlinx.serialization reaches the generated serializers by reflection only, so
# R8 would strip them and the first API call would fail.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keep,includedescriptorclasses class org.sonorus.tv.**$$serializer { *; }
-keepclassmembers class org.sonorus.tv.** {
    *** Companion;
}
-keepclasseswithmembers class org.sonorus.tv.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Field names are the JSON keys.
-keep @kotlinx.serialization.Serializable class org.sonorus.tv.** { *; }

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
