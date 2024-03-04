package com.goke.windowlauncher.service;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.goke.windowlauncher.util.NotifyHelper;

public class MyNotificationListenerService extends NotificationListenerService {
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // 获取通知内容;
        NotifyHelper.getInstance().onReceive(sbn);
        Notification notification = sbn.getNotification();
        if (notification != null) {
            // 通知标题
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            // 通知内容
            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            // 通知时间戳
            long timestamp = sbn.getPostTime();

            // 在这里处理通知内容
            // ...

            Log.d("NotificationListener", "Title: " + title);
            Log.d("NotificationListener", "Text: " + text);
            Log.d("NotificationListener", "Timestamp: " + timestamp);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // 通知被移除时的回调
        NotifyHelper.getInstance().onRemoved(sbn);
    }

}
