package com.gonimo.baby;

import android.app.NotificationManager;
import android.app.Notification;
import android.content.Context;

// We should probably use the Android compatibility package ....
public class AndroidCompat {
    // public static Notification.Builder makeNotificationBuilder(Context context, String channelId) {
    //     if(android.os.Build.VERSION.SDK_INT >= 26)
    //         new Notification.Builder(this, channelId + magic)
    //     else
    //         new Notification.Builder(this)
    // }
    public static NotificationManager getNotificationManager(Context ctx) {
        NotificationManager notificationManager = android.os.Build.VERSION.SDK_INT >= 23 ? ctx.getSystemService(NotificationManager.class) : (NotificationManager) ctx.getSystemService(ctx.NOTIFICATION_SERVICE);
        return notificationManager;
    }
}

/*
  magic (https://developer.android.com/training/notify-user/build-notification.html):
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
  // Create the NotificationChannel, but only on API 26+ because
  // the NotificationChannel class is new and not in the support library
  CharSequence name = getString(R.string.channel_name);
  String description = getString(R.string.channel_description);
  int importance = NotificationManagerCompat.IMPORTANCE_DEFAULT;
  NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
  channel.setDescription(description);
  // Register the channel with the system
  NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
  notificationManager.createNotificationChannel(channel);
  }

 */
