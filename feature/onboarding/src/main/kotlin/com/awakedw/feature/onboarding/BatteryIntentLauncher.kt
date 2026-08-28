package com.awakedw.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat

/**
 * 分机型省电白名单引导的 Intent 装配与跳转接缝。
 *
 * 设计契约（brief §Interfaces）：
 * - [bestEffortIntents] 返回「按优先级排序的尝试清单」，队尾永远是应用详情页兜底，永不抛异常；
 * - 厂商定向条目经 `resolveActivity` 探测，解析为空的条目直接剔除（没装对应管家就别跳）；
 * - ROM 清单以 [ROM_ENTRIES] 常量表维护，追加新机型在表尾插一行即可；
 * - 通知权限（POST_NOTIFICATIONS）在 onboarding 主路径一并请求，接缝收口在本类便于测试。
 */
object BatteryIntentLauncher {
    /** 单个 ROM 定向入口：厂商关键词匹配 + 候选 Intent 工厂（同厂多代系统包名可能不同，逐个尝试）。 */
    internal data class RomEntry(
        val note: String,
        val keywords: List<String>,
        val intents: (packageName: String) -> List<Intent>,
    )

    /** ROM 清单常量表（AOSP 直通与详情页兜底不在此表内）。 */
    internal val ROM_ENTRIES: List<RomEntry> =
        listOf(
            RomEntry(
                note = "小米 MIUI / HyperOS 自启动权限",
                keywords = listOf("xiaomi", "redmi"),
                intents = { pkg ->
                    listOf(
                        Intent(MIUI_HITS_PERMISSION_ACTION).apply {
                            component =
                                ComponentName(
                                    "com.miui.securitycenter",
                                    "com.miui.permcenter.autostart.AutoStartManagementActivity",
                                )
                            putExtra("extra_package_name", pkg)
                        },
                    )
                },
            ),
            RomEntry(
                note = "华为 / 荣耀 受保护应用",
                keywords = listOf("huawei", "honor"),
                intents = { pkg ->
                    listOf(
                        Intent(HUAWEI_HIACTION_PERMISSION_REQUEST).apply {
                            component =
                                ComponentName(
                                    "com.huawei.systemmanager",
                                    "com.huawei.systemmanager.optimize.process.ProtectActivity",
                                )
                            putExtra("packageName", pkg)
                        },
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.hihonor.systemmanager",
                                    "com.hihonor.systemmanager.optimize.process.ProtectActivity",
                                )
                        },
                    )
                },
            ),
            RomEntry(
                note = "OPPO / OnePlus / realme 自启动管理",
                keywords = listOf("oppo", "oneplus", "realme"),
                intents = { _ ->
                    listOf(
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.coloros.safecenter",
                                    "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                                )
                        },
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.coloros.safecenter",
                                    "com.coloros.safecenter.startupapp.StartupAppListActivity",
                                )
                        },
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.oppo.safe",
                                    "com.oppo.safe.permission.startup.StartupAppListActivity",
                                )
                        },
                    )
                },
            ),
            RomEntry(
                note = "vivo / iQOO 后台弹出与自启动管理",
                keywords = listOf("vivo", "iqoo"),
                intents = { _ ->
                    listOf(
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.vivo.permissionmanager",
                                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                                )
                        },
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.iqoo.secure",
                                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
                                )
                        },
                        Intent().apply {
                            component =
                                ComponentName(
                                    "com.iqoo.secure",
                                    "com.iqoo.secure.safeguard.PurviewTabActivity",
                                )
                        },
                    )
                },
            ),
        )

    /** 通知权限请求码（onboarding 专用，取 Task 13 的幸运数字）。 */
    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 13

    private const val MIUI_HITS_PERMISSION_ACTION = "miui.intent.action.HITS_PERMISSION"
    private const val HUAWEI_HIACTION_PERMISSION_REQUEST = "hiaction.PERMISSION_REQUEST"

    /** 返回厂商定向失败后可用的兜底 Intent（应用详情页）；永不抛异常。 */
    fun bestEffortIntents(context: Context): List<Intent> {
        val packageManager = context.packageManager
        val resolvable =
            candidateIntents(
                packageName = context.packageName,
                manufacturer = Build.MANUFACTURER,
            ).filter { intent -> packageManager.resolveActivity(intent, 0) != null }
        return resolvable + fallbackIntent(context.packageName)
    }

    /** 依序尝试清单内的每个入口：首个成功即返回 true，失败吞异常继续，全部失败返回 false。 */
    fun tryStartInOrder(
        context: Context,
        intents: List<Intent>,
    ): Boolean {
        for (intent in intents) {
            // 非 Activity 场景（如 Application 级上下文）需要 NEW_TASK 才能从外部启动。
            val attempt =
                if (context is Activity) {
                    intent
                } else {
                    Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            try {
                context.startActivity(attempt)
                return true
            } catch (error: Exception) {
                // 该入口被 ROM 拦截或不存在：静默换下一个，兜底交给清单末尾的应用详情页。
            }
        }
        return false
    }

    /** T+33 上仍需授予时的待请求清单；其余版本为空（安装时默认授予）。 */
    fun neededNotificationPermissions(context: Context): List<String> =
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            listOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }

    /** onboarding 主路径入口：经 ActivityCompat 请求通知权限（无待请求项时静默跳过）。 */
    fun requestNotificationPermission(activity: Activity) {
        val permissions = neededNotificationPermissions(activity)
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                NOTIFICATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    /** 候选顺序（纯逻辑，便于 ROM 场景测试）：AOSP 直通打头 → 表序展开命中厂商的条目。 */
    internal fun candidateIntents(
        packageName: String,
        manufacturer: String,
    ): List<Intent> {
        val lowered = manufacturer.lowercase()
        return buildList {
            add(aospWhitelistIntent(packageName))
            ROM_ENTRIES
                .filter { entry -> entry.keywords.any { keyword -> lowered.contains(keyword) } }
                .forEach { entry -> addAll(entry.intents(packageName)) }
        }
    }

    /** AOSP 直通弹窗：跳过管家 UI 直接请求忽略电池优化（需清单声明权限，由 :app 集成任务接线）。 */
    private fun aospWhitelistIntent(packageName: String): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }

    /** 应用详情页兜底：所有机型必有，永远殿后。 */
    private fun fallbackIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
}
