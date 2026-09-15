plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.awakedw.core.data"
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

    // Room schema 导出：首改表（migration）前必须先有 v1 基线 JSON 入库。
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    testOptions {
        unitTests {
            // Room.inMemoryDatabaseBuilder 需要真实 Context，JVM 单测借助 Robolectric 提供。
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
    // 依赖倒置：本模块为 domain 契约（com.awakedw.core.domain.contracts）提供实现。
    implementation(project(":core:domain"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
}
