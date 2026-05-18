package com.example.rentalmanager.ui.tenant;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.rentalmanager.R;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.TenantEntity;
import com.example.rentalmanager.data.model.BillWithInfo;
import com.example.rentalmanager.ui.bill.BillAdapter;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.util.CurrencyInputHelper;
import com.example.rentalmanager.util.ImgbbUploader;
import com.example.rentalmanager.viewmodel.BillViewModel;

import android.net.Uri;
import android.text.InputType;
import android.text.TextUtils;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import androidx.appcompat.widget.Toolbar;

public class TenantDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TENANT_ID = "tenant_id";

    private BillViewModel billViewModel;
    private DocPhotoAdapter docAdapter;
    private File docsDir;
    private int tenantId;
    private Uri pendingPhotoUri;
    private File pendingPhotoFile;
    private TextView tvNoDoc;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_detail);

        tenantId = getIntent().getIntExtra(EXTRA_TENANT_ID, -1);
        if (tenantId == -1) { finish(); return; }

        AppDatabase db = AppDatabase.getInstance(this);

        // TOOLBAR BACK BUTTON
        Toolbar toolbar = findViewById(R.id.toolbar);
        final TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // VIEWMODEL
        billViewModel = new ViewModelProvider(this).get(BillViewModel.class);
        billViewModel.getMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // INFO VIEWS
        TextView tvName    = findViewById(R.id.tvTenantName);
        TextView tvPhone   = findViewById(R.id.tvTenantPhone);
        TextView tvCccd    = findViewById(R.id.tvCccd);
        TextView tvAddress = findViewById(R.id.tvAddress);
        com.google.android.material.button.MaterialButton btnCallTenant = findViewById(R.id.btnCallTenant);
        com.google.android.material.button.MaterialButton btnChatZalo = findViewById(R.id.btnChatZalo);
        tvNoDoc            = findViewById(R.id.tvNoDoc);

        // THƯ MỤC ẢNH (mỗi khách có thư mục riêng)
        docsDir = new File(getFilesDir(), "tenant_docs/tenant_" + tenantId);
        if (!docsDir.exists()) docsDir.mkdirs();

        // GALLERY ADAPTER
        docAdapter = new DocPhotoAdapter();
        RecyclerView recyclerDocs = findViewById(R.id.recyclerDocs);
        GridLayoutManager docsLayoutManager = new GridLayoutManager(this, 3);
        docsLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return docAdapter.getSpanSize(position, docsLayoutManager.getSpanCount());
            }
        });
        recyclerDocs.setLayoutManager(docsLayoutManager);
        recyclerDocs.setAdapter(docAdapter);
        recyclerDocs.setNestedScrollingEnabled(false);

        docAdapter.setListener(new DocPhotoAdapter.Listener() {
            @Override
            public void onDelete(File file) {
                new AlertDialog.Builder(TenantDetailActivity.this)
                        .setTitle("Xoá ảnh")
                        .setMessage("Bạn có chắc muốn xoá ảnh này không?")
                        .setPositiveButton("Xoá", (d, w) -> {
                            if (file.delete()) {
                                docAdapter.removePhoto(file);
                                updateEmptyState();
                                Toast.makeText(TenantDetailActivity.this,
                                        "Đã xoá ảnh", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }

            @Override
            public void onView(File file) {
                // Mở dialog xem ảnh full màn hình
                Dialog dialog = new Dialog(TenantDetailActivity.this,
                        android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_view_photo);
                ImageView img = dialog.findViewById(R.id.imgFullscreen);
                ImageButton btnClose = dialog.findViewById(R.id.btnClosePhoto);
                Glide.with(TenantDetailActivity.this)
                        .load(file)
                        .into(img);
                btnClose.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
        });

        // LAUNCHER CAMERA
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && pendingPhotoFile != null) {
                        docAdapter.addPhoto(pendingPhotoFile);
                        updateEmptyState();
                        uploadPhotoToFirebase(pendingPhotoFile);
                    }
                }
        );

        // LAUNCHER GALLERY
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            String fileName = "tenant_gallery_" + System.currentTimeMillis() + ".jpg";
                            File dest = new File(docsDir, fileName);
                            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                                 java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                                if (in != null) {
                                    byte[] buf = new byte[4096];
                                    int n;
                                    while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                                }
                            }
                            docAdapter.addPhoto(dest);
                            updateEmptyState();
                            uploadPhotoToFirebase(dest);
                        } catch (Exception e) {
                            Toast.makeText(this, "Lỗi nhập ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // NÚT THÊM ẢNH
        View btnTakePhoto = findViewById(R.id.btnTakePhoto);
        if (btnTakePhoto != null) {
            btnTakePhoto.setOnClickListener(v -> showPhotoSourceDialog());
        }

        // LOAD THÔNG TIN KHÁCH
        final TenantEntity[] loadedTenant = {null};
        new Thread(() -> {
            TenantEntity t = db.tenantDao().getById(tenantId);
            if (t == null) { finish(); return; }
            loadedTenant[0] = t;

            // Lấy danh sách ảnh hiện có
            List<File> photos = loadPhotos();

            runOnUiThread(() -> {
                String name = t.fullName != null ? t.fullName : "---";
                tvName.setText(name);
                tvPhone.setText("📞 " + (t.phone != null ? t.phone : "---"));
                tvCccd.setText("🪪 CCCD: " + (t.cccd != null ? t.cccd : "---"));
                tvAddress.setText("📍 " + (t.address != null ? t.address : "---"));
                docAdapter.setPhotos(photos);
                updateEmptyState();
                bindQuickActions(btnCallTenant, btnChatZalo, t.phone);

                // Nút Sửa — gắn sau khi đã có dữ liệu khách
                com.google.android.material.button.MaterialButton btnEdit = findViewById(R.id.btnEditTenant);
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> {
                        if (loadedTenant[0] == null) return;
                        TenantFormDialog dialog = new TenantFormDialog(loadedTenant[0]);
                        dialog.show(getSupportFragmentManager(), "edit_tenant");
                        // Reload màn hình sau khi đóng dialog để thấy thay đổi
                        getSupportFragmentManager().setFragmentResultListener(
                            "tenant_updated", TenantDetailActivity.this,
                            (requestKey, result) -> recreate());
                    });
                }
            });
        }).start();

        // BILL HISTORY
        RecyclerView recyclerBills = findViewById(R.id.recyclerBillHistory);
        recyclerBills.setLayoutManager(new LinearLayoutManager(this));
        recyclerBills.setNestedScrollingEnabled(false);

        BillAdapter billAdapter = new BillAdapter();
        recyclerBills.setAdapter(billAdapter);

        db.billDao().getBillsByTenant(tenantId).observe(this, bills -> billAdapter.setData(bills));

        billAdapter.setListener(new BillAdapter.BillListener() {
            @Override
            public void onPay(BillWithInfo bill) {
                if (!bill.meterUpdated) showEditBillDialog(bill);
                else showPaymentDialog(bill);
            }
            @Override
            public void onViewDetail(BillWithInfo bill) {
                showBillDetailDialog(bill);
            }
        });
    }

    // ---- HELPER ----

    private void showPhotoSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thêm ảnh")
                .setItems(new String[]{"📷 Chụp ảnh mới", "🖼️ Chọn từ thư viện"}, (d, w) -> {
                    if (w == 0) launchCamera();
                    else pickImageLauncher.launch("image/*");
                })
                .show();
    }

    private void launchCamera() {
        // Tên file theo timestamp để không đè nhau
        String fileName = "doc_" + System.currentTimeMillis() + ".jpg";
        pendingPhotoFile = new File(docsDir, fileName);
        pendingPhotoUri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", pendingPhotoFile);
        takePictureLauncher.launch(pendingPhotoUri);
    }

    private List<File> loadPhotos() {
        List<File> result = new ArrayList<>();
        if (docsDir.exists()) {
            File[] files = docsDir.listFiles(f -> f.getName().endsWith(".jpg"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified())); // mới nhất trước
                result.addAll(Arrays.asList(files));
            }
        }
        return result;
    }

    private void updateEmptyState() {
        if (tvNoDoc != null) {
            tvNoDoc.setVisibility(docAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void bindQuickActions(
            com.google.android.material.button.MaterialButton btnCallTenant,
            com.google.android.material.button.MaterialButton btnChatZalo,
            String phone
    ) {
        String normalizedPhone = normalizePhone(phone);
        boolean hasPhone = !TextUtils.isEmpty(normalizedPhone);

        if (btnCallTenant != null) {
            btnCallTenant.setEnabled(hasPhone);
            btnCallTenant.setOnClickListener(v -> openDialer(normalizedPhone));
        }

        if (btnChatZalo != null) {
            btnChatZalo.setEnabled(hasPhone);
            btnChatZalo.setOnClickListener(v -> openZalo(normalizedPhone));
        }
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        return rawPhone.replaceAll("[^\\d+]", "");
    }

    private void openDialer(String phone) {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Khách chưa có số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
    }

    private void openZalo(String phone) {
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Khách chưa có số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/" + phone.replace("+", "")));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không mở được Zalo trên thiết bị này", Toast.LENGTH_SHORT).show();
        }
    }

    // ---- BILL DIALOGS ----

    private void showPaymentDialog(BillWithInfo bill) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        CurrencyInputHelper.attach(input);
        long remaining = (long)(bill.totalAmount - bill.totalPaid);
        if (remaining > 0) input.setText(String.valueOf(remaining));

        new AlertDialog.Builder(this)
                .setTitle("Nhập số tiền khách trả")
                .setView(input)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    String text = input.getText().toString().replaceAll("[^\\d]", "");
                    if (text.isEmpty()) { Toast.makeText(this, "Nhập số tiền", Toast.LENGTH_SHORT).show(); return; }
                    billViewModel.addPayment(bill.billId, Double.parseDouble(text));
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showBillDetailDialog(BillWithInfo bill) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            com.example.rentalmanager.data.entity.SettingEntity setting =
                    AppDatabase.getInstance(this).settingDao().getSetting();
            runOnUiThread(() -> {
                NumberFormat format = NumberFormat.getInstance(new Locale("vi","VN"));
                View view = LayoutInflater.from(this).inflate(R.layout.dialog_bill_detail, null);
                TextView txtElectric = view.findViewById(R.id.txtElectric);
                TextView txtWater    = view.findViewById(R.id.txtWater);
                TextView txtRent     = view.findViewById(R.id.txtRent);
                TextView txtTotal    = view.findViewById(R.id.txtTotal);
                TextView txtServiceFee = view.findViewById(R.id.txtServiceFee);

                int eUsed = bill.newElectric - bill.oldElectric;
                int wUsed = bill.newWater - bill.oldWater;
                double ePrice = bill.electricPrice;
                double wPrice = bill.waterPrice;
                if (ePrice <= 0 || wPrice <= 0) {
                    if (setting != null) {
                        if (ePrice <= 0) ePrice = setting.electricPrice > 0 ? setting.electricPrice : 3500;
                        if (wPrice <= 0) wPrice = setting.waterPrice > 0 ? setting.waterPrice : 20000;
                    } else {
                        if (ePrice <= 0) ePrice = 3500;
                        if (wPrice <= 0) wPrice = 20000;
                    }
                }
                double eMoney = eUsed * ePrice;
                double wMoney = wUsed * wPrice;
                double serviceFee = bill.serviceFee;
                double rent = bill.totalAmount - eMoney - wMoney - serviceFee;

                txtElectric.setText("Chỉ số cũ: " + bill.oldElectric + "\n"
                        + "Chỉ số mới: " + bill.newElectric + "\n"
                        + "Tiêu thụ: " + eUsed + " × " + format.format(ePrice) + " = " + format.format(eMoney) + " đ");
                txtWater.setText("Chỉ số cũ: " + bill.oldWater + "\n"
                        + "Chỉ số mới: " + bill.newWater + "\n"
                        + "Tiêu thụ: " + wUsed + " × " + format.format(wPrice) + " = " + format.format(wMoney) + " đ");
                txtRent.setText(format.format(rent > 0 ? rent : 0) + " đ");
                if (txtServiceFee != null) txtServiceFee.setText(format.format(serviceFee) + " đ");
                txtTotal.setText(format.format(bill.totalAmount) + " đ");

                final double fEPrice = ePrice, fWPrice = wPrice;

                // Hide QR components since they are not fully migrated to TenantDetailActivity yet
                view.findViewById(R.id.btnShowQR).setVisibility(View.GONE);

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Chi tiết hóa đơn " + bill.month)
                        .setView(view)
                        .setNeutralButton("📤 Chia sẻ", (d, w) ->
                            com.example.rentalmanager.util.InvoiceShareHelper.share(this, bill, fEPrice, fWPrice))
                        .create();

                view.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());
                view.findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> {
                    dialog.dismiss();
                    showPaymentDialog(bill);
                });

                dialog.show();
            });
        });
    }

    private void showEditBillDialog(BillWithInfo bill) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_bill, null);
        TextView txtOldE = view.findViewById(R.id.txtOldElectric);
        TextView txtOldW = view.findViewById(R.id.txtOldWater);
        EditText edtNewE = view.findViewById(R.id.edtNewElectric);
        EditText edtNewW = view.findViewById(R.id.edtNewWater);

        billViewModel.getLastBill(bill.contractId, lastBill -> {
            int oldE = (lastBill != null) ? lastBill.newElectric : 0;
            int oldW = (lastBill != null) ? lastBill.newWater : 0;
            runOnUiThread(() -> {
                txtOldE.setText("Điện cũ: " + oldE);
                txtOldW.setText("Nước cũ: " + oldW);
                new AlertDialog.Builder(this)
                        .setTitle("Nhập chỉ số điện nước")
                        .setView(view)
                        .setPositiveButton("Cập nhật", (d, w) -> {
                            String sE = edtNewE.getText().toString().trim();
                            String sW = edtNewW.getText().toString().trim();
                            if (sE.isEmpty() || sW.isEmpty()) {
                                Toast.makeText(this, "Nhập đầy đủ chỉ số", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            int newE = Integer.parseInt(sE);
                            int newW = Integer.parseInt(sW);
                            if (newE < oldE || newW < oldW) {
                                Toast.makeText(this, "Chỉ số mới phải lớn hơn chỉ số cũ", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            billViewModel.updateBillMeter(bill.billId, oldE, newE, oldW, newW);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            });
        });
    }

    private void uploadPhotoToFirebase(File file) {
        if (file == null || !file.exists()) return;

        ImgbbUploader.upload(file, new ImgbbUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                android.util.Log.d("TenantDetailActivity", "Tenant doc uploaded: " + imageUrl);
            }

            @Override
            public void onFailure(String error) {
                android.util.Log.e("TenantDetailActivity", "Tenant doc upload failed: " + error);
            }
        });
    }
}
