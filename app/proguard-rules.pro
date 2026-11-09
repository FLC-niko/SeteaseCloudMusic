# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# P0：release 开 R8（isMinifyEnabled=true）。以下 keep 保证混淆后不崩：
# Gson 反射解析网络 DTO（泛型签名不能丢）
-keepattributes Signature, InnerClasses, EnclosingMethod
# 网络/数据模型：字段名参与 Gson 解析，整体保留
-keep class com.example.seteasecloudmusic.feature.**.data.** { *; }
-keep class com.example.seteasecloudmusic.feature.**.domain.model.** { *; }
# Gson 本地缓存模型（SharedPreferences 里的旧 JSON 用的是原始字段名，
# R8 改名后读旧缓存会全字段 null → 空指针。必须保名）：
# RadarPlaylistData / FavoriteRecommendSectionData
-keep class com.example.seteasecloudmusic.feature.**.domain.repository.** { *; }
# SavedPlaybackState（播放队列恢复）
-keep class com.example.seteasecloudmusic.core.cache.** { *; }
-keep class com.example.seteasecloudmusic.core.model.** { *; }
# Hilt 入口与 Module（其余由 hilt 自带 consumer-rules 覆盖）
-keep class com.example.seteasecloudmusic.core.SeteaseCloudMusicApp { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp { *; }
-keep class * extends androidx.activity.ComponentActivity { *; }
# 枚举/序列化/Parcelable/R8 默认易误删项
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
-keepclassmembers class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
-keepclassmembers class * implements java.io.Serializable { static final long serialVersionUID; private static final java.io.ObjectStreamField[] serialPersistentFields; private void writeObject(java.io.ObjectOutputStream); private void readObject(java.io.ObjectInputStream); java.lang.Object writeReplace(); java.lang.Object readResolve(); }
-keepclasseswithmembernames class * { native <methods>; }
# Retrofit Service 接口方法名参与动态代理，保留签名
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }