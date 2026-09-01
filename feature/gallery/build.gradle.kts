plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.awakedw.feature.gallery"
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
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:domain"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)

    implementation(libs.lifecycle.viewmodel.compose)
    // GalleryViewModel 的 StateFlow/combine 管道需要 coroutines 类型（:core:domain 以 implementation 引入，不外泄）。
    implementation(libs.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // @HiltViewModel 在导航目的地内的标准取用（NavBackStackEntry 作用域）。
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    // GalleryViewModelTest 的 uiState 状态序列断言（Turbine）。
    testImplementation(libs.turbine)
}
