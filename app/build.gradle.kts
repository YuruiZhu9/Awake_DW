plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.awakedw.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.awakedw.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.3.0-alpha5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("release") {
            // 发布签名只经环境变量注入，不落库；未注入时保持 null，由 buildType 回退 debug 签名。
            val storeFilePath = System.getenv("AWAKE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("AWAKE_STORE_PASSWORD")
                keyAlias = System.getenv("AWAKE_KEY_ALIAS")
                keyPassword = System.getenv("AWAKE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // 与 release 包共存便于对照观察（同机可同时装两个变体）。
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig =
                if (signingConfigs.getByName("release").storeFile != null) {
                    signingConfigs.getByName("release")
                } else {
                    // 个人分发阶段：无发布签名时回退 debug 签名，保证随时可出未发布包。
                    signingConfigs.getByName("debug")
                }
        }
    }

    testOptions {
        unitTests {
            // Robolectric 冒烟测试需要应用资源与真实 Application 生命周期。
            isIncludeAndroidResources = true
            all { test ->
                // 本机网络无法直连 Maven Central，Robolectric 取 android-all 构件时改走阿里云镜像。
                test.systemProperty("robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/central")
            }
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // 底部栏三枚 Rounded 图标（WaterDrop/BarChart）在扩展图标包内；版本由 compose-bom 统一管理。
    implementation(libs.compose.material.icons.extended)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.splashscreen)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    // 声音门面（AwakeSoundPlayer → SoundPoolPlayer 的 Hilt 绑定随 :core:sound 聚合进主图）。
    implementation(project(":core:sound"))
    implementation(project(":core:notification"))
    implementation(project(":core:designsystem"))

    implementation(project(":feature:home"))
    implementation(project(":feature:stats"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // Robolectric 路线启动真实 MainActivity（compose 规则驱动首帧）。
    testImplementation(libs.compose.ui.test)
    // Hilt 官方 Robolectric 路线：测试组件随 kspTest 重新生成（test 源集的注入点不进主图聚合）。
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)
}
