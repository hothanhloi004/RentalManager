package com.example.rentalmanager.ui.dashboard;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rentalmanager.R;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.viewmodel.DashboardViewModel;
import com.example.rentalmanager.ui.report.ReportFragment;
import android.util.Log;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.widget.ImageButton;
public class DashboardFragment extends Fragment {
    private TextView tvOverdueBills;
    private TextView tvMeterWarning;
    private DashboardViewModel vm;

    private TextView tvMonth;
    private TextView tvRevenue, tvDebt, tvRooms, tvBills, tvElectric, tvWater;

    private ImageButton btnPrevMonth, btnNextMonth;

    private Calendar calendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvRevenue = v.findViewById(R.id.tvRevenue);
        tvDebt = v.findViewById(R.id.tvDebt);
        tvRooms = v.findViewById(R.id.tvRooms);
        tvBills = v.findViewById(R.id.tvBills);
        tvElectric = v.findViewById(R.id.tvElectric);
        tvWater = v.findViewById(R.id.tvWater);
        tvMonth = v.findViewById(R.id.tvMonth);
        tvOverdueBills = v.findViewById(R.id.tvOverdueBills);
        tvMeterWarning = v.findViewById(R.id.tvMeterWarning);
        btnPrevMonth = v.findViewById(R.id.btnPrevMonth);
        btnNextMonth = v.findViewById(R.id.btnNextMonth);

        TextView tvHeaderApp = v.findViewById(R.id.tvHeaderApp);
        TextView tvHeaderName = v.findViewById(R.id.tvHeaderName);

