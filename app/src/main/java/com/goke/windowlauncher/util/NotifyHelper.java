package com.goke.windowlauncher.util;

import android.service.notification.StatusBarNotification;

public class NotifyHelper {

    private NotifyInterface notifyInterface;

    private static NotifyHelper instance;

    public void setNotifyInterface(NotifyInterface notifyInterface) {
        this.notifyInterface = notifyInterface;
    }
    public static NotifyHelper getInstance() {
        if (instance == null) {
            instance = new NotifyHelper();
        }
        return instance;
    }
    public void onReceive(StatusBarNotification sbn) {
        if(notifyInterface != null) {
            notifyInterface.onReceiveMessage(sbn);
        }
    }
    public void onRemoved(StatusBarNotification sbn) {
        if (notifyInterface != null) {
            notifyInterface.onRemovedMessage(sbn);
        }
    }

}
