package com.awakedw.feature.onboarding

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * 省电白名单引导的 Intent 装配与逐个尝试（Robolectric）：
 * - 候选顺序契约：AOSP 直通弹窗打头 → ROM 表按厂商命名的条目依表序展开；
 * - 过滤契约：resolveActivity 为空的候选剔除，兜底（应用详情页）永远殿后；
 * - 尝试契约：逐个 startActivity，首个成功即停；失败继续、全部失败返回 false。
 */
@RunWith(RobolectricTestRunner::class)
class BatteryIntentLauncherTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val context: Context get() = app

    // region 候选表：ROM 场景顺序（纯逻辑，不依赖包管理器）

    @Test
    fun `未知厂商只含AOSP直通候选`() {
        val intents = BatteryIntentLauncher.candidateIntents("com.awakedw.dev", "robolectric")

        assertEquals(listOf(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS), intents.map { it.action })
        assertEquals("package:com.awakedw.dev", intents.single().data.toString())
    }

    @Test
    fun `小米厂商在直通弹窗后追加HITS_PERMISSION候选`() {
        val intents = BatteryIntentLauncher.candidateIntents("com.awakedw.dev", "Xiaomi")

        assertEquals(
            listOf(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "miui.intent.action.HITS_PERMISSION",
            ),
            intents.map { it.action },
        )
        assertEquals("com.awakedw.dev", intents.last().getStringExtra("extra_package_name"))
    }

    @Test
    fun `荣耀厂商追加华为hiaction与荣耀组件候选`() {
        val intents = BatteryIntentLauncher.candidateIntents("com.awakedw.dev", "HONOR")

        assertEquals(
            listOf(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "hiaction.PERMISSION_REQUEST",
                null,
            ),
            intents.map { it.action },
        )
        assertEquals(
            listOf("com.huawei.systemmanager", "com.hihonor.systemmanager"),
            intents.drop(1).map { it.component?.packageName },
        )
    }

    @Test
    fun `vivo厂商追加各自包名组件候选`() {
        val intents = BatteryIntentLauncher.candidateIntents("com.awakedw.dev", "vivo")

        assertEquals(
            listOf(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, null, null, null),
            intents.map { it.action },
        )
        assertEquals(
            listOf("com.vivo.permissionmanager", "com.iqoo.secure", "com.iqoo.secure"),
            intents.drop(1).map { it.component?.packageName },
        )
    }

    @Test
    fun `OPPO厂商候选与华为厂商候选互不混入`() {
        val oppo = BatteryIntentLauncher.candidateIntents("com.awakedw.dev", "OPPO")
        val huawei = BatteryIntentLauncher.candidateIntents("com.awakedw.dev", "HUAWEI")

        assertEquals(
            listOf("com.coloros.safecenter", "com.coloros.safecenter", "com.oppo.safe"),
            oppo.drop(1).map { it.component?.packageName },
        )
        assertTrue(huawei.drop(1).all { it.component!!.packageName.contains("systemmanager") })
        assertFalse(oppo.drop(1).any { it.component!!.packageName.contains("systemmanager") })
    }

    // endregion

    // region bestEffortIntents：resolveActivity 过滤 + 兜底永远殿后

    @Test
    fun `候选全部无法解析时仅剩兜底应用详情页`() {
        val intents = BatteryIntentLauncher.bestEffortIntents(context)

        assertEquals(1, intents.size)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intents.single().action)
        assertEquals("package:${context.packageName}", intents.single().data.toString())
    }

    @Test
    fun `可解析的候选按序保留兜底永远殿后`() {
        val candidates = BatteryIntentLauncher.candidateIntents(context.packageName, Build.MANUFACTURER)
        val packageManager = context.packageManager
        // 让全部候选都可解析：列表按候选顺序原样保留。
        candidates.forEach { intent ->
            shadowOf(packageManager).addResolveInfoForIntent(
                intent,
                ResolveInfo().apply {
                    activityInfo =
                        ActivityInfo().apply {
                            packageName = context.packageName
                            name = "ResolverActivity"
                        }
                },
            )
        }

        val intents = BatteryIntentLauncher.bestEffortIntents(context)

        // 解析成功的候选按原顺序保留（动作或组件签名一致），兜底永远殿后。
        assertEquals(candidates.map(::signature), intents.dropLast(1).map(::signature))
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intents.last().action)
    }

    /** Intent 的弱身份签名：action + 组件，足以区分候选表条目。 */
    private fun signature(intent: Intent): String = "${intent.action}|${intent.component}"

    // endregion

    // region tryStartInOrder：逐个尝试、首个成功即停

    @Test
    fun `首个入口成功即停后续不再尝试`() {
        val started = BatteryIntentLauncher.tryStartInOrder(context, listOf(Intent("test.first"), Intent("test.second")))

        assertTrue(started)
        assertEquals("test.first", shadowOf(app).nextStartedActivity.action)
    }

    @Test
    fun `首个入口抛异常则继续尝试下一个`() {
        val throwing =
            object : android.content.ContextWrapper(context) {
                override fun startActivity(intent: Intent) {
                    if (intent.action == "test.blocked") throw SecurityException("ROM 拦截")
                    super.startActivity(intent)
                }
            }

        val started =
            BatteryIntentLauncher.tryStartInOrder(
                throwing,
                listOf(Intent("test.blocked"), Intent("test.ok")),
            )

        assertTrue(started)
        assertEquals("test.ok", shadowOf(app).nextStartedActivity.action)
    }

    @Test
    fun `全部入口失败返回false`() {
        val throwing =
            object : android.content.ContextWrapper(context) {
                override fun startActivity(intent: Intent) {
                    throw SecurityException("全部被 ROM 拦截")
                }
            }

        val started = BatteryIntentLauncher.tryStartInOrder(throwing, listOf(Intent("test.a"), Intent("test.b")))

        assertFalse(started)
    }

    // endregion

    // region 通知权限（onboarding 主路径一并请求 POST_NOTIFICATIONS）

    @Test
    @Config(sdk = [33])
    fun `T33未授权时通知权限进入待请求清单`() {
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            BatteryIntentLauncher.neededNotificationPermissions(context),
        )
    }

    @Test
    @Config(sdk = [33])
    fun `T33已授权后无需再请求`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertEquals(emptyList<String>(), BatteryIntentLauncher.neededNotificationPermissions(context))
    }

    @Test
    @Config(sdk = [28])
    fun `T33以下不走运行时通知权限`() {
        assertEquals(emptyList<String>(), BatteryIntentLauncher.neededNotificationPermissions(context))
    }

    @Test
    @Config(sdk = [33])
    fun `requestNotificationPermission经ActivityCompat发起请求`() {
        val activity = Robolectric.setupActivity(Activity::class.java)

        BatteryIntentLauncher.requestNotificationPermission(activity)

        val request = shadowOf(activity).lastRequestedPermission
        assertEquals(BatteryIntentLauncher.NOTIFICATION_PERMISSION_REQUEST_CODE, request.requestCode)
        assertTrue(
            request.requestedPermissions.contentEquals(arrayOf(Manifest.permission.POST_NOTIFICATIONS)),
        )
    }

    // endregion
}
