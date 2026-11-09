package com.example.seteasecloudmusic.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 冷启动 Baseline Profile 生成器（P0 核心）。
 *
 * simpleperf 实测：冷启动约一半 CPU 烧在 ART 验 dex / 链接类 / 解释执行
 * （78MB dex、21 个文件、R8 关闭、无 profile）。
 * 这条 journey 把启动路径的类/方法写入 baseline-prof，经 profileinstaller
 * 在安装后转码为 AOT，verify + interpret 开销从每次冷启动移到安装时。
 *
 * 生成：在模拟器/真机上执行
 *   ./gradlew :baselineprofile:generateBaselineProfile
 * 产物自动落到 app/src/main/baselineProfiles/baseline-prof.txt。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(
        packageName = "com.luna.music",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        // 等首页首帧 + 异步 feed 回填（模拟器上后端可能不可达，空态路径同样要收录）
        device.waitForIdle()
        Thread.sleep(3000L)
        device.waitForIdle()
    }
}
