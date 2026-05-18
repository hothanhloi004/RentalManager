package com.example.rentalmanager.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.model.InquiryModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationFragment extends Fragment {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private Chip chipAll, chipUnread, chipRead;

    private List<InquiryModel> allInquiries = new ArrayList<>();
    private Set<String> readIds = new HashSet<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private com.google.firebase.firestore.ListenerRegistration snapshotListener;

    private static final String PREFS_NAME = "rental_manager_notifications";
    private static final String KEY_READ_IDS = "read_inquiry_ids";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        chipAll = view.findViewById(R.id.chipAll);
        chipUnread = view.findViewById(R.id.chipUnread);
        chipRead = view.findViewById(R.id.chipRead);

        loadReadIds();

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        MaterialButton btnClearAll = view.findViewById(R.id.btnClearAll);
        btnClearAll.setOnClickListener(v -> clearAllNotifications());

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter(requireContext(), new NotificationAdapter.OnNotificationClickListener() {
            @Override
            public void onMarkReadClick(InquiryModel item) {
                markAsRead(item);
            }

            @Override
            public void onDeleteClick(InquiryModel item) {
                deleteNotification(item);
            }
        });
        rvNotifications.setAdapter(adapter);

        chipAll.setOnClickListener(v -> {
            chipAll.setChecked(true);
            chipUnread.setChecked(false);
            chipRead.setChecked(false);
            filterList("ALL");
        });
        chipUnread.setOnClickListener(v -> {
            chipAll.setChecked(false);
            chipUnread.setChecked(true);
            chipRead.setChecked(false);
            filterList("UNREAD");
        });
        chipRead.setOnClickListener(v -> {
            chipAll.setChecked(false);
            chipUnread.setChecked(false);
            chipRead.setChecked(true);
            filterList("READ");
        });

        fetchNotifications();

        return view;
    }

    private void loadReadIds() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        readIds = new HashSet<>(prefs.getStringSet(KEY_READ_IDS, new HashSet<>()));
    }

    private void saveReadIds() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_READ_IDS, readIds).apply();
    }

    private void fetchNotifications() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        if (snapshotListener != null) {
            snapshotListener.remove();
        }

        snapshotListener = db.collection("inquiries").document(uid).collection("requests")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Lỗi tải thông báo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        allInquiries.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            InquiryModel inq = doc.toObject(InquiryModel.class);
                            inq.setId(doc.getId());
                            inq.setRead(readIds.contains(inq.getId()));
                            allInquiries.add(inq);
                        }
                        Collections.sort(allInquiries, (a, b) -> {
                            long tA = a.getCreatedAt() != null ? a.getCreatedAt().getSeconds() : 0;
                            long tB = b.getCreatedAt() != null ? b.getCreatedAt().getSeconds() : 0;
                            return Long.compare(tB, tA); // descending
                        });
                        
                        progressBar.setVisibility(View.GONE);
                        filterList(getCurrentFilter());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (snapshotListener != null) {
            snapshotListener.remove();
            snapshotListener = null;
        }
    }

    private String getCurrentFilter() {
        if (chipUnread.isChecked()) return "UNREAD";
        if (chipRead.isChecked()) return "READ";
        return "ALL";
    }

    private void filterList(String filter) {
        List<InquiryModel> filtered = new ArrayList<>();
        for (InquiryModel inq : allInquiries) {
            inq.setRead(readIds.contains(inq.getId()));
            if ("ALL".equals(filter)) {
                filtered.add(inq);
            } else if ("READ".equals(filter) && inq.isRead()) {
                filtered.add(inq);
            } else if ("UNREAD".equals(filter) && !inq.isRead()) {
                filtered.add(inq);
            }
        }

        if (filtered.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.submitList(filtered);
        }
    }

    private void markAsRead(InquiryModel item) {
        readIds.add(item.getId());
        saveReadIds();
        filterList(getCurrentFilter());
    }

    private void deleteNotification(InquiryModel item) {
        if (auth.getCurrentUser() == null || getContext() == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa thông báo")
            .setMessage("Bạn có chắc muốn xóa thông báo này không?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                String uid = auth.getCurrentUser().getUid();
                db.collection("inquiries").document(uid).collection("requests").document(item.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            allInquiries.removeIf(inq -> inq.getId().equals(item.getId()));
                            filterList(getCurrentFilter());
                            Toast.makeText(getContext(), "Đã xóa thông báo", Toast.LENGTH_SHORT).show();
                        });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void clearAllNotifications() {
        if (auth.getCurrentUser() == null || allInquiries.isEmpty() || getContext() == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa tất cả thông báo")
            .setMessage("Bạn có chắc muốn xóa toàn bộ thông báo? Dữ liệu thực sự trên hệ thống sẽ không bị mất.")
            .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                String uid = auth.getCurrentUser().getUid();
                Toast.makeText(getContext(), "Đang xóa tất cả...", Toast.LENGTH_SHORT).show();
                
                for (InquiryModel item : allInquiries) {
                    db.collection("inquiries").document(uid).collection("requests").document(item.getId()).delete();
                }
                allInquiries.clear();
                filterList(getCurrentFilter());
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
