/*
 * This file is part of SuperLyricApi.
 *
 * Copyright (C) 2025–2026 HChenX
 */
// ISuperLyricManager.aidl
package com.hchen.superlyricapi;

import com.hchen.superlyricapi.SuperLyricData;
import com.hchen.superlyricapi.ISuperLyricReceiver;

interface ISuperLyricManager {
    // 注册发行商
    void registerPublisher();

    // 解除发行商注册
    void unregisterPublisher();

    // 是否已注册为发行商
    boolean isPublisherRegistered();

    // 发布歌词数据
    void sendLyric(in SuperLyricData data);

    // 发布状态暂停
    void sendStop(in SuperLyricData data);

    // 注册接收器
    void registerReceiver(in ISuperLyricReceiver receiver);

    // 解除注册接收器
    void unregisterReceiver(in ISuperLyricReceiver receiver);

    // 此接收器是否已经被注册
    boolean isReceiverRegistered(in ISuperLyricReceiver receiver);

    // 设置是否启用系统层播放状态监听功能
    void setSystemPlayStateListenerEnabled(in boolean enabled);
}

