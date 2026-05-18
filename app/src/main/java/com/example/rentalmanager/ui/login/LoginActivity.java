package com.example.rentalmanager.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rentalmanager.MainActivity;
import com.example.rentalmanager.R;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.SettingEntity;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.util.FirebaseSyncHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private int failCount = 0;
    private long lockedUntil = 0;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private View layoutAuth;
    private View layoutPin;

    private boolean isLoginMode = true;

    private MaterialButton btnForgotPassword;
    private MaterialCheckBox cbRememberLogin;
    private MaterialButton btnGoogleSignIn;
    private CircularProgressIndicator progressGoogle;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                setGoogleLoading(false);
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this,
                                "H\u1ee7y ch\u1ecdn t\u00e0i kho\u1ea3n ho\u1eb7c l\u1ed7i Google (" + e.getStatusCode() + ")",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        android.content.SharedPreferences prefs = getSharedPreferences("rm_prefs", MODE_PRIVATE);

        layoutAuth = findViewById(R.id.layoutAuth);
        layoutPin = findViewById(R.id.layoutPin);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean rememberLogin = prefs.getBoolean("remember_login", false);
        if (currentUser != null && !rememberLogin) {
            mAuth.signOut();
            currentUser = null;
        }

        if (currentUser == null) {
            setupAuthScreen();
        } else {
            checkPinRequired();
        }
    }

    private void setupAuthScreen() {
        layoutAuth.setVisibility(View.VISIBLE);
        layoutPin.setVisibility(View.GONE);

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextInputEditText edtUsername = findViewById(R.id.edtUsername);
        TextInputEditText edtPassword = findViewById(R.id.edtPassword);
        TextInputLayout layoutRePassword = findViewById(R.id.layoutRePassword);
        TextInputEditText edtRePassword = findViewById(R.id.edtRePassword);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        cbRememberLogin = findViewById(R.id.cbRememberLogin);
        TextView tvSwitchMode = findViewById(R.id.tvSwitchMode);

        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        progressGoogle = findViewById(R.id.progressGoogle);

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                if (cbRememberLogin != null) {
                    getSharedPreferences("rm_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("remember_login", cbRememberLogin.isChecked())
                            .apply();
                }
                setGoogleLoading(true);
                mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
            });
        }

        boolean rememberLogin = getSharedPreferences("rm_prefs", MODE_PRIVATE)
                .getBoolean("remember_login", false);
        if (cbRememberLogin != null) cbRememberLogin.setChecked(rememberLogin);

        updateAuthUI(tvTitle, layoutRePassword, btnSubmit, tvSwitchMode);

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateAuthUI(tvTitle, layoutRePassword, btnSubmit, tvSwitchMode);
        });

        if (btnForgotPassword != null) {
            btnForgotPassword.setOnClickListener(v -> {
                String loginId = edtUsername.getText().toString().trim();
                if (loginId.isEmpty()) {
                    Toast.makeText(this, "Vui l\u00f2ng nh\u1eadp Gmail tr\u01b0\u1edbc", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!isGmailEmail(loginId)) {
                    Toast.makeText(this, "Ch\u1ec9 h\u1ed7 tr\u1ee3 t\u00e0i kho\u1ea3n Gmail (@gmail.com)", Toast.LENGTH_LONG).show();
                    return;
                }

                String firebaseEmail = loginId.toLowerCase(Locale.ROOT);
                btnForgotPassword.setEnabled(false);
                String oldText = btnForgotPassword.getText() != null
                        ? btnForgotPassword.getText().toString()
                        : "Qu\u00ean m\u1eadt kh\u1ea9u?";
                btnForgotPassword.setText("\u0110ang g\u1eedi...");

                mAuth.sendPasswordResetEmail(firebaseEmail)
                        .addOnCompleteListener(this, task -> {
                            btnForgotPassword.setEnabled(true);
                            btnForgotPassword.setText(oldText);
                            if (task.isSuccessful()) {
                                Toast.makeText(this,
                                        "\u0110\u00e3 g\u1eedi link \u0111\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u \u0111\u1ebfn: " + firebaseEmail,
                                        Toast.LENGTH_LONG).show();
                            } else {
                                String error = task.getException() != null
                                        ? task.getException().getMessage()
                                        : "G\u1eedi th\u1ea5t b\u1ea1i";
                                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        btnSubmit.setOnClickListener(v -> {
            String loginId = edtUsername.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (loginId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui l\u00f2ng \u0111i\u1ec1n \u0111\u1ee7 th\u00f4ng tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cbRememberLogin != null) {
                getSharedPreferences("rm_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("remember_login", cbRememberLogin.isChecked())
                        .apply();
            }

            if (!isGmailEmail(loginId)) {
                Toast.makeText(this, "Ch\u1ec9 h\u1ed7 tr\u1ee3 t\u00e0i kho\u1ea3n Gmail (@gmail.com)", Toast.LENGTH_LONG).show();
                return;
            }

            String firebaseEmail = loginId.toLowerCase(Locale.ROOT);
            if (isLoginMode) {
                btnSubmit.setEnabled(false);
                btnSubmit.setText("\u0110ang \u0111\u0103ng nh\u1eadp...");

                mAuth.signInWithEmailAndPassword(firebaseEmail, password)
                        .addOnCompleteListener(this, task -> {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("\u0110\u0103ng nh\u1eadp");
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "\u0110\u0103ng nh\u1eadp th\u00e0nh c\u00f4ng!", Toast.LENGTH_SHORT).show();
                                checkPinRequired();
                            } else {
                                Toast.makeText(this, "Sai t\u00e0i kho\u1ea3n ho\u1eb7c m\u1eadt kh\u1ea9u!", Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                String rePassword = edtRePassword.getText().toString().trim();
                if (!password.equals(rePassword)) {
                    Toast.makeText(this, "M\u1eadt kh\u1ea9u nh\u1eadp l\u1ea1i kh\u00f4ng kh\u1edbp!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(this, "M\u1eadt kh\u1ea9u ph\u1ea3i t\u1eeb 6 k\u00fd t\u1ef1 tr\u1edf l\u00ean!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSubmit.setEnabled(false);
                btnSubmit.setText("\u0110ang \u0111\u0103ng k\u00fd...");

                mAuth.createUserWithEmailAndPassword(firebaseEmail, password)
                        .addOnCompleteListener(this, task -> {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("\u0110\u0103ng k\u00fd");
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "\u0110\u0103ng k\u00fd th\u00e0nh c\u00f4ng!", Toast.LENGTH_SHORT).show();
                                checkPinRequired();
                            } else {
                                String error = task.getException() != null
                                        ? task.getException().getMessage()
                                        : "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
                                if (error.contains("already in use")) {
                                    Toast.makeText(this, "T\u00ean \u0111\u0103ng nh\u1eadp n\u00e0y \u0111\u00e3 c\u00f3 ng\u01b0\u1eddi s\u1eed d\u1ee5ng!", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "\u0110\u0103ng k\u00fd th\u1ea5t b\u1ea1i: " + error, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }

    private void updateAuthUI(TextView tvTitle, TextInputLayout layoutRePassword, MaterialButton btnSubmit, TextView tvSwitchMode) {
        if (isLoginMode) {
            tvTitle.setText("\u0110\u0103ng Nh\u1eadp");
            layoutRePassword.setVisibility(View.GONE);
            btnSubmit.setText("\u0110\u0103ng nh\u1eadp");
            tvSwitchMode.setText("Ch\u01b0a c\u00f3 t\u00e0i kho\u1ea3n? \u0110\u0103ng k\u00fd ngay");
            if (btnForgotPassword != null) btnForgotPassword.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("\u0110\u0103ng K\u00fd");
            layoutRePassword.setVisibility(View.VISIBLE);
            btnSubmit.setText("\u0110\u0103ng k\u00fd");
            tvSwitchMode.setText("\u0110\u00e3 c\u00f3 t\u00e0i kho\u1ea3n? \u0110\u0103ng nh\u1eadp ngay");
            if (btnForgotPassword != null) btnForgotPassword.setVisibility(View.GONE);
        }
    }

    private void checkPinRequired() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            setupAuthScreen();
            return;
        }

        String newUid = user.getUid();
        android.content.SharedPreferences prefs = getSharedPreferences("rm_prefs", MODE_PRIVATE);
        String lastUid = prefs.getString("last_uid", null);

        boolean isNewAccount = !newUid.equals(lastUid);
        if (isNewAccount) {
            if (lastUid == null) {
                prefs.edit().putString("last_uid", newUid).apply();
                Toast.makeText(this, "Đang tải dữ liệu từ đám mây...", Toast.LENGTH_SHORT).show();
                FirebaseSyncHelper.restoreAll(this, new FirebaseSyncHelper.SyncCallback() {
                    @Override
                    public void onSuccess(String message) {
                        runOnUiThread(() -> checkSettingAndProceed());
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                            checkSettingAndProceed();
                        });
                    }
                });
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Phát hiện đổi tải khoản")
                        .setMessage("Bạn vừa đăng nhập bằng tài khoản Cloud khác. Bạn muốn xử lý dữ liệu hiện đang có trên máy điện thoại này như thế nào?")
                        .setPositiveButton("Bảo lưu & Đồng bộ lên Cloud mới", (d, w) -> {
                            prefs.edit().putString("last_uid", newUid).apply();
                            Toast.makeText(this, "Đang sao lưu lên tài khoản mới...", Toast.LENGTH_SHORT).show();
                            FirebaseSyncHelper.backupAll(this, new FirebaseSyncHelper.SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    runOnUiThread(() -> checkSettingAndProceed());
                                }
                                @Override
                                public void onFailure(String error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                                        checkSettingAndProceed();
                                    });
                                }
                            });
                        })
                        .setNegativeButton("Xóa & Nạp dữ liệu Cloud mới về", (d, w) -> {
                            prefs.edit().putString("last_uid", newUid).apply();
                            Toast.makeText(this, "Đang tải dữ liệu từ đám mây...", Toast.LENGTH_SHORT).show();
                            FirebaseSyncHelper.restoreAll(this, new FirebaseSyncHelper.SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    runOnUiThread(() -> checkSettingAndProceed());
                                }
                                @Override
                                public void onFailure(String error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                                        checkSettingAndProceed();
                                    });
                                }
                            });
                        })
                        .setCancelable(false)
                        .show();
            }
        } else {
            checkSettingAndProceed();
        }
    }

    private void checkSettingAndProceed() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            SettingEntity s = db.settingDao().getSetting();
            runOnUiThread(() -> {
                if (s == null || !s.pinEnabled || s.pinCode == null || s.pinCode.isEmpty()) {
                    goToMain();
                } else {
                    showPinScreen(s.pinCode);
                }
            });
        });
    }

    private void showPinScreen(String correctPin) {
        layoutAuth.setVisibility(View.GONE);
        layoutPin.setVisibility(View.VISIBLE);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("M\u1edf Kh\u00f3a \u1ee8ng D\u1ee5ng");

        TextInputEditText edtPin = findViewById(R.id.edtPinInput);
        TextView tvError = findViewById(R.id.tvPinError);
        MaterialButton btnUnlock = findViewById(R.id.btnUnlock);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("\u0110\u0103ng xu\u1ea5t")
                .setMessage("T\u00e0i kho\u1ea3n \u0111\u00e1m m\u00e2y s\u1ebd b\u1ecb \u0111\u0103ng xu\u1ea5t. D\u1eef li\u1ec7u tr\u00ean m\u00e1y s\u1ebd t\u1ef1 \u0111\u1ed9ng x\u00f3a \u0111\u1ec3 b\u1ea3o m\u1eadt (d\u1eef li\u1ec7u tr\u00ean \u0111\u00e1m m\u00e2y v\u1eabn an to\u00e0n). B\u1ea1n c\u00f3 ch\u1eafc ch\u1eafn?")
                .setPositiveButton("\u0110\u0103ng xu\u1ea5t", (d, w) -> AppExecutors.getInstance().diskIO().execute(() -> {
                    AppDatabase.getInstance(this).clearAllTables();
                    runOnUiThread(() -> {
                        mAuth.signOut();
                        Toast.makeText(this, "\u0110\u00e3 \u0111\u0103ng xu\u1ea5t v\u00e0 x\u00f3a d\u1eef li\u1ec7u t\u1ea1m", Toast.LENGTH_SHORT).show();
                        setupAuthScreen();
                    });
                }))
                .setNegativeButton("H\u1ee7y", null)
                .show());

        btnUnlock.setOnClickListener(v -> {
            if (System.currentTimeMillis() < lockedUntil) {
                long secs = (lockedUntil - System.currentTimeMillis()) / 1000;
                tvError.setText("Nh\u1eadp sai qu\u00e1 nhi\u1ec1u. Th\u1eed l\u1ea1i sau " + secs + " gi\u00e2y.");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            String entered = edtPin.getText() != null ? edtPin.getText().toString().trim() : "";
            if (entered.equals(correctPin)) {
                goToMain();
            } else {
                failCount++;
                if (failCount >= 5) {
                    lockedUntil = System.currentTimeMillis() + 30_000;
                    failCount = 0;
                    tvError.setText("Sai PIN qu\u00e1 5 l\u1ea7n. B\u1ecb kh\u00f3a 30 gi\u00e2y.");
                } else {
                    tvError.setText("PIN kh\u00f4ng \u0111\u00fang. C\u00f2n " + (5 - failCount) + " l\u1ea7n th\u1eed.");
                }
                tvError.setVisibility(View.VISIBLE);
                edtPin.setText("");
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean isGmailEmail(String email) {
        if (email == null) return false;
        String e = email.trim().toLowerCase(Locale.ROOT);
        return e.matches("^[a-z0-9._%+-]+@gmail\\.com$");
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setGoogleLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setGoogleLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "\u0110\u0103ng nh\u1eadp Google th\u00e0nh c\u00f4ng!", Toast.LENGTH_SHORT).show();
                        checkPinRequired();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "L\u1ed7i kh\u00f4ng x\u00e1c \u0111\u1ecbnh";
                        Toast.makeText(LoginActivity.this, "Kh\u00f4ng th\u1ec3 x\u00e1c th\u1ef1c: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setGoogleLoading(boolean isLoading) {
        if (btnGoogleSignIn != null && progressGoogle != null) {
            if (isLoading) {
                btnGoogleSignIn.setText("");
                btnGoogleSignIn.setEnabled(false);
                progressGoogle.setVisibility(View.VISIBLE);
            } else {
                btnGoogleSignIn.setText("Ti\u1ebfp t\u1ee5c truy c\u1eadp v\u1edbi Google");
                btnGoogleSignIn.setEnabled(true);
                progressGoogle.setVisibility(View.GONE);
            }
        }
    }
}
