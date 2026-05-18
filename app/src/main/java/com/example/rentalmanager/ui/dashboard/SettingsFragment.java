package com.example.rentalmanager.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.SettingEntity;
import com.example.rentalmanager.ui.login.LoginActivity;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.util.CurrencyInputHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SettingsFragment extends Fragment {

    private TextInputEditText edtElectric, edtWater, edtTrash, edtWifi, edtService, edtPin;
    private TextInputEditText edtBankCode, edtBankAccount;
    private TextInputEditText edtHostelName, edtLandlordName, edtLandlordPhone, edtHostelAddress;
    private TextInputLayout pinLayout;
    private SwitchMaterial switchPin;
    private SwitchMaterial switchDarkMode;

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        edtElectric = view.findViewById(R.id.edtElectricPrice);
        edtWater = view.findViewById(R.id.edtWaterPrice);
        edtTrash = view.findViewById(R.id.edtTrashFee);
        edtWifi = view.findViewById(R.id.edtWifiPrice);
        edtService = view.findViewById(R.id.edtServiceFee);
        edtPin = view.findViewById(R.id.edtPin);
        pinLayout = view.findViewById(R.id.pinLayout);
        switchPin = view.findViewById(R.id.switchPin);
        edtBankCode = view.findViewById(R.id.edtBankCode);
        edtBankAccount = view.findViewById(R.id.edtBankAccount);
        edtHostelName = view.findViewById(R.id.edtHostelName);
        edtLandlordName = view.findViewById(R.id.edtLandlordName);
        edtLandlordPhone = view.findViewById(R.id.edtLandlordPhone);
        edtHostelAddress = view.findViewById(R.id.edtHostelAddress);

        android.widget.ImageButton btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        switchPin.setOnCheckedChangeListener((btn, isChecked) ->
                pinLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            SettingEntity s = db.settingDao().getSetting();
            if (s == null) s = new SettingEntity();
            final SettingEntity setting = s;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                edtElectric.setText(setting.electricPrice > 0 ? nf.format(setting.electricPrice) : "");
                edtWater.setText(setting.waterPrice > 0 ? nf.format(setting.waterPrice) : "");
                edtTrash.setText(setting.trashFee > 0 ? nf.format(setting.trashFee) : "");
                edtWifi.setText(setting.wifiPrice > 0 ? nf.format(setting.wifiPrice) : "");
                edtService.setText(setting.serviceFee > 0 ? nf.format(setting.serviceFee) : "");
                switchPin.setChecked(setting.pinEnabled);
                if (setting.pinEnabled) {
                    pinLayout.setVisibility(View.VISIBLE);
                    edtPin.setText(setting.pinCode);
                }
                edtBankCode.setText(setting.bankCode != null ? setting.bankCode : "MB");
                edtBankAccount.setText(setting.bankAccount != null ? setting.bankAccount : "");
                edtHostelName.setText(setting.hostelName != null ? setting.hostelName : "");
                edtLandlordName.setText(setting.landlordName != null ? setting.landlordName : "");
                edtLandlordPhone.setText(setting.landlordPhone != null ? setting.landlordPhone : "");
                edtHostelAddress.setText(setting.hostelAddress != null ? setting.hostelAddress : "");
            });
        });

        MaterialButton btnSave = view.findViewById(R.id.btnSaveSettings);
        btnSave.setOnClickListener(v -> saveSettings());

        // Dark Mode toggle
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDark);
        switchDarkMode.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(KEY_DARK_MODE, checked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });

        addCurrencyFormatter(edtElectric);
        addCurrencyFormatter(edtWater);
        addCurrencyFormatter(edtTrash);
        addCurrencyFormatter(edtWifi);
        addCurrencyFormatter(edtService);

        return view;
    }

    private void addCurrencyFormatter(TextInputEditText edt) {
        CurrencyInputHelper.attach(edt);
    }

    private void saveSettings() {
        String sElec = edtElectric.getText().toString().trim();
        String sWater = edtWater.getText().toString().trim();
        String sTrash = edtTrash.getText().toString().trim();
        String sWifi = edtWifi.getText().toString().trim();
        String sService = edtService.getText().toString().trim();

        if (sElec.isEmpty() || sWater.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập giá điện và nước", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean pinOn = switchPin.isChecked();
        String pin = edtPin.getText() != null ? edtPin.getText().toString().trim() : "";
        if (pinOn && pin.length() < 4) {
            Toast.makeText(getContext(), "Mã PIN phải có ít nhất 4 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            SettingEntity s = db.settingDao().getSetting();
            if (s == null) {
                s = new SettingEntity();
                s.electricPrice = parseDouble(sElec);
                s.waterPrice = parseDouble(sWater);
                s.trashFee = parseDouble(sTrash);
                s.wifiPrice = parseDouble(sWifi);
                s.serviceFee = parseDouble(sService);
                s.pinEnabled = pinOn;
                s.pinCode = pin;
                s.bankCode = edtBankCode.getText() != null ? edtBankCode.getText().toString().trim() : "MB";
                s.bankAccount = edtBankAccount.getText() != null ? edtBankAccount.getText().toString().trim() : "";
                s.hostelName = edtHostelName.getText() != null ? edtHostelName.getText().toString().trim() : "";
                s.landlordName = edtLandlordName.getText() != null ? edtLandlordName.getText().toString().trim() : "";
                s.landlordPhone = edtLandlordPhone.getText() != null ? edtLandlordPhone.getText().toString().trim() : "";
                s.hostelAddress = edtHostelAddress.getText() != null ? edtHostelAddress.getText().toString().trim() : "";
                db.settingDao().insert(s);
            } else {
                s.electricPrice = parseDouble(sElec);
                s.waterPrice = parseDouble(sWater);
                s.trashFee = parseDouble(sTrash);
                s.wifiPrice = parseDouble(sWifi);
                s.serviceFee = parseDouble(sService);
                s.pinEnabled = pinOn;
                s.pinCode = pin;
                s.bankCode = edtBankCode.getText() != null ? edtBankCode.getText().toString().trim() : "MB";
                s.bankAccount = edtBankAccount.getText() != null ? edtBankAccount.getText().toString().trim() : "";
                s.hostelName = edtHostelName.getText() != null ? edtHostelName.getText().toString().trim() : "";
                s.landlordName = edtLandlordName.getText() != null ? edtLandlordName.getText().toString().trim() : "";
                s.landlordPhone = edtLandlordPhone.getText() != null ? edtLandlordPhone.getText().toString().trim() : "";
                s.hostelAddress = edtHostelAddress.getText() != null ? edtHostelAddress.getText().toString().trim() : "";
                db.settingDao().update(s);
            }
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Đã lưu cài đặt!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            });
        });
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void confirmClearLocalData() {
        if (!isAdded()) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa dữ liệu trên máy")
                .setMessage("Tất cả dữ liệu hiện có trên thiết bị này sẽ bị xóa. Dữ liệu trên Cloud không bị xóa.")
                .setPositiveButton("Xóa", (dialog, which) -> clearLocalData())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void clearLocalData() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase.getInstance(requireContext()).clearAllTables();

            if (!isAdded()) return;
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Đã xóa dữ liệu trên máy", Toast.LENGTH_SHORT).show();
                android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        });
    }
}
