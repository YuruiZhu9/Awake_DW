plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.awakedw.feature.home"
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

    testOptions {
        unitTests {
            // compose-ui 测试走 Robolectric 路线：需要应用资源与真实组件生命周期。
            isIncludeAndroidResources = true
            all { test ->
                // 本机网络无法直连 Maven Central，Robolectric 取 android-all 构件时改走阿里云镜像。
                test.systemProperty("robolectric.dependency.repo.url", "https://maven.aliyun.com/repository/central")
                // Compose+Robolectric 组合测试的 NATIVE 渲染内存峰值大（溢出断言类曾 OOM），给足堆。
                test.maxHeapSize = "2g"
            }
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    // 打卡/达标/摸猫三触发点的声音接线（打卡→DROP_*、达标→GOAL_MELODY、摸猫→PURR）。
    implementation(project(":core:sound"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.tooling.preview)

    implementation(libs.lifecycle.viewmodel.compose)
    // HomeViewModel 的 StateFlow/去重管道需要 coroutines 类型（:core:domain 以 implementation 引入，不外泄）。
    implementation(libs.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // @HiltViewModel 在导航目的地内的标准取用（NavBackStackEntry 作用域）。
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.robolectric)
    // Robolectric compose 测试的宿主 Activity 登记在 src/test/AndroidManifest.xml（两变体共用）。
}
