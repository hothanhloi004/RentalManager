package com.example.rentalmanager.ui.tenant;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.entity.TenantEntity;
import com.example.rentalmanager.util.ResultState;
import com.example.rentalmanager.viewmodel.TenantViewModel;

public class AddTenantDialog extends DialogFragment {

    private TenantViewModel viewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = getLayoutInflater().inflate(R.layout.dialog_add_tenant, null);

        EditText etName = view.findViewById(R.id.etName);
        EditText etPhone = view.findViewById(R.id.etPhone);
        EditText etCccd = view.findViewById(R.id.etCccd);
        EditText etAddress = view.findViewById(R.id.etAddress);

        viewModel = new ViewModelProvider(requireActivity())
                .get(TenantViewModel.class);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                String name = etName.getText().toString().trim();
                String phone = etPhone.getText().toString().trim();
                String cccd = etCccd.getText().toString().trim();
                String address = etAddress.getText().toString().trim();

                boolean hasError = false;

                if (name.isEmpty()) {
                    etName.setError("Không được để trống");
                    hasError = true;
                }

                if (phone.isEmpty()) {
                    etPhone.setError("Không được để trống");
                    hasError = true;
                }

                if (cccd.isEmpty()) {
                    etCccd.setError("Không được để trống");
                    hasError = true;
                }

                if (hasError) return;

                TenantEntity tenant = new TenantEntity();
                tenant.fullName = name;
                tenant.phone = phone;
                tenant.cccd = cccd;
                tenant.address = address;

                viewModel.insert(tenant)
                        .observe(this, result -> {

                            if (result.success) {
                                dialog.dismiss();
                            } else {

                                if (result.message.contains("CCCD")) {
                                    etCccd.setError(result.message);
                                } else if (result.message.contains("SĐT")) {
                                    etPhone.setError(result.message);
                                }
                            }
                        });
            });
        });

        return dialog;
    }
}
