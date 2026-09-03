/*
 * This file is part of SuperLyricApi.
 *
 * Copyright (C) 2025–2026 HChenX
 */
package com.hchen.superlyricapi;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

/**
 * API 助手：连接与操作系统级 SuperLyric 服务的工具类。
 * 通过反射访问 ServiceManager.getService("super_lyric")，保证运行环境解耦与安全。
 *
 * @author 焕晨HChen
 */
public class SuperLyricHelper {
    private static final String TAG = "SuperLyricHelper";
    public static final int API_VERSION = 34; // 3.4
    private static volatile ISuperLyricManager mManager;

    private SuperLyricHelper() {
    }

    /**
     * 获取 SuperLyric 服务是否可用
     */
    public static boolean isAvailable() {
        try {
            ensureManager();
            return mManager != null;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * 获取当前 API 版本
     */
    public static int getApiVersion() {
        return API_VERSION;
    }

    /**
     * 发布歌词数据
     */
    public static void sendLyric(@NonNull SuperLyricData data) {
        try {
            ensureManager();
            ensurePublisherRegistered();

            if (mManager != null) {
                mManager.sendLyric(data);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException on sendLyric", e);
        } catch (Throwable e) {
            Log.d(TAG, "Failed to sendLyric to SuperLyric: " + e.getMessage());
        }
    }

    /**
     * 发布状态暂停
     */
    public static void sendStop(@NonNull SuperLyricData data) {
        try {
            ensureManager();
            ensurePublisherRegistered();

            if (mManager != null) {
                mManager.sendStop(data);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException on sendStop", e);
        } catch (Throwable e) {
            Log.d(TAG, "Failed to sendStop to SuperLyric: " + e.getMessage());
        }
    }

    public static void sendStop() {
        sendStop(new SuperLyricData());
    }

    /**
     * 注册为发行商
     * 发布歌词之前请务必先注册为发行商，否则将会触发异常
     */
    public static void registerPublisher() {
        try {
            ensureManager();
            if (mManager != null) {
                mManager.registerPublisher();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException on registerPublisher", e);
        } catch (Throwable e) {
            Log.d(TAG, "Failed to registerPublisher: " + e.getMessage());
        }
    }

    /**
     * 解除发行商注册
     */
    public static void unregisterPublisher() {
        try {
            ensureManager();
            if (mManager != null) {
                mManager.unregisterPublisher();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException on unregisterPublisher", e);
        } catch (Throwable e) {
            Log.d(TAG, "Failed to unregisterPublisher: " + e.getMessage());
        }
    }

    /**
     * 是否已注册为发行商
     */
    public static boolean isPublisherRegistered() {
        try {
            ensureManager();
            if (mManager != null) {
                return mManager.isPublisherRegistered();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException on isPublisherRegistered", e);
        } catch (Throwable e) {
            Log.d(TAG, "isPublisherRegistered check failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * 是否启用系统层面的播放状态监听器
     */
    public static void setSystemPlayStateListenerEnabled(boolean enabled) {
        try {
            ensureManager();
            if (mManager != null) {
                mManager.setSystemPlayStateListenerEnabled(enabled);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException on setSystemPlayStateListenerEnabled", e);
        } catch (Throwable e) {
            Log.d(TAG, "setSystemPlayStateListenerEnabled failed: " + e.getMessage());
        }
    }

    private static void ensureManager() {
        if (mManager != null) {
            return;
        }

        synchronized (SuperLyricHelper.class) {
            if (mManager != null) {
                return;
            }

            try {
                Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
                Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
                IBinder iBinder = (IBinder) getServiceMethod.invoke(null, "super_lyric");
                if (iBinder == null) {
                    throw new IllegalStateException("super_lyric system service is not running.");
                }

                mManager = ISuperLyricManager.Stub.asInterface(iBinder);
                if (mManager == null) {
                    throw new IllegalStateException("ISuperLyricManager.Stub.asInterface returned null.");
                }

                try {
                    iBinder.linkToDeath(new IBinder.DeathRecipient() {
                        @Override
                        public void binderDied() {
                            mManager = null;
                        }
                    }, 0);
                } catch (RemoteException ignore) {
                    mManager = null;
                }
            } catch (Throwable e) {
                throw new IllegalStateException("SuperLyricManager not attached: " + e.getMessage(), e);
            }
        }
    }

    private static void ensurePublisherRegistered() {
        boolean isRegistered = false;
        try {
            ensureManager();
            if (mManager != null) {
                isRegistered = mManager.isPublisherRegistered();
                if (!isRegistered) {
                    mManager.registerPublisher();
                    isRegistered = mManager.isPublisherRegistered();
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "SuperLyricManager RemoteException in ensurePublisherRegistered", e);
        } catch (Throwable ignore) {
        }

        if (!isRegistered) {
            throw new IllegalStateException("Not yet registered as a publisher.");
        }
    }
}

