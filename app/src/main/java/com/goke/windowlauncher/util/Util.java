package com.goke.windowlauncher.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.goke.windowlauncher.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Util {
    private static final String TAG = "Util";
    private static List<ResolveInfo> applist = new ArrayList<>();
    public static List<ResolveInfo> getAllApps(Context context){
        if(context ==null){
            return  new ArrayList<>();
        }
        PackageManager packageManager = context.getPackageManager();
        Intent it = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> tmpList = packageManager.queryIntentActivities(it,0);
        if(tmpList !=null){
            Intent leanbackIntent = new Intent(Intent.ACTION_MAIN,null).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
            tmpList.addAll(packageManager.queryIntentActivities(leanbackIntent,0));
            tmpList = removeRepeat(tmpList);
            ArrayMap<String,Boolean> map = filterAppParse(context);
            for(int i=0;i<tmpList.size();){
                ResolveInfo info = tmpList.get(i);
                String pkg = info.activityInfo.packageName;
                if(map.get(pkg)!=null){
                    tmpList.remove(i);
                }else{
                    i++;
                }
            }

            applist.clear();
            applist.addAll(tmpList);
        }
        Log.d(TAG, "getAllApps: "+tmpList.size());
        return tmpList;
    }
    public static ArrayMap<String,ResolveInfo> getAppsMap(){
        ArrayMap<String,ResolveInfo> map =new ArrayMap<>();
        for(int i=0;i<applist.size();i++){
            Log.d(TAG, "getAppsMap: "+applist.get(i).activityInfo.packageName);
            map.put(applist.get(i).activityInfo.packageName,applist.get(i));
        }
        return map;
    }

    private static ArrayMap<String, Boolean> filterAppParse(Context context) {
        ArrayMap<String,Boolean> filterList = new ArrayMap<String, Boolean>();
        if(context !=null){
            InputStream fis = context.getResources().openRawResource(R.raw.filter_apps);
            if(fis!=null){
                try {
                    XmlPullParserFactory factory  = XmlPullParserFactory.newInstance();
                    XmlPullParser xmlPullParser = factory.newPullParser();
                    xmlPullParser.setInput(fis,"UTF-8");
                    int eventType =xmlPullParser.getEventType();
                    String name = "";
                    while(eventType !=XmlPullParser.END_DOCUMENT){
                        if(eventType == XmlPullParser.START_TAG && xmlPullParser.getName().equalsIgnoreCase("PackageName")){
                            name = xmlPullParser.nextText();
                            if(!TextUtils.isEmpty(name)){
                                filterList.put(name.trim(),true);
                            }
                        }
                        eventType = xmlPullParser.next();
                    }
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    if(fis!=null){
                        try{
                            fis.close();
                        }catch (IOException e){
                            Log.e(TAG, "filterAppParse: "+ e.getMessage() );
                        }
                    }
                }
            }
        }
        return filterList;
    }

    private static List<ResolveInfo> removeRepeat(List<ResolveInfo> tmpList) {
        if(tmpList ==null){
            return tmpList;
        }
        for(int i=0;i< tmpList.size()-1;i++){
            for(int j = tmpList.size()-1;j>i;j--){
                if(tmpList.get(j).activityInfo.packageName.equals(tmpList.get(i).activityInfo.packageName)){
                    tmpList.remove(j);
                }
            }
        }
        return tmpList;
    }
}
