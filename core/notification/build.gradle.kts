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
                // Robolectric 构件走本地离线目录（首次用 tools/sync-robolectric-jars.ps1 同步），绕开运行时下载与锁文件竞争。
                test.systemProperty("robolectric.dependency.dir", rootProject.file(".robolectric/offline").absolutePath)
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
