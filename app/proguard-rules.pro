# Yandex MapKit
-keep class com.yandex.** { *; }
-dontwarn com.yandex.**

# Retrofit + OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# DTO-классы
-keep class com.territorywars.data.remote.dto.** { *; }
-keep class com.territorywars.domain.model.** { *; }

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
