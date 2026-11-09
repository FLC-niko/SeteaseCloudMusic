plugins {
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.example.seteasecloudmusic.baselineprofile"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 生成对象：app 的 benchmark 变体（R8 后的 release 同构体，保证 profile 与线上代码一致）
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
}
