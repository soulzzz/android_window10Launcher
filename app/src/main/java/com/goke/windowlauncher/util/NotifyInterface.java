package com.goke.windowlauncher.util;

import android.service.notification.StatusBarNotification;

public interface NotifyInterface {

    /**
     * 接收到通知栏消息
     * @param sbn
     */
    void onReceiveMessage(StatusBarNotification sbn);

    /**
     * 移除掉通知栏消息
     * @param sbn
     */
    void onRemovedMessage(StatusBarNotification sbn);
}
