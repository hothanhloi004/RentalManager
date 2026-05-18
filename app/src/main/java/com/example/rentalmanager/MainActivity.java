package com.example.rentalmanager;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.rentalmanager.ui.bill.BillFragment;
import com.example.rentalmanager.ui.contract.ContractListFragment;
import com.example.rentalmanager.ui.dashboard.DashboardFragment;
import com.example.rentalmanager.ui.dashboard.SettingsFragment;
import com.example.rentalmanager.ui.room.RoomFragment;
import com.example.rentalmanager.ui.tenant.TenantFragment;
import com.example.rentalmanager.util.NotificationHelper;
import com.example.rentalmanager.util.ReminderWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.os.Build;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
public class MainActivity extends AppCompatActivity {

    public static final String ACTION_CREATE_CONTRACT = "com.example.rentalmanager.action.CREATE_CONTRACT";
    public static final String EXTRA_PRESELECT_ROOM_ID = "extra_preselect_room_id";

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng Dark Mode đã lưu trước khi inflate layout
        android.content.SharedPreferences prefs =
                getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                       : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        // Mac dinh mo Dashboard khi app start
        if (savedInstanceState == null) {
            if (!handleIntentNavigation(getIntent())) {
                bottomNav.setSelectedItemId(R.id.nav_dashboard);
                loadFragment(new DashboardFragment());
            }
        }

        // Khoi dong WorkManager nhac nho hang ngay
        NotificationHelper.createChannel(this);
        PeriodicWorkRequest reminderRequest =
            new PeriodicWorkRequest.Builder(ReminderWorker.class, 1,
                java.util.concurrent.TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest);

        // API 33+ (Android 13) POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Fetch FCM token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        com.example.rentalmanager.service.MyFirebaseMessagingService.sendRegistrationToServer(token);
                    }
                });

        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_room) {
                selectedFragment = new RoomFragment();

            } else if (item.getItemId() == R.id.nav_tenant) {
                selectedFragment = new TenantFragment();

            } else if (item.getItemId() == R.id.nav_contract) {
                selectedFragment = new ContractListFragment();

            } else if (item.getItemId() == R.id.nav_bill) {
                selectedFragment = new BillFragment();
            }

            else if (item.getItemId() == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            }

            return loadFragment(selectedFragment);
        });

        if (savedInstanceState != null) {
            handleIntentNavigation(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentNavigation(intent);
    }

    private boolean handleIntentNavigation(Intent intent) {
        if (intent == null || !ACTION_CREATE_CONTRACT.equals(intent.getAction())) {
            return false;
        }

        int roomId = intent.getIntExtra(EXTRA_PRESELECT_ROOM_ID, -1);
        Bundle args = new Bundle();
        args.putInt(com.example.rentalmanager.ui.contract.ContractFragment.ARG_PRESELECT_ROOM_ID, roomId);

        com.example.rentalmanager.ui.contract.ContractFragment fragment = new com.example.rentalmanager.ui.contract.ContractFragment();
        fragment.setArguments(args);

        if (bottomNav != null) {
            bottomNav.getMenu().findItem(R.id.nav_contract).setChecked(true);
        }
        loadFragment(fragment);
        intent.setAction(null);
        return true;
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment == null) return false;

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        return true;
    }

}
