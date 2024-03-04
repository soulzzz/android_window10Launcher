package com.goke.windowlauncher;



import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;


import com.goke.windowlauncher.adapter.AppAdapter;
import com.goke.windowlauncher.service.DesktopService;
import com.goke.windowlauncher.util.Util;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG ="MainActivity";
    private ImageView imageView;
    private RecyclerView recyclerView;
    private View rootView;
    private List<ResolveInfo> appList;
    private int rootX,rootY,rootW,rootH;
    private float clickX,clickY;
    private AppAdapter adapter;
    private PopupMenu popupMenu;
    private Intent startServiceIntent;
    private IMyAidlInterface iBinder;


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: ");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        WindowManager.LayoutParams lp = getWindow().getAttributes();

        lp.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(lp);
        setContentView(R.layout.activity_main);
        //省略了动态申请文件权限的代码 可以在设置里手动打开 内网系统应用无需动态申请默认有
//        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
//
//        Drawable wallpaperDrawable = wallpaperManager.getDrawable();
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            getWindow().getDecorView().setBackground(wallpaperDrawable);
//        } else {
//            getWindow().getDecorView().setBackgroundDrawable(wallpaperDrawable);
//        }

        initData();
        initView();
        startServiceIntent =new Intent(this, DesktopService.class);
        Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        if(!Settings.canDrawOverlays(this)){
            startActivityForResult(permissionIntent,1001);
        }else{
            startService(startServiceIntent);
            bindService(startServiceIntent,serviceConnection,BIND_AUTO_CREATE);
        }


        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        registerReceiver(broadcastReceiver, intentFilter);
    }
    //监听壁纸变换
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals( Intent.ACTION_WALLPAPER_CHANGED )){
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(MainActivity.this);

                Drawable wallpaperDrawable = wallpaperManager.getDrawable();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getWindow().getDecorView().setBackground(wallpaperDrawable);
                } else {
                    getWindow().getDecorView().setBackgroundDrawable(wallpaperDrawable);
                }

            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if(iBinder!=null){
            try {
                iBinder.UpdateCurrentAppStatus(null);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==1001) {
            if(requestCode == PackageManager.PERMISSION_GRANTED){
                startService(startServiceIntent);
                bindService(startServiceIntent,serviceConnection,BIND_AUTO_CREATE);
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iBinder = IMyAidlInterface.Stub.asInterface(service);

            try {
                iBinder.SendAppList(appList);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void initData() {
        appList = new ArrayList<>();
        MyAsyncTask myAsyncTask = new MyAsyncTask(this);
        myAsyncTask.execute();
        adapter = new AppAdapter(appList,getPackageManager(),this,AppAdapter.DESKTOP_ADAPTER);
        adapter.setAppAdapterListener(new AppAdapter.AppAdapterListener() {
            @Override
            public void onAppClick(String pkgName) {
                if(iBinder!=null){
                    try {
                        iBinder.UpdateCurrentAppStatus(pkgName);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: "+event.getX()+" "+event.getY());
        return super.onTouchEvent(event);
    }

    private void initView() { ;
        rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                rootView.getLocationOnScreen(location);
                rootX = location[0];
                rootY = location[1];
                rootW = rootView.getWidth();
                rootH = rootView.getHeight();
                Log.d(TAG, "onGlobalLayout: "+rootX+" "+rootY +" "+rootW+" "+rootH);
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        popupMenu = new PopupMenu(this,rootView,Gravity.START);
        popupMenu.getMenuInflater().inflate(R.menu.desktop_menu,popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.sortByLength:
                        appList.sort(new Comparator<ResolveInfo>() {
                            int value = 0;
                            @Override
                            public int compare(ResolveInfo o1, ResolveInfo o2) {
                                try {
                                   File file1 = new File(getPackageManager().getApplicationInfo(o1.activityInfo.packageName,0).sourceDir) ;
                                    File file2 = new File(getPackageManager().getApplicationInfo(o2.activityInfo.packageName,0).sourceDir) ;
                                    value = (int) (file1.length() -file2.length());
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                return value;
                            }
                        });
                        adapter.notifyDataSetChanged();
                        break;
                    case R.id.sortByName:
                        Collator collator = Collator.getInstance();
                        Collections.sort(appList, new Comparator<ResolveInfo>() {
                            PackageManager packageManager = getPackageManager();
                            @Override
                            public int compare(ResolveInfo o1, ResolveInfo o2) {
                                return collator.compare(packageManager.getApplicationLabel( o1.activityInfo.applicationInfo),packageManager.getApplicationLabel( o2.activityInfo.applicationInfo));

                            }
                        });
                        adapter.notifyDataSetChanged();

                        break;
                    case R.id.sortByData:
                        appList.sort(new Comparator<ResolveInfo>() {
                            int value = 0;
                            @Override
                            public int compare(ResolveInfo o1, ResolveInfo o2) {
                                try {
                                    File file1 = new File(getPackageManager().getApplicationInfo(o1.activityInfo.packageName,0).sourceDir) ;
                                    File file2 = new File(getPackageManager().getApplicationInfo(o2.activityInfo.packageName,0).sourceDir) ;
                                    value = (int) (file1.lastModified() -file2.lastModified());
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                return value;
                            }
                        });
                        adapter.notifyDataSetChanged();
                        break;
                    case R.id.smallIcon:
                        adapter.setType(AppAdapter.SMALL_ICON);
                        recyclerView.setAdapter(adapter);
                        break;
                    case R.id.mediumIcon:
                        adapter.setType(AppAdapter.MEDIUM_ICON);
                        recyclerView.setAdapter(adapter);
                        break;
                    case R.id.bigIcon:
                        adapter.setType(AppAdapter.BIG_ICON);
                        recyclerView.setAdapter(adapter);
                        break;
                }

                return false;
            }
        });


        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);
        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager(this);
        flexboxLayoutManager.setFlexDirection(FlexDirection.COLUMN);
        recyclerView.setLayoutManager(flexboxLayoutManager);

    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY || event.getButtonState() == MotionEvent.BUTTON_BACK) {
            // 获取点击位置的坐标
            clickX = (int) event.getX();
            clickY = (int) event.getY();
            Log.d(TAG, "onGenericMotion: "+clickX+"+"+clickY+" "+event.getButtonState());
            // 显示 Toast
            showMenu((int)clickX,(int)clickY);
            return true; // 表示已处理事件
        }
        return super.onGenericMotionEvent(event);
    }

    public void showMenu(int xOffset, int yOffset){
        try{
            Field mPopup = popupMenu.getClass().getDeclaredField("mPopup");
            mPopup.setAccessible(true);
            Object menuPopupHelper = mPopup.get(popupMenu);

// 获取show方法
            Method showMethod = menuPopupHelper.getClass().getMethod("show", int.class, int.class);

            showMethod.invoke(menuPopupHelper,xOffset,yOffset-(rootH+rootY));
            Log.d(TAG, "showMenu: "+xOffset+" "+ yOffset );
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }finally {
            //出错时调用普通show方法。未出错时此方法也不会影响正常显示
            popupMenu.show();
        }

    }
    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
        unbindService(serviceConnection);
        if(broadcastReceiver!=null){
            unregisterReceiver(broadcastReceiver);
        }
    }
    public class MyAsyncTask extends AsyncTask<Void, Void, List<ResolveInfo>> {
        private Context mContext;
        private List<ResolveInfo> list;
        public MyAsyncTask(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        protected void onPreExecute() {
            // 在执行任务之前执行，通常用于初始化操作
        }

        @Override
        protected  List<ResolveInfo> doInBackground(Void... params) {
            list = Util.getAllApps(mContext);
            int size = list.size();
            while(size<=1 ){
                list = Util.getAllApps(mContext);
                size = list.size();
            }
            // 在后台执行耗时操作，不可在该方法中更新 UI
            // 返回结果会传递给 onPostExecute() 方法
            return  list;
        }

        @Override
        protected void onPostExecute( List<ResolveInfo> result) {
            appList.addAll(result);
            adapter.notifyDataSetChanged();
            // 在任务执行完成后执行，可以更新 UI
            // 参数 result 是 doInBackground() 方法的返回结果
        }
    }
}
