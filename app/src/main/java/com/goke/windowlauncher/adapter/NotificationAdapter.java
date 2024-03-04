package com.goke.windowlauncher.adapter;

import android.app.Notification;
import android.content.pm.PackageManager;
import android.service.notification.StatusBarNotification;
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

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.MyHolder> {
    private final String TAG="NotificationAdapter";
    private List<StatusBarNotification> statusBarNotificationList;
    private PackageManager packageManager;

    public NotificationAdapter(List<StatusBarNotification> statusBarNotificationList, PackageManager packageManager) {
        this.statusBarNotificationList = statusBarNotificationList;
        this.packageManager = packageManager;
    }

    @NonNull
    @Override
    public NotificationAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationAdapter.MyHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.MyHolder holder, int position) {
        holder.title.setText(statusBarNotificationList.get(position).getNotification().extras.getCharSequence(Notification.EXTRA_TITLE));
        holder.content.setText(statusBarNotificationList.get(position).getNotification().extras.getCharSequence(Notification.EXTRA_TEXT));
        try {
            holder.imageView_icon.setImageDrawable( packageManager.getApplicationIcon(statusBarNotificationList.get(position).getPackageName()) );
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
            }
        });
    }

    @Override
    public int getItemCount() {
        return statusBarNotificationList.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder{
        TextView title,content;
        ImageView imageView_icon;
        public MyHolder(@NonNull View itemView) {
            super(itemView);
            imageView_icon = itemView.findViewById(R.id.imageView_icon);
            title = itemView.findViewById(R.id.title);
            content = itemView.findViewById(R.id.content);

        }
    }
    public interface NotificationListener{
        public void onNotificationClick();
    }
}
