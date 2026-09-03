plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)

    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.seteasecloudmusic"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.luna.music"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "BASE_URL", "\"http://57.158.26.135:3000/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    
    packaging {
        jniLibs {
            // 解决 16KB 页面对齐警告
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // 本地 aar 依赖
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.foundation.layout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    
    // Retrofit 核心库（它会自动引入其依赖的 OkHttp 和 Okio 库）
    implementation(libs.retrofit)
    // Retrofit 的 GSON 转换器，用于 JSON 数据的序列化和反序列化
    implementation(libs.retrofit.converter.gson)
    
    // 实现协程相关依赖
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Liquid glass 实现相关库
    implementation("io.github.kyant0:backdrop:1.0.6")
    implementation("io.github.kyant0:shapes:1.2.0")

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Compose 核心依赖
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    
    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)

    implementation(libs.coil.compose)

    // Palette API for extracting dominant colors from album artwork
    implementation("androidx.palette:palette-ktx:1.0.0")

    //hilt依赖注入框架
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Hilt 与 Compose Navigation 集成（提供 hiltViewModel()）
    implementation(libs.androidx.hilt.navigation.compose)
}
