package com.example.rentalmanager.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyInputHelper {

    private static final Locale VIETNAM = new Locale("vi", "VN");

    private CurrencyInputHelper() {
    }

    public static void attach(EditText editText) {
        if (editText == null) {
            return;
        }

        editText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    return;
                }

                isUpdating = true;
                try {
                    String clean = digitsOnly(s == null ? "" : s.toString());
                    if (clean.isEmpty()) {
                        editText.setText("");
                    } else {
                        String formatted = NumberFormat.getInstance(VIETNAM)
                                .format(Long.parseLong(clean));
                        editText.setText(formatted);
                        editText.setSelection(formatted.length());
                    }
                } catch (Exception ignored) {
                }
                isUpdating = false;
            }
        });
    }

    public static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("[^\\d]", "");
    }
}
