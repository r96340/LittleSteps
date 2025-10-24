package com.counterintuitive.littlesteps;

import static android.content.ContentValues.TAG;

import static com.counterintuitive.littlesteps.CounterService.ACTION_RESET;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.counterintuitive.littlesteps.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    // 宣告主畫面計時器文字元件內容讀寫器
    private TextView counterTextView;
    // 宣告接收器以接收來自計時器服務的廣播
    private BroadcastReceiver counterUpdateReceiver;
    // 宣告取得權限的回呼方法
    // 通知
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCounterService();
                } else {

                }
            });

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        startOverlayService();
                    } else {

                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //初始化計數器及其廣播接口
        counterTextView = findViewById(R.id.counter_text_view);
        counterUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals(CounterService.ACTION_COUNTER_UPDATE)) {
                    String counterText = intent.getStringExtra(CounterService.EXTRA_COUNTER_TEXT);
                    if (counterTextView != null) {
                        counterTextView.setText(counterText);
                    }
                }
            }
        };
        startCounterServiceWithPermissionCheck();

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkOverlayPermissionAndShow();
            }
        });
    }

    private void startCounterServiceWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            startCounterService();
        }
    }

    private void startCounterService() {
        Intent serviceIntent = new Intent(this, CounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(counterUpdateReceiver,
                new IntentFilter(CounterService.ACTION_COUNTER_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(counterUpdateReceiver);
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        LocalBroadcastManager.getInstance(this).registerReceiver(counterUpdateReceiver,
//                new IntentFilter(CounterService.ACTION_COUNTER_UPDATE));
//    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(counterUpdateReceiver);
//    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(counterUpdateReceiver);
//    }

    private void checkOverlayPermissionAndShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                overlayPermissionLauncher.launch(intent);
            } else {
                startOverlayService();
            }
        } else {
            startOverlayService();
        }
    }

    private void startOverlayService() {
        startService(new Intent(this, OverlayService.class));
    }

//    private void stopOverlayService() {
//        stopService(new Intent(this, OverlayService.class));
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_reset) {
            resetCounterAndPreferences();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopCounterService(boolean isResetting) {
        Intent serviceIntent = new Intent(this, CounterService.class);
        if (isResetting) {
            serviceIntent.setAction(CounterService.ACTION_STOP_SERVICE);
            stopService(serviceIntent);
            serviceIntent.setAction(CounterService.ACTION_RESET);
            stopService(serviceIntent);
        }
    }

    private void resetCounterAndPreferences() {
        stopCounterService(true);
        SharedPreferences prefs = getSharedPreferences("CounterServicePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("cycle", 1);
        editor.putInt("subcycle", 1);
        editor.commit();
        if (counterTextView != null) {
            counterTextView.setText("請重新啟動應用程式以重啟計時");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}