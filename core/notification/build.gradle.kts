plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.awakedw.core.notification"
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
            // 接收器 @AndroidEntryPoint 注入与通知/闹钟 shadow 需要真实资源与 framework 行为。
            isIncludeAndroidResources = true
            all { test ->
                // 本机网络无法直连 Maven Central，Robolectric 取 android-all 构件时改走阿里云镜像。
                test.systemProperty("robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/central")
            }
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // Hilt 官方 Robolectric 路线：测试组件随 kspTest 重新生成，接收器注入走测试图中的假仓储绑定。
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)
}
