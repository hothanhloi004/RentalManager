package com.example.rentalmanager.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.rentalmanager.MainActivity;
import com.example.rentalmanager.R;

public class NotificationHelper {

    public static final String CHANNEL_ID = "RENTAL_REMINDER";
    public static final String CHANNEL_NAME = "Nhắc nhở thanh toán";

    public static void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Nhắc nhở hoá đơn quá hạn và hợp đồng sắp hết hạn");
            NotificationManager mgr = ctx.getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(channel);
        }
    }

    public static void sendNotification(Context ctx, int id, String title, String content) {
        Intent intent = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManager mgr = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr != null) mgr.notify(id, builder.build());
    }
}
