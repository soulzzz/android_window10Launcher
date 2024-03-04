package com.goke.windowlauncher.service;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.goke.windowlauncher.IMyAidlInterface;
import com.goke.windowlauncher.MainActivity;
import com.goke.windowlauncher.R;
import com.goke.windowlauncher.adapter.AppAdapter;
import com.goke.windowlauncher.adapter.NotificationAdapter;
import com.goke.windowlauncher.adapter.RunningAppAdapter;
import com.goke.windowlauncher.util.NotifyHelper;
import com.goke.windowlauncher.util.NotifyInterface;
import com.goke.windowlauncher.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.Destroyable;

public class DesktopService extends Service {
    private final String TAG= "DesktopService";
    private WindowManager windowManager;
    private View view;
    private DisplayMetrics dm;
    //Window
    private ImageView imageView_window;
    private View window_dialogView;
    private AlertDialog alertDialog_window;
    private RecyclerView recyclerView;
    private List<ResolveInfo> appList ;
    private AppAdapter adapter;
    //RunningAppStatus
    private List<ResolveInfo> runnningApps;
    private RecyclerView recyclerView_runningAppStatus;
    private RunningAppAdapter runningAppAdapter ;
    private ArrayMap<String,ResolveInfo> arrayMap;
    //BottomLine
    private int clickX,clickY,bottomLineW,bottomLineH,bottomLineX,bottomLineY;
    private View bottom_dialogView;
    private RelativeLayout bottom_line;
    private AlertDialog alertDialog_bottom;
    private TextView backToDeskTop;
    //StatusBar
    private int wifi_level  =0;
    private AudioManager audioManager;
    private SeekBar seekBar;
    private TextView textView_volume_value;
    private AlertDialog alertDialog_volume;
    private View volumeView;
    private TextView time;
    private ImageView imageView_volume,imageView_eth_wifi;
    private int[] wifi_image = new int[]{
            R.drawable.ic_wifi_0,R.drawable.ic_wifi_1,R.drawable.ic_wifi_2,R.drawable.ic_wifi_3,R.drawable.ic_wifi_4
    };
    private boolean isEthOn = false;

    //notification panel
    private View notifyPanel;
    private AlertDialog alertDialog_notifyPanel;
    private ImageView imageView_notify;
    private Button button_clearall,button_turnoff;
    private List<StatusBarNotification> statusBarNotificationList = new ArrayList<>();
    private NotificationAdapter notificationAdapter;


    //aidl
    @Override
    public IBinder onBind(Intent intent) {
       return  mybinder;
    }
    private IBinder mybinder = new IMyAidlInterface.Stub() {
        @Override
        public void SendAppList(List<ResolveInfo> appList) throws RemoteException {
            DesktopService.this.appList.clear();
            DesktopService.this.appList.addAll(appList);
            adapter = new AppAdapter(DesktopService.this.appList,getPackageManager(),DesktopService.this,AppAdapter.PANEL_ADAPTER);
            adapter.setAppAdapterListener(new AppAdapter.AppAdapterListener() {
                @Override
                public void onAppClick(String pkgName) {
                    if(alertDialog_window.isShowing()){
                        alertDialog_window.dismiss();
                    }
                    updateCurrentAppStatus(pkgName);
                }
            });
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(DesktopService.this));
        }

