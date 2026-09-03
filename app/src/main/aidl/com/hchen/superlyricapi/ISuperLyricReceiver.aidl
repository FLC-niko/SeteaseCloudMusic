/*
 * This file is part of SuperLyricApi.
 *
 * Copyright (C) 2025–2026 HChenX
 */
// ISuperLyricReceiver.aidl
package com.hchen.superlyricapi;

import com.hchen.superlyricapi.SuperLyricData;

interface ISuperLyricReceiver {
    // 接收数据时调用
    void onLyric(in String publisher, in SuperLyricData data);

    // 播放暂停时调用
    void onStop(in String publisher, in SuperLyricData data);
}

