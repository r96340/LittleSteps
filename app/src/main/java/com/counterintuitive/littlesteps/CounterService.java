package com.counterintuitive.littlesteps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CounterService extends Service {

    public static final String ACTION_COUNTER_UPDATE = "com.counterintuitive.littlesteps.COUNTER_UPDATE";
    public static final String EXTRA_COUNTER_TEXT = "extra_counter_text";
    private static final String CHANNEL_ID = "CounterServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private int secondsPassed = 0;
    private int cycle = 1;
    private int subcycle = 1;
    private final Handler timerHandler = new Handler();
    private NotificationManager notificationManager;

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
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = createNotification("Counter service is running...");
        startForeground(NOTIFICATION_ID, notification);
        timerHandler.post(timerRunnable);
        Log.d("CounterService", "Service started.");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
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
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Counter")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOnlyAlertOnce(true)
                .build();
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
}