        @Override
        public void UpdateCurrentAppStatus(String pkgName) throws RemoteException {
            updateCurrentAppStatus(pkgName);

        }
    };
    private void updateCurrentAppStatus(String pkgName){
        if(arrayMap ==null) arrayMap = Util.getAppsMap();

        if(pkgName !=null){

            Log.d(TAG, "updateCurrentAppStatus: add "+pkgName);
            runnningApps.add(arrayMap.get(pkgName));
            Log.d(TAG, "updateCurrentAppStatus: "+runnningApps.size());
        }else{
            runnningApps.clear();

            Log.d(TAG, "updateCurrentAppStatus: "+runnningApps.size());
        }
//        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
//        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
//            String pkgname = processInfo.pkgList[0];
//            Log.d(TAG, "updateCurrentAppStatus: "+pkgname);
//            if(arrayMap.get(pkgname) !=null){
//                runnningApps.add(arrayMap.get(pkgname));
//            }
//        }
        runningAppAdapter.notifyDataSetChanged();
        Log.d(TAG, "updateCurrentAppStatus:  "+runningAppAdapter.getItemCount());



    }
    private BroadcastReceiver netReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: "+action);
            if(action ==null){
                return;
            }
            if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if(networkInfo !=null && networkInfo.getType() ==ConnectivityManager.TYPE_ETHERNET && networkInfo.isConnected()){
                    isEthOn = true;
                    imageView_eth_wifi.setImageResource(R.drawable.ic_eth_on);

                    //eth connected
                }else{
                    isEthOn =false;
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                    NetworkInfo networkInfo2 = connectivityManager.getActiveNetworkInfo();

                    boolean isWifiConnected = networkInfo2 != null && networkInfo2.isConnected() && networkInfo2.getType() == ConnectivityManager.TYPE_WIFI;

                    if (isWifiConnected) {
                        // WiFi 已连接
                        imageView_eth_wifi.setImageResource(wifi_image[wifi_level]);
                    } else {
                        // WiFi 未连接
                        imageView_eth_wifi.setImageResource(R.drawable.ic_eth_off);
                    }

                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION ) ) {
                if(!(intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO) instanceof NetworkInfo)){
                    return;
                }
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(networkInfo ==null){
                    return;
                }
                if(!networkInfo.isConnected()){
                    //wifi off
                    if(!isEthOn){
                        imageView_eth_wifi.setImageResource(R.drawable.ic_wifi_off);
                    }
                }else{
                    int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,0);
                    wifi_level = WifiManager.calculateSignalLevel(rssi,5);
                    //1-5
                    if(!isEthOn){
                        imageView_eth_wifi.setImageResource(wifi_image[wifi_level]);
                    }

                }
            }else if (action.equals(WifiManager.RSSI_CHANGED_ACTION) &&!isEthOn){
                int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI,0);
                wifi_level = WifiManager.calculateSignalLevel(rssi,5);
                //1-5
                if(!isEthOn){
                    imageView_eth_wifi.setImageResource(wifi_image[wifi_level]);
                }

            }
        }
    };
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    @Override
    public void onCreate() {
        super.onCreate();
        appList = new ArrayList<>();
        //bindService
        NotifyHelper.getInstance().setNotifyInterface(new NotifyInterface() {
            @Override
            public void onReceiveMessage(StatusBarNotification sbn) {
                statusBarNotificationList.add(sbn);
            }

            @Override
            public void onRemovedMessage(StatusBarNotification sbn) {
                statusBarNotificationList.remove(sbn);
            }
        });
        Intent it = new Intent(this,MyNotificationListenerService.class);
        bindService(it,serviceConnection,BIND_AUTO_CREATE);

        dm = getResources().getDisplayMetrics();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_desktop, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY; //内网中为TYPE_NAVIGATION_VAR
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.BOTTOM;
        params.y = 0;
        windowManager.addView(view, params);
        initViewAndData();
        registerBroadcastRecevier();
    }

    private void registerBroadcastRecevier(){
        IntentFilter intentFilter =new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(netReceiver,intentFilter);
    }
    private void initViewAndData() {
        initWindowPanel();
        initRunningAppStatus();
        initBottomLine();
        initStatusBar();
        initNotificationPanel();
        Log.d(TAG, "initView: ");
    }

    private void initRunningAppStatus() {
        recyclerView_runningAppStatus = view.findViewById(R.id.recyclerView_runningApps);
        runnningApps = new ArrayList<>();
        runningAppAdapter = new RunningAppAdapter(runnningApps,getPackageManager(),this);
        recyclerView_runningAppStatus.setAdapter(runningAppAdapter);
        recyclerView_runningAppStatus.setLayoutManager(new LinearLayoutManager(this,RecyclerView.HORIZONTAL,false));


    }

    private void initNotificationPanel() {
        //notifyPanel
//        NotificationManager notificationManager = getSystemService(NotificationManager.class);
//        NotificationChannel channel = new NotificationChannel("1234", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
//        notificationManager.createNotificationChannel(channel);
//
//        Notification notification = new NotificationCompat.Builder(this, "1234")
//                .setContentTitle("Notification Title")
//                .setContentText("Notification Content")
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .build();
//        notificationManager.notify(1, notification);


        notificationAdapter = new NotificationAdapter(statusBarNotificationList,getPackageManager());
        notifyPanel = LayoutInflater.from(DesktopService.this).inflate(R.layout.dialog_layout_notify_panel,null);
        button_clearall = notifyPanel.findViewById(R.id.button_clear);
        button_clearall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                statusBarNotificationList.clear();
                notificationAdapter.notifyDataSetChanged();
                //adapter clear list
            }
        });
        button_turnoff = notifyPanel.findViewById(R.id.button_turnoff);
        button_turnoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(alertDialog_notifyPanel!=null && alertDialog_notifyPanel.isShowing()){
                    alertDialog_notifyPanel.dismiss();
                }
            }
        });
        alertDialog_notifyPanel = new  AlertDialog.Builder(DesktopService.this).setView(notifyPanel).create();
        alertDialog_notifyPanel.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                imageView_notify.setImageResource(R.drawable.ic_left);
            }
        });
        alertDialog_notifyPanel.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alertDialog_notifyPanel.getWindow().setDimAmount(0.0f);
        alertDialog_notifyPanel.getWindow().setGravity(Gravity.BOTTOM |Gravity.RIGHT);
        alertDialog_notifyPanel.getWindow().getDecorView().setPadding(0,0,0,0);
        alertDialog_notifyPanel.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        alertDialog_notifyPanel.getWindow().setWindowAnimations(R.style.DialogRLAnimation);
        imageView_notify = view.findViewById(R.id.imageView_notify);
        imageView_notify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView_notify.setImageResource(R.drawable.ic_arrow_top);
                alertDialog_notifyPanel.show();
                WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                params.copyFrom(alertDialog_notifyPanel.getWindow().getAttributes());
                params.width =  (int)(dm.widthPixels *0.2);
                Log.d(TAG, "onClick: "+dm.heightPixels+" "+bottomLineY +" "+bottomLineH);
                params.height = bottomLineY;
                alertDialog_notifyPanel.getWindow().setAttributes(params);
            }
        });
    }

    private void initStatusBar() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // 获取当前媒体音量
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumeView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialog_layout_volume, null);
        textView_volume_value = volumeView.findViewById(R.id.volume_value);
        textView_volume_value.setText(currentVolume+"");
        seekBar = volumeView.findViewById(R.id.seekBar);
        seekBar.setProgress(currentVolume);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textView_volume_value.setText(""+progress);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,AudioManager.FLAG_SHOW_UI);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        alertDialog_volume=new  AlertDialog.Builder(DesktopService.this).setView(volumeView).create();
        alertDialog_volume.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alertDialog_volume.getWindow().setDimAmount(0.0f);
        alertDialog_volume.getWindow().setGravity(Gravity.BOTTOM |Gravity.LEFT);
        alertDialog_volume.getWindow().getDecorView().setPadding(0,0,0,0);
        alertDialog_volume.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        alertDialog_volume.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if(seekBar.getProgress() !=0){
                    imageView_volume.setImageResource(R.drawable.ic_volume);
                }else{
                    imageView_volume.setImageResource(R.drawable.ic_volume_off);

                }
            }
        });

        time = view.findViewById(R.id.textView_time);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                CharSequence dateFormat = DateFormat.format("HH:mm",System.currentTimeMillis());
                time.post(()->time.setText(dateFormat));
            }
        },0,1000);
        imageView_eth_wifi = view.findViewById(R.id.imageView_eth);
        imageView_volume = view.findViewById(R.id.imageView_volume);
        if(currentVolume ==0){
            imageView_volume.setImageResource(R.drawable.ic_volume_off);
        }
        imageView_volume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: "+clickX);
                alertDialog_volume.getWindow().getAttributes().x =  clickX -(int)(dm.widthPixels *0.1);;
                alertDialog_volume.show();
                WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                params.copyFrom(alertDialog_volume.getWindow().getAttributes());
                params.width =  (int)(dm.widthPixels *0.2);
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                alertDialog_volume.getWindow().setAttributes(params);
            }
        });

    }

    private void initBottomLine() {
        bottom_line = view.findViewById(R.id.bottom_line);
        bottom_dialogView = LayoutInflater.from(DesktopService.this).inflate(R.layout.dialog_layout_bottom,null);
        backToDeskTop = bottom_dialogView.findViewById(R.id.backToDeskTop);
        backToDeskTop.setOnClickListener(v -> {
            if(alertDialog_bottom.isShowing()){
                alertDialog_bottom.dismiss();
            }
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        alertDialog_bottom =new  AlertDialog.Builder(DesktopService.this).setView(bottom_dialogView).create();

        alertDialog_bottom.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alertDialog_bottom.getWindow().setDimAmount(0.0f);
        alertDialog_bottom.getWindow().setGravity(Gravity.BOTTOM |Gravity.LEFT);
        alertDialog_bottom.getWindow().getDecorView().setPadding(0,0,0,0);
        alertDialog_bottom.getWindow().getDecorView().setBackgroundColor(Color.WHITE);
        bottom_line.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int[] location = new int[2];
                bottom_line.getLocationOnScreen(location);
                bottomLineY = location[1];
                bottomLineH =  bottom_line.getHeight();
                bottomLineW =  bottom_line.getWidth();
                alertDialog_window.getWindow().getAttributes().y = bottom_line.getHeight();
                alertDialog_volume.getWindow().getAttributes().y = bottom_line.getHeight();
                alertDialog_notifyPanel.getWindow().getAttributes().y = bottom_line.getHeight();
                bottom_line.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        bottom_line.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {

                if(event.getButtonState() == MotionEvent.BUTTON_PRIMARY){

                    clickX = (int) event.getX();
                    clickY = (int) event.getY();
                    Log.d(TAG, "onGenericMotion: "+clickX+" "+clickY);
                }
                if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY || event.getButtonState() == MotionEvent.BUTTON_BACK) {
                    // 获取点击位置的坐标
                    clickX = (int) event.getX();
                    clickY = (int) event.getY();
                    Log.d(TAG, "onGenericMotion: "+clickX+"+"+clickY+" "+event.getButtonState());
                    alertDialog_bottom.getWindow().getAttributes().x = clickX;
                    alertDialog_bottom.getWindow().getAttributes().y = bottomLineH - clickY;
                    alertDialog_bottom.show();
                    WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                    params.copyFrom(alertDialog_bottom.getWindow().getAttributes());
                    params.width =  (int)(dm.widthPixels *0.1);
                    params.height =(int)(dm.heightPixels *0.05);
                    alertDialog_bottom.getWindow().setAttributes(params);
                    return true; // 表示已处理事件
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (view != null) {
            windowManager.removeView(view);
        }
        if(netReceiver!=null){
            unregisterReceiver(netReceiver);
        }
        if(serviceConnection!=null){
            unbindService(serviceConnection);
        }
    }

    private void initWindowPanel(){
        imageView_window = view.findViewById(R.id.imageView_win);
        window_dialogView = LayoutInflater.from(DesktopService.this).inflate(R.layout.dialog_layout_left,null);
        recyclerView = window_dialogView.findViewById(R.id.recyclerView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(DesktopService.this));
        ImageView imageView_power = window_dialogView.findViewById(R.id.imageView_power);
        imageView_power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String command = "input keyevent --longpress  26";
                        try {
                            Process process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });

                            // 等待命令执行完成
                            int exitCode = process.waitFor();

                            // 检查命令执行结果
                            if (exitCode == 0) {
                                Log.d(TAG, "run: 0");
                                // 命令执行成功
                            } else {
                                // 命令执行失败
                                Log.d(TAG, "run: -1");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        alertDialog_window =new  AlertDialog.Builder(DesktopService.this).setView(window_dialogView).create();
        alertDialog_window.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        alertDialog_window.getWindow().setDimAmount(0.0f);
        alertDialog_window.getWindow().setGravity(Gravity.BOTTOM |Gravity.LEFT);
        alertDialog_window.getWindow().getDecorView().setPadding(0,0,0,0);
        alertDialog_window.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        alertDialog_window.getWindow().setWindowAnimations(R.style.DialogLRAnimation);
        imageView_window.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(alertDialog_window !=null){
                        alertDialog_window.show();
                        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                        params.copyFrom(alertDialog_window.getWindow().getAttributes());
                        params.width =  (int)(dm.widthPixels *0.3);
                        params.height =(int)(dm.heightPixels *0.4);
                        alertDialog_window.getWindow().setAttributes(params);
                }

            }
        });
    }


}