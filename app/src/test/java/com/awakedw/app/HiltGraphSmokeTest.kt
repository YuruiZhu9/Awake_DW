package com.awakedw.app

import com.awakedw.core.domain.LogWaterUseCase
import com.awakedw.core.domain.ObserveHomeUseCase
import com.awakedw.core.domain.contracts.WaterRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * 全图闭环证明一：Robolectric 按 manifest 启动真实的 [AwakeApplication]（@HiltAndroidApp），
 * attach/onCreate 阶段即构造 SingletonComponent——任一绑定缺失，图创建在此抛错；
 * 编译期 Dagger 全图校验则已覆盖所有注入点的可解析性。
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = AwakeApplication::class)
class HiltAppBootSmokeTest {
    @Test
    fun `真实应用启动即完成Hilt图装配`() {
        val app = RuntimeEnvironment.getApplication() as AwakeApplication
        assertNotNull(app)
    }
}

/**
 * 全图闭环证明二：Hilt 官方 Robolectric 路线（@HiltAndroidTest），成员注入走真实组件图，
 * 取出三条关键依赖链的叶节点（契约绑定 → Room/DataStore 提供者 → 用例聚合）并断言可用。
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class HiltGraphSmokeTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var waterRepository: WaterRepository

    @Inject lateinit var logWaterUseCase: LogWaterUseCase

    @Inject lateinit var observeHomeUseCase: ObserveHomeUseCase

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `关键依赖经组件图注入可达`() {
        assertNotNull(waterRepository)
        assertNotNull(logWaterUseCase)
        assertNotNull(observeHomeUseCase)
    }
}
