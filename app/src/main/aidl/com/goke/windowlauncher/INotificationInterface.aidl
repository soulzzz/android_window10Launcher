// INotificationInterface.aidl
package com.goke.windowlauncher;

// Declare any non-default types here with import statements

interface INotificationInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
      void GetNoficatinoList(inout List<StatusBarNotification> notificationLists);
}
