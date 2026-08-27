plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// 依赖倒置（rules §四.1：ui → domain ← data）：用例与仓储契约都住在纯 JVM 的 :core:domain，
// 仓储接口在 com.awakedw.core.domain.contracts；:core:data 反向依赖本模块提供实现。
// 本模块源码零 Android import，单测跑在纯 JVM。

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.coroutines.core)
    // contracts.CopyLibrary 以 @Serializable 描述存储形态，序列化生成器在本模块启用。
    implementation(libs.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
