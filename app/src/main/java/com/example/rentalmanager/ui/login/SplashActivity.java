package com.example.rentalmanager.ui.login;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rentalmanager.R;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2500; // 2.5 giây

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ẩn Action Bar để toàn màn hình
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_splash);

        // Animation cho Logo (phóng to + mờ dần vào)
        ImageView imgLogo = findViewById(R.id.imgLogo);
        TextView tvAppName = findViewById(R.id.tvAppName);
        TextView tvTagline = findViewById(R.id.tvTagline);

        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(800);

        Animation slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        slideUp.setDuration(600);

        imgLogo.startAnimation(fadeIn);
        tvAppName.startAnimation(fadeIn);
        tvTagline.startAnimation(fadeIn);

        // Chuyển sang LoginActivity sau SPLASH_DELAY_MS mili giây
        new Handler(getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            // Hiệu ứng chuyển màn hình (mờ dần)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish(); // Đóng SplashActivity để không thể bấm Back quay lại
        }, SPLASH_DELAY_MS);
    }
}