        AppExecutors.getInstance().diskIO().execute(() -> {
            com.example.rentalmanager.data.entity.SettingEntity s =
                com.example.rentalmanager.data.database.AppDatabase.getInstance(requireContext()).settingDao().getSetting();
            if (s != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    if (s.hostelName != null && !s.hostelName.trim().isEmpty()) {
                        String cleanName = s.hostelName.replace("\n", " ").replace("\r", " ").trim();
                        tvHeaderApp.setText(cleanName + " 👋");
                    }
                    if (s.landlordName != null && !s.landlordName.trim().isEmpty()) {
                        tvHeaderName.setText("Chủ trọ: " + s.landlordName);
                    }
                });
            }
        });

        vm = new ViewModelProvider(this).get(DashboardViewModel.class);

        NumberFormat f = NumberFormat.getInstance(new Locale("vi","VN"));

        vm.stats.observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;

            tvRevenue.setText(f.format(s.revenue) + " đ");
            tvDebt.setText(f.format(s.debt) + " đ");

            tvRooms.setText(String.valueOf(s.rentingRooms));
            tvBills.setText(String.valueOf(s.unpaidBills));

            tvElectric.setText(s.electricUsed + " kWh");
            tvWater.setText(s.waterUsed + " m³");
            tvOverdueBills.setText(String.valueOf(s.overdueBills));
            tvMeterWarning.setText(String.valueOf(s.meterNotUpdatedBills));
        });

        ImageButton btnNotification = v.findViewById(R.id.btnNotification);
        TextView tvNotifBadge = v.findViewById(R.id.tvNotifBadge);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v1 -> {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, new NotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });
            
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String uid = auth.getCurrentUser().getUid();
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("inquiries").document(uid).collection("requests")
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null) return;
                        if (!isAdded() || getContext() == null) return;
                        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("rental_manager_notifications", android.content.Context.MODE_PRIVATE);
                        java.util.Set<String> readIds = prefs.getStringSet("read_inquiry_ids", new java.util.HashSet<>());
                        int unreadCount = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            if (!readIds.contains(doc.getId())) unreadCount++;
                        }
                        final int finalUnreadCount = unreadCount;
                        requireActivity().runOnUiThread(() -> {
                            if (tvNotifBadge != null) {
                                if (finalUnreadCount > 0) {
                                    tvNotifBadge.setVisibility(View.VISIBLE);
                                    tvNotifBadge.setText(finalUnreadCount > 9 ? "9+" : String.valueOf(finalUnreadCount));
                                    btnNotification.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                                } else {
                                    tvNotifBadge.setVisibility(View.GONE);
                                    btnNotification.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#C7D2FE")));
                                }
                            }
                        });
                    });
            }
        }

        btnPrevMonth.setOnClickListener(v1 -> {
            Log.d("DASHBOARD","PREV CLICK");
            calendar.add(Calendar.MONTH, -1);
            updateDashboard();
        });

        btnNextMonth.setOnClickListener(v12 -> {
            Log.d("DASHBOARD","NEXT CLICK");
            calendar.add(Calendar.MONTH, 1);
            updateDashboard();
        });

        ImageButton btnLogout = v.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(view -> {
                // BƯỚC 1: Nhắc sao lưu trước khi đăng xuất
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ Trước khi đăng xuất")
                    .setMessage("Dữ liệu trên máy sẽ bị xoá sau khi đăng xuất.\n\nBạn có muốn SAO LƯU lên Cloud trước không?\n(Khuyến nghị để không mất dữ liệu)")
                    .setCancelable(false)
                    .setPositiveButton("Sao lưu rồi đăng xuất", (d, w) -> {
                        Toast.makeText(requireContext(), "⏳ Đang sao lưu...", Toast.LENGTH_SHORT).show();
                        com.example.rentalmanager.util.FirebaseSyncHelper.backupAll(
                            requireContext(),
                            new com.example.rentalmanager.util.FirebaseSyncHelper.SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                            .setTitle("Sao lưu thành công!")
                                            .setMessage(message + "\n\nBây giờ sẽ đăng xuất.")
                                            .setCancelable(false)
                                            .setPositiveButton("Đăng xuất", (d2, w2) -> performLogout())
                                            .show();
                                    });
                                }
                                @Override
                                public void onFailure(String error) {
                                    if (!isAdded()) return;
                                    requireActivity().runOnUiThread(() -> {
                                        if (!isAdded()) return;
                                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                            .setTitle("Sao lưu thất bại")
                                            .setMessage(error + "\n\nBạn vẫn muốn đăng xuất không?")
                                            .setPositiveButton("Vẫn đăng xuất", (d2, w2) -> performLogout())
                                            .setNegativeButton("Ở lại", null)
                                            .show();
                                    });
                                }
                            }
                        );
                    })
                    .setNeutralButton("Đăng xuất không sao lưu", (d, w) ->
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Xác nhận đăng xuất")
                            .setMessage("Dữ liệu trên máy sẽ BỊ XOÁ và không thể khôi phục nếu chưa sao lưu.\n\nBạn chắc chắn muốn tiếp tục?")
                            .setPositiveButton("Đăng xuất", (d2, w2) -> performLogout())
                            .setNegativeButton("Hủy", null)
                            .show()
                    )
                    .setNegativeButton("Hủy", null)
                    .show();

            });
        }

        ImageButton btnSettings = v.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(view -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        ImageButton btnReport = v.findViewById(R.id.btnReport);
        if (btnReport != null) {
            btnReport.setOnClickListener(view -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ReportFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        ImageButton btnSync = v.findViewById(R.id.btnSyncCloud);
        if (btnSync != null) {
            btnSync.setOnClickListener(view -> {
                String[] options = {"📤 Đẩy dữ liệu lên Cloud (Sao lưu)", "📥 Tải dữ liệu về máy (Phục hồi)"};
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Đồng bộ dữ liệu")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                    // BACKUP
                    android.app.ProgressDialog progress = new android.app.ProgressDialog(requireContext());
                    progress.setMessage("⏳ Đang sao lưu lên Đám Mây...\nVui lòng chờ trong giây lát.");
                    progress.setCancelable(false);
                    progress.show();
                    com.example.rentalmanager.util.FirebaseSyncHelper.backupAll(
                        requireContext(),
                        new com.example.rentalmanager.util.FirebaseSyncHelper.SyncCallback() {
                            @Override
                            public void onSuccess(String message) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    if (!isAdded()) return;
                                    progress.dismiss();
                                    // Lưu thời điểm sao lưu thành công
                                    long now = System.currentTimeMillis();
                                    requireContext().getSharedPreferences("rental_manager_notifications", android.content.Context.MODE_PRIVATE)
                                        .edit().putLong("last_backup_time", now).apply();
                                    String timeStr = new java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date(now));
                                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("✅ Sao lưu thành công!")
                                        .setMessage(message + "\n\n🕒 Lúc: " + timeStr)
                                        .setPositiveButton("OK", null)
                                        .show();
                                });
                            }
                            @Override
                            public void onFailure(String error) {
                                if (!isAdded()) return;
                                requireActivity().runOnUiThread(() -> {
                                    if (!isAdded()) return;
                                    progress.dismiss();
                                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("❌ Sao lưu thất bại")
                                        .setMessage(error)
                                        .setPositiveButton("OK", null)
                                        .show();
                                });
                            }
                        }
                    );
                            } else if (which == 1) {
                                // RESTORE
                                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Cảnh báo khôi phục")
                                    .setMessage("Toàn bộ dữ liệu hiện tại trên máy sẽ BỊ XOÁ và ghi đè bằng dữ liệu từ Cloud. Bạn có chắc chắn không?")
                                    .setPositiveButton("Chắc chắn tải về", (d, w) -> {
                                        Toast.makeText(requireContext(), "⏳ Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
                                        com.example.rentalmanager.util.FirebaseSyncHelper.restoreAll(
                                            requireContext(),
                                            new com.example.rentalmanager.util.FirebaseSyncHelper.SyncCallback() {
                                                @Override
                                                public void onSuccess(String message) {
                                                    if (!isAdded()) return;
                                                    requireActivity().runOnUiThread(() -> {
                                                        if (!isAdded()) return;
                                                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                                            .setTitle("Phục hồi thành công!")
                                                            .setMessage(message)
                                                            .setPositiveButton("Khởi động lại", (d2, w2) -> {
                                                                android.content.Intent intent = new android.content.Intent(requireContext(), com.example.rentalmanager.ui.login.LoginActivity.class);
                                                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                startActivity(intent);
                                                                Runtime.getRuntime().exit(0);
                                                            })
                                                            .setCancelable(false)
                                                            .show();
                                                    });
                                                }
                                                @Override
                                                public void onFailure(String error) {
                                                    if (!isAdded()) return;
                                                    requireActivity().runOnUiThread(() -> {
                                                        if (!isAdded()) return;
                                                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                                                    });
                                                }
                                            }
                                        );
                                    })
                                    .setNegativeButton("Hủy", null)
                                    .show();
                            }
                        })
                        .show();
            });
        }

        updateDashboard();

        return v;
    }

    private void updateDashboard() {

        SimpleDateFormat dbFormat =
                new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        SimpleDateFormat displayFormat =
                new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        String monthDB = dbFormat.format(calendar.getTime());
        String monthDisplay = displayFormat.format(calendar.getTime());

        tvMonth.setText("Tháng " + monthDisplay);

        vm.loadDashboard(monthDB);
    }

    private void performLogout() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            com.example.rentalmanager.data.database.AppDatabase
                    .getInstance(requireContext()).clearAllTables();
            requireActivity().runOnUiThread(() -> {
                requireContext()
                        .getSharedPreferences("rm_prefs", android.content.Context.MODE_PRIVATE)
                        .edit().remove("last_uid").apply();
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                android.content.Intent intent = new android.content.Intent(
                        requireContext(),
                        com.example.rentalmanager.ui.login.LoginActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        });
    }
}
