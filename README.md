# Awake_DW

一个用于提醒喝水的应用。

## 技术

Kotlin · Jetpack Compose · Gradle 多模块 · Hilt

## 构建

```bash
./gradlew :app:assembleDebug
```

发布构建：

```bash
./gradlew :app:assembleRelease
```

开启混淆（R8）。发布签名经环境变量 `AWAKE_STORE_FILE` / `AWAKE_STORE_PASSWORD` / `AWAKE_KEY_ALIAS` / `AWAKE_KEY_PASSWORD` 注入；未注入时回退 debug 签名出未发布包。产物位于 `app/build/outputs/apk/release/`，对外发布的副本归档至 `dist/`。

## 文档

设计与开发约定见仓库内部：

- [`rules.md`](rules.md) — 开发规则
- `docs/superpowers/specs/` — 设计文档
