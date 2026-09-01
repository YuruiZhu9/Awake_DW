// 网络镜像说明：本机网络无法直连 dl.google.com / maven.google.com（连接超时），
// 因此在官方源之前前置阿里云镜像。镜像内容与官方仓库完全一致（相同构件、相同版本），
// 仅用于加速与可达性；官方源仍列于其后作为兜底。
// 在可直连 Google 仓库的网络环境中，可直接删除下方三个 maven("https://maven.aliyun.com/...") 条目。
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        google()
        mavenCentral()
    }
}

rootProject.name = "Awake_DW"
include(":app")
include(":core:model", ":core:common", ":core:domain", ":core:data", ":core:notification", ":core:designsystem")
include(":feature:home", ":feature:stats", ":feature:settings", ":feature:onboarding", ":feature:gallery")
