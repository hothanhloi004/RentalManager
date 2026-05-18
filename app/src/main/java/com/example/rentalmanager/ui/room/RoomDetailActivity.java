package com.example.rentalmanager.ui.room;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rentalmanager.MainActivity;
import com.example.rentalmanager.R;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.model.RoomWithTenant;
import com.example.rentalmanager.ui.tenant.DocPhotoAdapter;
import com.example.rentalmanager.util.RoomStatus;
import com.example.rentalmanager.util.ImgbbUploader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RoomDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ROOM_ID = "room_id";

    private DocPhotoAdapter photoAdapter;
    private File photosDir;
    private int roomId;
    private Uri pendingPhotoUri;
    private File pendingPhotoFile;
    private String pendingPhotoCategoryPrefix;
    private TextView tvNoPhoto;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_detail);

        roomId = getIntent().getIntExtra(EXTRA_ROOM_ID, -1);
        if (roomId == -1) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        TextView tvRoomName = findViewById(R.id.tvRoomName);
        Chip chipRoomStatus = findViewById(R.id.chipRoomStatus);
        TextView tvRoomPrice = findViewById(R.id.tvRoomPrice);
        TextView tvTenantName = findViewById(R.id.tvTenantName);
        TextView tvContractDate = findViewById(R.id.tvContractDate);
        TextView tvRoomNote = findViewById(R.id.tvRoomNote);
        tvNoPhoto = findViewById(R.id.tvNoPhoto);

        photosDir = new File(getFilesDir(), "room_photos/room_" + roomId);
        if (!photosDir.exists()) {
            photosDir.mkdirs();
        }

        photoAdapter = new DocPhotoAdapter();
        RecyclerView recyclerPhotos = findViewById(R.id.recyclerPhotos);
        GridLayoutManager photoLayoutManager = new GridLayoutManager(this, 3);
        photoLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return photoAdapter.getSpanSize(position, photoLayoutManager.getSpanCount());
            }
        });
        recyclerPhotos.setLayoutManager(photoLayoutManager);
        recyclerPhotos.setAdapter(photoAdapter);
        recyclerPhotos.setNestedScrollingEnabled(false);

        photoAdapter.setListener(new DocPhotoAdapter.Listener() {
            @Override
            public void onDelete(File file) {
                new AlertDialog.Builder(RoomDetailActivity.this)
                        .setTitle("Xóa ảnh")
                        .setMessage("Bạn có chắc muốn xóa ảnh này không?")
                        .setPositiveButton("Xóa", (d, w) -> {
                            if (file.delete()) {
                                photoAdapter.removePhoto(file);
                                updateEmptyState();
                                Toast.makeText(RoomDetailActivity.this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onView(File file) {
                Dialog dialog = new Dialog(
                        RoomDetailActivity.this,
                        android.R.style.Theme_Black_NoTitleBar_Fullscreen
                );
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_view_photo);
                ImageView img = dialog.findViewById(R.id.imgFullscreen);
                ImageButton btnClose = dialog.findViewById(R.id.btnClosePhoto);
                Glide.with(RoomDetailActivity.this).load(file).into(img);
                btnClose.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
        });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingPhotoFile != null) {
                        photoAdapter.addPhoto(pendingPhotoFile);
                        updateEmptyState();
                        if ("TAG_WEB".equals(pendingPhotoCategoryPrefix)) {
                            uploadRoomPhotoToFirebase(pendingPhotoFile, roomId);
                        }
                    }
                    pendingPhotoCategoryPrefix = null;
                }
        );

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            String fileName = buildPhotoFileName("gallery");
                            File dest = new File(photosDir, fileName);
                            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                                 java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                                if (in != null) {
                                    byte[] buf = new byte[4096];
                                    int n;
                                    while ((n = in.read(buf)) != -1) {
                                        out.write(buf, 0, n);
                                    }
                                }
                            }
                            photoAdapter.addPhoto(dest);
                            updateEmptyState();
                            if ("TAG_WEB".equals(pendingPhotoCategoryPrefix)) {
                                uploadRoomPhotoToFirebase(dest, roomId);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Lỗi nhập ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        } finally {
                            pendingPhotoCategoryPrefix = null;
                        }
                    }
                }
        );

        View btnTakePhoto = findViewById(R.id.btnTakePhoto);
        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> showPhotoCategoryDialog());
        }

        AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
            List<RoomWithTenant> rooms = db.roomDao().getRoomsWithTenantSync();
            RoomWithTenant room = null;
            if (rooms != null) {
                for (RoomWithTenant r : rooms) {
                    if (r.roomId == roomId) {
                        room = r;
                        break;
                    }
                }
            }
            if (room == null) {
                runOnUiThread(this::finish);
                return;
            }

            List<File> photos = loadPhotos();
            RoomWithTenant finalRoom = room;
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

            runOnUiThread(() -> {
                tvRoomName.setText(finalRoom.roomName);
                tvRoomPrice.setText("\uD83D\uDCB0 " + nf.format(finalRoom.price) + " đ/tháng");

                if (RoomStatus.TRONG.equals(finalRoom.status)) {
                    chipRoomStatus.setText("Trống");
                    chipRoomStatus.setChipBackgroundColorResource(R.color.status_vacant_bg);
                    chipRoomStatus.setTextColor(ContextCompat.getColor(this, R.color.status_vacant));
                } else if (RoomStatus.BAO_TRI.equals(finalRoom.status)) {
                    chipRoomStatus.setText("Bảo trì");
                    chipRoomStatus.setChipBackgroundColorResource(R.color.status_maintenance_bg);
                    chipRoomStatus.setTextColor(ContextCompat.getColor(this, R.color.status_maintenance));
                } else {
                    chipRoomStatus.setText("Đang thuê");
                    chipRoomStatus.setChipBackgroundColorResource(R.color.status_active_bg);
                    chipRoomStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active));
                }

                if (finalRoom.tenantName != null && finalRoom.tenantId != null) {
                    tvTenantName.setText("\uD83D\uDC64 Khách: " + finalRoom.tenantName);
                    tvTenantName.setPaintFlags(tvTenantName.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                    tvTenantName.setTextColor(ContextCompat.getColor(this, R.color.primary));
                    tvTenantName.setOnClickListener(v -> {
                        Intent intent = new Intent(
                                RoomDetailActivity.this,
                                com.example.rentalmanager.ui.tenant.TenantDetailActivity.class
                        );
                        intent.putExtra(
                                com.example.rentalmanager.ui.tenant.TenantDetailActivity.EXTRA_TENANT_ID,
                                finalRoom.tenantId
                        );
                        startActivity(intent);
                    });
                } else {
                    tvTenantName.setText("\uD83D\uDC64 Chưa có người thuê");
                    tvTenantName.setPaintFlags(tvTenantName.getPaintFlags() & (~android.graphics.Paint.UNDERLINE_TEXT_FLAG));
                    tvTenantName.setTextColor(ContextCompat.getColor(this, R.color.on_surface));
                    tvTenantName.setOnClickListener(null);
                }

                if (finalRoom.startDate != null) {
                    String ds = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(new Date(finalRoom.startDate));
                    tvContractDate.setText("\uD83D\uDCC5 Hợp đồng từ: " + ds);
                    tvContractDate.setVisibility(View.VISIBLE);
                } else {
                    tvContractDate.setVisibility(View.GONE);
                }

                if (finalRoom.note != null && !finalRoom.note.isEmpty()) {
                    tvRoomNote.setText("\uD83D\uDCDD " + finalRoom.note);
                    tvRoomNote.setVisibility(View.VISIBLE);
                } else {
                    tvRoomNote.setVisibility(View.GONE);
                }

                photoAdapter.setPhotos(photos);
                updateEmptyState();

                MaterialButton btnCreateContractForRoom = findViewById(R.id.btnCreateContractForRoom);
                if (btnCreateContractForRoom != null) {
                    boolean isVacant = RoomStatus.TRONG.equals(finalRoom.status);
                    btnCreateContractForRoom.setVisibility(isVacant ? View.VISIBLE : View.GONE);
                    btnCreateContractForRoom.setOnClickListener(v -> openCreateContractFlow(finalRoom.roomId));
                }

                MaterialButton btnEditRoom = findViewById(R.id.btnEditRoom);
                if (btnEditRoom != null) {
                    btnEditRoom.setOnClickListener(v -> {
                        View dialogView = LayoutInflater.from(RoomDetailActivity.this)
                                .inflate(R.layout.dialog_add_room, null);

                        android.widget.EditText edtName = dialogView.findViewById(R.id.edtRoomName);
                        android.widget.EditText edtPrice = dialogView.findViewById(R.id.edtRoomPrice);
                        android.widget.EditText edtNote = dialogView.findViewById(R.id.edtRoomNote);
                        com.google.android.material.switchmaterial.SwitchMaterial switchMaintenance =
                                dialogView.findViewById(R.id.switchMaintenance);
                        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);

                        com.example.rentalmanager.util.CurrencyInputHelper.attach(edtPrice);

                        edtName.setText(finalRoom.roomName);
                        if (finalRoom.price > 0) {
                            edtPrice.setText(NumberFormat.getInstance(new Locale("vi", "VN")).format(finalRoom.price));
                        } else {
                            edtPrice.setText("0");
                        }
                        edtNote.setText(finalRoom.note);

                        if (RoomStatus.DANG_THUE.equals(finalRoom.status)) {
                            edtName.setEnabled(false);
                            edtPrice.setEnabled(false);
                            if (tvTitle != null) {
                                tvTitle.setText("Sửa ghi chú phòng");
                            }
                        } else {
                            switchMaintenance.setVisibility(View.VISIBLE);
                            switchMaintenance.setChecked(RoomStatus.BAO_TRI.equals(finalRoom.status));
                            if (tvTitle != null) {
                                tvTitle.setText("Chỉnh sửa phòng");
                            }
                        }

                        new AlertDialog.Builder(RoomDetailActivity.this)
                                .setView(dialogView)
                                .setPositiveButton("Cập nhật", (dialog, which) -> {
                                    String name = edtName.getText().toString().trim();
                                    String priceStr = edtPrice.getText().toString().replaceAll("[^\\d]", "");
                                    String note = edtNote.getText().toString().trim();

                                    if (name.isEmpty() || priceStr.isEmpty()) {
                                        Toast.makeText(RoomDetailActivity.this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    double price = Double.parseDouble(priceStr);
                                    com.example.rentalmanager.data.entity.RoomEntity updated =
                                            new com.example.rentalmanager.data.entity.RoomEntity();
                                    updated.roomId = finalRoom.roomId;
                                    updated.roomName = name;
                                    updated.price = price;
                                    updated.note = note;
                                    updated.status = RoomStatus.DANG_THUE.equals(finalRoom.status)
                                            ? RoomStatus.DANG_THUE
                                            : (switchMaintenance.isChecked() ? RoomStatus.BAO_TRI : RoomStatus.TRONG);

                                    new Thread(() -> {
                                        db.roomDao().update(updated);
                                        runOnUiThread(this::recreate);
                                    }).start();
                                })
                                .setNegativeButton("Hủy", null)
                                .show();
                    });
                }
            });
        }).start();
    }

    private void showPhotoCategoryDialog() {
        String[] categories = {"Ảnh đăng web", "Ảnh trước thuê", "Ảnh đồng hồ", "Khác"};
        new AlertDialog.Builder(this)
                .setTitle("Chọn loại ảnh")
                .setItems(categories, (d, w) -> {
                    if (w == 0) {
                        pendingPhotoCategoryPrefix = "TAG_WEB";
                    } else if (w == 1) {
                        pendingPhotoCategoryPrefix = "TAG_TRUOCTHUE";
                    } else if (w == 2) {
                        pendingPhotoCategoryPrefix = "TAG_DONGHO";
                    } else {
                        pendingPhotoCategoryPrefix = "TAG_KHAC";
                    }
                    showPhotoSourceDialog();
                })
                .show();
    }

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thêm ảnh")
                .setItems(new String[]{"📷 Chụp ảnh mới", "🖼️ Chọn từ thư viện"}, (d, w) -> {
                    if (w == 0) {
                        launchCamera();
                    } else {
                        pickImageLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void launchCamera() {
        String fileName = buildPhotoFileName("camera");
        pendingPhotoFile = new File(photosDir, fileName);
        pendingPhotoUri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".provider",
                pendingPhotoFile
        );
        takePictureLauncher.launch(pendingPhotoUri);
    }

    private String buildPhotoFileName(String source) {
        String prefix = pendingPhotoCategoryPrefix == null ? "TAG_KHAC" : pendingPhotoCategoryPrefix;
        return prefix + "_" + source + "_" + System.currentTimeMillis() + ".jpg";
    }

    private List<File> loadPhotos() {
        List<File> result = new ArrayList<>();
        if (photosDir.exists()) {
            File[] files = photosDir.listFiles(f -> f.getName().endsWith(".jpg"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                result.addAll(Arrays.asList(files));
            }
        }
        return result;
    }

    private void updateEmptyState() {
        if (tvNoPhoto != null) {
            tvNoPhoto.setVisibility(photoAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void uploadRoomPhotoToFirebase(File file, int roomId) {
        if (file == null || !file.exists()) return;

        ImgbbUploader.upload(file, new ImgbbUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                Log.d("RoomDetailActivity", "Photo uploaded, URL: " + imageUrl);
                new Thread(() -> {
                    AppDatabase db = AppDatabase.getInstance(RoomDetailActivity.this);
                    com.example.rentalmanager.data.entity.RoomEntity room =
                            db.roomDao().getRoomByIdSync(roomId);
                    if (room != null) {
                        room.imageUrl = imageUrl;
                        db.roomDao().update(room);
                        com.example.rentalmanager.util.FirebaseSyncHelper.updateRoomToCloud(room);
                        Log.d("RoomDetailActivity", "imageUrl saved to local DB and Cloud for room " + roomId);
                    }
                }).start();
            }

            @Override
            public void onFailure(String error) {
                Log.e("RoomDetailActivity", "Photo upload failed: " + error);
            }
        });
    }


    private void openCreateContractFlow(int selectedRoomId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_CREATE_CONTRACT);
        intent.putExtra(MainActivity.EXTRA_PRESELECT_ROOM_ID, selectedRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
