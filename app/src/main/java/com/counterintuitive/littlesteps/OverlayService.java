package com.counterintuitive.littlesteps;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class OverlayService extends Service {
    private WindowManager windowManager;
    private View overlayView;
    private BroadcastReceiver counterUpdateReceiver;
    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);

        int layoutParamsType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParamsType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParamsType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;

        windowManager.addView(overlayView, params);
        setupCounterUpdateReceiver();

        ImageView closeButton = overlayView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(v -> {
            stopSelf();
        });
        addTouchListenerToOverlay();
    }

    private void setupCounterUpdateReceiver() {
        counterUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && CounterService.ACTION_COUNTER_UPDATE.equals(intent.getAction())) {
                    boolean shouldRest = intent.getBooleanExtra(CounterService.EXTRA_SHOULD_REST, false);
                    if (overlayView != null) {
                        View root = overlayView.findViewById(R.id.overlay_root);
                        if (shouldRest) {
                            root.setBackgroundColor(Color.parseColor("#880000FF"));
                        } else {
                            root.setBackgroundColor(Color.parseColor("#88FF0000"));
                        }
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                counterUpdateReceiver, new IntentFilter(CounterService.ACTION_COUNTER_UPDATE)
        );
    }

    private void addTouchListenerToOverlay() {
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
    }
}
