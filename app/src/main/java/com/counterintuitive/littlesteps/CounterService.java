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

            // Update the foreground notification
            updateNotification(counterText);

            // Send a broadcast to update the UI in MainActivity if it's open
            sendBroadcastToActivity(counterText);

            if (secondsPassed == cycle) {
                secondsPassed = 0;
                if (subcycle == cycle) {
                    cycle++;
                    subcycle = 1;
                } else {
                    subcycle++;
                }
            }
            // Schedule the next run
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
        // Start the service as a foreground service
        Notification notification = createNotification("Counter service is running...");
        startForeground(NOTIFICATION_ID, notification);

        // Start the counter
        timerHandler.post(timerRunnable);

        Log.d("CounterService", "Service started.");

        // If the service is killed, it will be automatically restarted
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the timer when the service is destroyed
        timerHandler.removeCallbacks(timerRunnable);
        Log.d("CounterService", "Service destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
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
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure you have this icon
                .setOnlyAlertOnce(true) // Don't make a sound for every update
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
