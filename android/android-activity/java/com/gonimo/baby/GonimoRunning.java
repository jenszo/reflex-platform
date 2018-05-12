package com.gonimo.baby;

import android.content.Intent;
import android.content.Context;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.Service;
import android.os.IBinder;
import com.gonimo.baby.R;
import systems.obsidian.HaskellActivity;
import com.gonimo.baby.AndroidCompat;



// Service needed so we can reliably get rid of the Gonimo-running notification when Gonimo gets destroyed.
public class GonimoRunning extends Service {

  @Override
  public void onCreate() {
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
      // If we get killed, after returning from here, restart
      // showRunningNotification();
      return START_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
      // We don't provide binding, so return null
      return null;
  }

  @Override
  public void onDestroy() {
      cancelRunningNotification(this);
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
      cancelRunningNotification(this);
      stopSelf();
  }


  public static void cancelRunningNotification(Context c) {
      NotificationManager notificationManager = AndroidCompat.getNotificationManager(c);

      // Now hopefully cancel:
      notificationManager.cancel(HaskellActivity.notificationId);
  }
}
