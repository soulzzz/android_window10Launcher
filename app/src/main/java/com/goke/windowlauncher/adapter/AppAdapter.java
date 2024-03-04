package com.goke.windowlauncher.adapter;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goke.windowlauncher.R;

import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.MyHolder> {
    private final  String TAG ="AppAdapter";
    public static int DESKTOP_ADAPTER = 0;
    public static int PANEL_ADAPTER = 1;
    public static int SMALL_ICON = 10;
    public static int MEDIUM_ICON = 11;
    public static int BIG_ICON = 12;
    private int type = 0;
    private List<ResolveInfo> apps ;
    private PackageManager pm;
    private Context context;
    private AppAdapterListener appAdapterListener;

    public AppAdapter(List<ResolveInfo> apps, PackageManager pm, Context context,int type) {
        this.apps = apps;
        this.pm = pm;
        this.context = context;
        this.type = type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setAppAdapterListener(AppAdapterListener appAdapterListener) {
        this.appAdapterListener = appAdapterListener;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int id = R.layout.app_item;
        if (MEDIUM_ICON == type || DESKTOP_ADAPTER ==type) {
            id = R.layout.app_item;
        }else if(PANEL_ADAPTER  == type){
            id = R.layout.panel_app_item;
        }else if(SMALL_ICON == type){
            id = R.layout.app_item_small;
        }else if(BIG_ICON ==type){
            id =R.layout.app_item_big;
        }
        return new MyHolder(LayoutInflater.from(parent.getContext())
                .inflate(id, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.appName.setText(pm.getApplicationLabel(
                apps.get(position).activityInfo.applicationInfo));
        holder.appIcon.setImageDrawable(pm.getApplicationIcon(
                apps.get(position).activityInfo.applicationInfo));
        holder.itemView.setOnClickListener(v->{
            if(appAdapterListener !=null){
                Log.d(TAG, "onAppClick: "+apps.get(position).activityInfo.packageName);
                appAdapterListener.onAppClick(apps.get(position).activityInfo.packageName);
            }
            String pkg = null;
            String cls = null;
            pkg = apps.get(position).activityInfo.packageName;
            cls = apps.get(position).activityInfo.name;
            if(TextUtils.isEmpty(pkg) || TextUtils.isEmpty(cls)){
                return;
            }
            try{
                ComponentName componentName = new ComponentName(pkg,cls);
                Intent it = new Intent(Intent.ACTION_MAIN);
                it.addCategory(Intent.CATEGORY_LAUNCHER);
                it.setComponent(componentName);
                it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(it);
            }catch (ActivityNotFoundException e){
                Log.e("AppAdapter", "ActivityNotFoundException: "+e.getMessage());
            }

        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        ImageView appIcon;
        TextView appName;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);

        }
    }
    public interface AppAdapterListener{
        public void onAppClick(String pkgName);
    }
}
