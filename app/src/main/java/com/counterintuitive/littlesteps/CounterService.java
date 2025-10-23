package com.counterintuitive.littlesteps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CounterService extends Service {

    // 初始化進度快取
    private static final String PREFS_NAME = "CounterServicePrefs";
    private static final String KEY_SECONDS_PASSED = "secondsPassed";
    private static final String KEY_CYCLE = "cycle";
    private static final String KEY_SUBCYCLE = "subcycle";

    // 宣告通知相關常數
    public static final String ACTION_COUNTER_UPDATE = "com.counterintuitive.littlesteps.COUNTER_UPDATE";
    public static final String EXTRA_COUNTER_TEXT = "extra_counter_text";
    private static final String CHANNEL_ID = "CounterServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    // 宣告計數器相關變數
    private int secondsPassed = 0;
    private int cycle = 1;
    private int subcycle = 1;
    private final Handler timerHandler = new Handler();
    private NotificationManager notificationManager;
    private boolean shouldRest = false;

    // 宣告停止按鈕的廣播接收器
    public static final String ACTION_STOP_SERVICE = "com.counterintuitive.littlesteps.STOP_SERVICE";
    private final BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
                timerHandler.removeCallbacks(timerRunnable);
                stopForeground(true);
                stopSelf();
                Log.d("CounterService", "Service stopped via notification action.");
            }
        }
    };

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            secondsPassed++;
            String counterText = "目標秒數 " + cycle + " 第 " + subcycle + " 輪\n" + secondsPassed + " 秒";
            // 將計數器的狀態更新到通知欄
            updateNotification(counterText);
            // 將計數器的狀態更新到主UI
            sendBroadcastToActivity(counterText);

            // 對數式遞增計數器
            if (secondsPassed == cycle) {
                secondsPassed = 0;
                if (subcycle == cycle) {
                    cycle++;
                    subcycle = 1;
                } else {
                    subcycle++;
                }
                shouldRest = !shouldRest;
                playSound(getApplicationContext());
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        loadCounterState();
        createNotificationChannel();
        registerReceiver(stopServiceReceiver, new IntentFilter(ACTION_STOP_SERVICE), RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            Log.d("CounterService", "Stopping service from onStartCommand.");
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        loadCounterState();
        Notification notification = createNotification("Counter service is running...");
        startForeground(NOTIFICATION_ID, notification);
        timerHandler.post(timerRunnable);
        Log.d("CounterService", "Service started.");
        return START_STICKY;
    }

    private void saveCounterState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_SECONDS_PASSED, secondsPassed);
        editor.putInt(KEY_CYCLE, cycle);
        editor.putInt(KEY_SUBCYCLE, subcycle);
        editor.apply();
        Log.d("CounterService", "Counter state saved.");
    }

    private void loadCounterState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        secondsPassed = prefs.getInt(KEY_SECONDS_PASSED, 0);
        cycle = prefs.getInt(KEY_CYCLE, 1);
        subcycle = prefs.getInt(KEY_SUBCYCLE, 1);
        Log.d("CounterService", "Counter state loaded.");
    }

    private void resetCounterState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d("CounterService", "Counter state reset.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        unregisterReceiver(stopServiceReceiver);
        saveCounterState();
        Log.d("CounterService", "Service destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Counter Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }


    private Notification createNotification(String text) {
        Intent stopBroadcastIntent = new Intent(ACTION_STOP_SERVICE);
        stopBroadcastIntent.setComponent(new android.content.ComponentName(this, StopServiceReceiver.class));
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                stopBroadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        int color = shouldRest ? Color.BLUE : Color.RED;
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Counter")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setColor(color)
                .setOnlyAlertOnce(true)
                .addAction(0, "停止計數器", stopPendingIntent)
                .build();
    }

    public static class StopServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
                Intent stopServiceIntent = new Intent(context, CounterService.class);
                stopServiceIntent.setAction(ACTION_STOP_SERVICE);
                context.startService(stopServiceIntent);
                Log.d("StopServiceReceiver", "Stop action received, telling service to stop.");
            }
        }
    }

    private void updateNotification(String text) {
        Notification notification = createNotification(text);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void sendBroadcastToActivity(String counterText) {
        Intent intent = new Intent(ACTION_COUNTER_UPDATE);
        intent.putExtra(EXTRA_COUNTER_TEXT, counterText);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void playSound(Context context) {
        try {
            // 通知音
            Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notificationSoundUri);
            r.play();
            // 震動
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
