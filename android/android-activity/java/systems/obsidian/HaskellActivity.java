package systems.obsidian;

import android.app.Activity;
import android.content.Intent;
import android.app.PendingIntent;
import android.app.Notification;
import android.os.Bundle;
import android.util.Log;
import java.util.concurrent.SynchronousQueue;
import android.content.pm.PackageManager;
import android.Manifest;
import android.webkit.PermissionRequest;
import android.provider.Settings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.HashSet;
import android.annotation.TargetApi;
import android.os.Build;
import android.content.Context;
import android.os.PowerManager;
import android.net.Uri;
import android.app.NotificationManager;

// import android.app.Service;
// import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.net.wifi.WifiManager.WifiLock;
import android.net.wifi.WifiManager;
import android.content.pm.PackageManager;
import com.gonimo.baby.R;
import com.gonimo.baby.GonimoRunning;
import com.gonimo.baby.AndroidCompat;

public class HaskellActivity extends Activity {
  public native int haskellStartMain(SynchronousQueue<Long> setCallbacks);
  public native void haskellOnCreate(long callbacks);
  public native void haskellOnCreateWithIntent(long callbacks, String intent, String intentdata);
  public native void haskellOnStart(long callbacks);
  public native void haskellOnResume(long callbacks);
  public native void haskellOnPause(long callbacks);
  public native void haskellOnStop(long callbacks);
  public native void haskellOnDestroy(long callbacks);
  public native void haskellOnRestart(long callbacks);
  public native void haskellOnBackPressed(long callbacks);
  public native void haskellOnNewIntent(long callbacks, String intent, String intentdata);

  // Apparently 'long' is the right way to store a C pointer in Java
  // See https://stackoverflow.com/questions/337268/what-is-the-correct-way-to-store-a-native-pointer-inside-a-java-object
  final long callbacks;

  static {
    System.loadLibrary("HaskellActivity");
  }

  public HaskellActivity() throws InterruptedException {
    final SynchronousQueue<Long> setCallbacks = new SynchronousQueue<Long>();
    permissionRequests = new HashMap<Integer, PermissionRequest>();
    new Thread() {
      public void run() {
        final int exitCode = haskellStartMain(setCallbacks);
        Log.d("HaskellActivity", String.format("Haskell main action exited with status %d", exitCode));
        try {
          // Since Haskell's main has exited, it won't call mainStarted.
          // Instead, we unblock the main thread here.
          //TODO: If continueWithCallbacks has already been called, is this safe?
          setCallbacks.put(0L); //TODO: Always call finish() if we hit this
        } catch(InterruptedException e) {
          //TODO: Should we do something with this?
        }
      }
    }.start();
    callbacks = setCallbacks.take();
  }

  // This can be called by the Haskell application to unblock the construction
  // of the HaskellActivity and proced with the given callbacks

  // NOTE: This shouldn't be an instance method, because it must be called *before*
  // the constructor returns (it *causes* the constructor to return).
  // 'callbacks' should never be 0
  private static void continueWithCallbacks(SynchronousQueue<Long> setCallbacks, long callbacks) {
    try {
      setCallbacks.put(callbacks);
    } catch(InterruptedException e) {
      Log.d("HaskellActivity", "setting callbacks interrupted");
      //TODO: Should we do something with this?
    }
  }
  private boolean isIgnoringBatteryOptimizations() {
    Context context=this;
    String packageName = context.getPackageName();
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    return pm.isIgnoringBatteryOptimizations(packageName);
  }

  private void askIgnoreBatteryOptimizations() {
      try {
        if(isIgnoringBatteryOptimizations())
            Log.d("HaskellActivity", "We are ignoring doze!!!!! Yeah!");
        else {
            Log.d("HaskellActivity", "We are not ignoring doze!!!!! Oh noooooooo!");
            Intent intentBattery = new Intent( Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                             , Uri.parse("package:" + getPackageName())
                                             );
            // Necessary to prevent it showing up (dead) in the task manager.
            intentBattery.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            startActivity(intentBattery);
        }
      }
      catch (NoSuchMethodError e) {
          Log.d("HaskellActivity", "Asking for ignore battery optimizations failed. (Not supported and thus irrelevant ;-) ");
      }
  }

