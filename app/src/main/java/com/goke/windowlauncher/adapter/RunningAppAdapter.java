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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.goke.windowlauncher.R;

import java.util.List;

public class RunningAppAdapter extends RecyclerView.Adapter<RunningAppAdapter.MyHolder> {
    private List<ResolveInfo> apps;
    private PackageManager packageManager;
    private Context context;

    public RunningAppAdapter(List<ResolveInfo> apps, PackageManager packageManager, Context context) {
        this.apps = apps;
        this.packageManager = packageManager;
        this.context = context;
    }


    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RunningAppAdapter.MyHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.icon_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.appIcon.setImageDrawable(packageManager.getApplicationIcon(
                apps.get(position).activityInfo.applicationInfo));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        ImageView appIcon;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);

        }
    }
}
