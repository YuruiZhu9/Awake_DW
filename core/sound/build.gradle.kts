plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.awakedw.core.sound"
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

    testOptions {
        unitTests {
            // 后续 SoundPoolPlayer 的 Robolectric 测试需要真实资源与 framework shadow 行为。
            isIncludeAndroidResources = true
            all { test ->
                // 本机网络无法直连 Maven Central，Robolectric 取 android-all 构件时改走阿里云镜像。
                test.systemProperty("robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/central")
            }
        }
    }
}

dependencies {
    // 声音开关裁决需要 Task 2 的 soundEnabled（UserPreferences 契约）。
    implementation(project(":core:domain"))
    // 契约接口的 UserSettings/ThemeChoice 等模型类型（:core:domain 以 implementation 引入，不外泄）。
    implementation(project(":core:model"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // Hilt 官方 Robolectric 路线：测试组件随 kspTest 重新生成。
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)
}
