// IMyAidlInterface.aidl
package com.goke.windowlauncher;

// Declare any non-default types here with import statements

interface IMyAidlInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void SendAppList(in List<ResolveInfo> appList);

    void UpdateCurrentAppStatus(in String pkgName);

}