    // TODO: Delete, as we no longer show running notifications.
  private void showRunningNotification() {
      Notification.Builder templ =
          new Notification.Builder(this)
          .setSmallIcon(R.drawable.ic_launcher)
          .setContentTitle(getString(R.string.gonimo_running));
      showActionNotification(templ);
  }

  public void showStoppedWarningNotification() {
      Notification.Builder templ =
          new Notification.Builder(this)
          .setSmallIcon(android.R.drawable.stat_sys_warning)
          .setContentTitle(getString(R.string.gonimo_must_run_in_the_foreground))
          .setContentText(getString(R.string.please_switch_back_to_gonimo));
      showActionNotification(templ);
  }

  private void showActionNotification(Notification.Builder templ) {
    Intent intent = new Intent(this, HaskellActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    Intent stopIntent = new Intent(this, HaskellActivity.class);
    stopIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    stopIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    stopIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    stopIntent.putExtra("com.gonimo.baby.stopIt", true);
    PendingIntent stopPending = PendingIntent.getActivity(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification notification = templ
        .setContentIntent(pendingIntent)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_gonimo), stopPending)
        .setDeleteIntent(stopPending)
        .build();
    NotificationManager notificationManager = AndroidCompat.getNotificationManager(this);
    notificationManager.notify(notificationId, notification);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    askIgnoreBatteryOptimizations();
    // We can't call finish() in the constructor, as it will have no effect, so
    // we call it here whenever we reach this code without having hit
    // 'continueWithCallbacks'

    Intent serviceIntent = new Intent(this, GonimoRunning.class);
    startService(serviceIntent);

    grabWakeLock();

    if(callbacks == 0) {
      finish();
    } else {
      haskellOnCreate(callbacks); //TODO: Pass savedInstanceState as well
      Intent intent = getIntent();
      String intentDataString = intent == null || intent.getDataString() == null ? "" : intent.getDataString();
      String intentAction = intent == null || intent.getAction() == null ? "" : intent.getAction();
      haskellOnCreateWithIntent(callbacks, intentAction, intentDataString); //TODO: Use a more canonical way of passing this data - i.e. pass the Intent and let the Haskell side get the data out with JNI
    }
  }

  private void grabWakeLock() {
      PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
      if (powerManager != null) {
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                    "MyWakelockTag");
        if (wakeLock != null)
            wakeLock.acquire();
      }

      WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
      if (wifiManager != null) {
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "gonimo-wifi-lock");

