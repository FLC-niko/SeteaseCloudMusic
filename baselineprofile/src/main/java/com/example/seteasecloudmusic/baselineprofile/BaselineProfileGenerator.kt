package com.example.seteasecloudmusic.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷启动 Baseline Profile 生成器。
 *
 * 通过录制冷启动路径上的类加载与热点方法生成 baseline-prof，
 * 安装时由 profileinstaller 预编译为 AOT，以分摊冷启动阶段的 dex 校验与解释执行开销。
 *
 * 生成命令（需连接真机或模拟器）：
 * `./gradlew :baselineprofile:generateBaselineProfile`
 * 产物输出至 `app/src/main/baselineProfiles/baseline-prof.txt`。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = "com.setease.cloudmusic",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        // 等待首页首帧及异步数据回填（后端不可达时同样覆盖空态路径）
        device.waitForIdle()
        // 异步加载采样等待时长（ms）
        Thread.sleep(3000L)
        device.waitForIdle()
    }
}
