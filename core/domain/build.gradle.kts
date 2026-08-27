plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// 说明：业务上 :core:domain 仍是「纯 Kotlin」用例层（源码零 Android import，单测跑在纯 JVM）。
// 采用 android-library 插件仅为本模块能按 ui → domain ← data 方向消费 :core:data 的接口
// （kotlin-jvm 消费者无法解析 com.android.library 发布的 androidJvm 变体）。

android {
    namespace = "com.awakedw.core.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    // 用例层面向 core:data 的仓储接口编程，Room/DataStore 细节不出 :core:data。
    implementation(project(":core:data"))
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
