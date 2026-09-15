plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.awakedw.core.designsystem"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets.getByName("test").assets.srcDir("../../app/src/main/assets")

    testOptions {
        unitTests {
            // assets 装载的 Robolectric 测试需要应用资源（含 src/main/assets）。
            isIncludeAndroidResources = true
            all { test ->
                // Robolectric 构件走本地离线目录（首次用 tools/sync-robolectric-jars.ps1 同步），绕开运行时下载与锁文件竞争。
                test.systemProperty("robolectric.dependency.dir", rootProject.file(".robolectric/offline").absolutePath)
            }
        }
    }
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)

    implementation(project(":core:model"))
    implementation(project(":core:common"))

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.robolectric)
    // Robolectric compose 测试的宿主 Activity 登记在 src/test/AndroidManifest.xml。
}