        if (wifiLock != null)
            wifiLock.acquire();
      }
  }

  private void releaseWakeLock() {
      if(wakeLock != null)
          wakeLock.release();
      if(wifiLock != null)
          wifiLock.release();
  }

  @Override
  public void onStart() {
    super.onStart();
    // Warning notification gets shown via Haskell, if a stream or something is running, cancel it now.
    GonimoRunning.cancelRunningNotification(this);
    if(callbacks != 0) {
      haskellOnStart(callbacks);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if(callbacks != 0) {
      haskellOnResume(callbacks);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if(callbacks != 0) {
      haskellOnPause(callbacks);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if(callbacks != 0) {
      haskellOnStop(callbacks);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if(callbacks != 0) {
      haskellOnDestroy(callbacks);
    }
    releaseWakeLock();
    GonimoRunning.cancelRunningNotification(this); // Don't know why this is necessary. We have a service that should have his onTaskRemoved triggered.
    //TODO: Should we call hs_exit somehow here?
    android.os.Process.killProcess(android.os.Process.myPid()); //TODO: Properly handle the process surviving between invocations which means that the Haskell RTS needs to not be initialized twice.
  }

  @Override
  public void onRestart() {
    super.onRestart();
    if(callbacks != 0) {
      haskellOnRestart(callbacks);
    }
  }

  @Override
  public void onBackPressed() {
    if(backEventListener != null) {
        backEventListener.backButtonClicked();
        return;
    }
    if(callbacks != 0) {
      haskellOnBackPressed(callbacks);
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    if(intent != null && intent.getExtras() != null && intent.getExtras().containsKey("com.gonimo.baby.stopIt")) {
        Log.d("HaskellActivity", "stopIt requested ... stopping");
        finishAndRemoveTask();
        return;
    }
    if(callbacks != 0 && intent != null && intent.getData() != null && intent.getAction() != null) {
      haskellOnNewIntent(callbacks, intent.getAction(), intent.getDataString()); //TODO: Use a more canonical way of passing this data - i.e. pass the Intent and let the Haskell side get the data out with JNI
    }
  }

  // Proper separation of concerns is really a whole lot of work in Java, so
  // we simply handle PermissionRequests directly here - it is just sooo much
  // easier. Java makes it really hard to write good code.
  public void requestWebViewPermissions(final PermissionRequest request) {
      try {
        String[] resources = request.getResources();
        ArrayList<String> sysResourcesToRequestList = new ArrayList<String>();
        for(int i=0; i < resources.length; i++) {
            String manifestRequest = null;
            switch(resources[i]) {
                case PermissionRequest.RESOURCE_AUDIO_CAPTURE:
                    manifestRequest =  Manifest.permission.RECORD_AUDIO;
                    break;
                case PermissionRequest.RESOURCE_VIDEO_CAPTURE:
                    manifestRequest = Manifest.permission.CAMERA;
                    break;
            }
            if(manifestRequest != null && checkSelfPermission(manifestRequest) != PackageManager.PERMISSION_GRANTED)
                sysResourcesToRequestList.add(manifestRequest);
        }
        String[] sysResourcesToRequest = sysResourcesToRequestList.toArray(new String[0]);
        if(sysResourcesToRequest.length > 0) {
            permissionRequests.put(nextRequestCode, request);
            requestPermissions(sysResourcesToRequest, nextRequestCode++);
        }
        else {
            runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
        }
      } catch (NoSuchMethodError e) { // Compatibility for older Android versions (Android 5 and below)
          runOnUiThread(new Runnable() {
                  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                  @Override
                  public void run() {
                      request.grant(request.getResources());
                  }
              });
      }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
      final PermissionRequest request = permissionRequests.get(requestCode);
      permissionRequests.remove(requestCode);

      String[] requestedPermissions = request.getResources();
      final HashSet<String> grantedPermissions = new HashSet<String>(Arrays.asList(requestedPermissions));

      // We assume grantResults and permissions have same length ... obviously.
      for(int i = 0; i< permissions.length; i++) {
          if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
              String permission = null;
              switch(permissions[i]) {
                  case Manifest.permission.RECORD_AUDIO:
                      Log.d("HaskellActivity", "Granting RESOURCE_AUDIO_CAPTURE!");
                      permission = PermissionRequest.RESOURCE_AUDIO_CAPTURE;
                      break;
                  case Manifest.permission.CAMERA:
                      Log.d("HaskellActivity", "Granting RESOURCE_VIDEO_CAPTURE!");
                      permission = PermissionRequest.RESOURCE_VIDEO_CAPTURE;
                      break;
              }
              if(permission != null && grantResults[i] != PackageManager.PERMISSION_GRANTED)
                  grantedPermissions.remove(permission);
          }
      }
      runOnUiThread(new Runnable() {
              @TargetApi(Build.VERSION_CODES.LOLLIPOP)
              @Override
              public void run() {
                if(grantedPermissions.size() > 0) {
                    Log.d("HaskellActivity", "Granting permissions!");
                    request.grant(grantedPermissions.toArray(new String[0]));
                }
                else {
                    request.deny();
                }
              }
          });
  }

  public interface BackEventListener {
      void backButtonClicked();
  }

  public void setBackEventListener(BackEventListener l) {
      backEventListener = l;
  }

  private BackEventListener backEventListener = null;
  private HashMap<Integer, PermissionRequest> permissionRequests;
  private int nextRequestCode = 0;
  final public static int notificationId = 31415926;
  // private Intent serviceIntent;
  private WakeLock wakeLock;
  private WifiLock wifiLock;
}
