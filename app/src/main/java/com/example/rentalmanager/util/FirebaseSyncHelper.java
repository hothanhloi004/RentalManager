package com.example.rentalmanager.util;

import android.content.Context;
import android.util.Log;

import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.entity.ContractEntity;
import com.example.rentalmanager.data.entity.PaymentEntity;
import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.entity.SettingEntity;
import com.example.rentalmanager.data.entity.TenantEntity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseSyncHelper {

    private static final String TAG = "FirebaseSyncHelper";
    private static final int BATCH_LIMIT = 450;

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    private interface CompletionHandler {
        void onComplete();
    }

    private static void commitBatchesSequentially(
            List<WriteBatch> batches,
            int index,
            SyncCallback callback,
            String successMsg
    ) {
        if (batches.isEmpty()) {
            if (callback != null) {
                callback.onSuccess(successMsg);
            }
            return;
        }

        if (index >= batches.size()) {
            if (callback != null) {
                callback.onSuccess(successMsg);
            }
            return;
        }

        batches.get(index).commit()
                .addOnSuccessListener(v ->
                        commitBatchesSequentially(batches, index + 1, callback, successMsg))
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure("Loi ket noi Dam May (batch " + index + "): " + e.getMessage());
                    }
                });
    }

    private static void cleanupCollection(
            FirebaseFirestore fs,
            String collectionPath,
            Set<String> localIds,
            SyncCallback callback,
            CompletionHandler onComplete
    ) {
        fs.collection(collectionPath).get()
                .addOnSuccessListener(snapshot -> {
                    List<WriteBatch> deleteBatches = new ArrayList<>();
                    WriteBatch current = fs.batch();
                    int count = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (!localIds.contains(doc.getId())) {
                            current.delete(doc.getReference());
                            count++;
                            if (count >= BATCH_LIMIT) {
                                deleteBatches.add(current);
                                current = fs.batch();
                                count = 0;
                            }
                        }
                    }

                    if (count > 0) {
                        deleteBatches.add(current);
                    }

                    commitBatchesSequentially(
                            deleteBatches,
                            0,
                            new SyncCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    onComplete.onComplete();
                                }

                                @Override
                                public void onFailure(String error) {
                                    if (callback != null) {
                                        callback.onFailure(error);
                                    }
                                }
                            },
                            "Cleanup " + collectionPath + " success"
                    );
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure("Khong the doi chieu du lieu cloud: " + e.getMessage());
                    }
                });
    }

    private static void cleanupDeletedCloudDocs(
            FirebaseFirestore fs,
            String base,
            Set<String> roomIds,
            Set<String> tenantIds,
            Set<String> contractIds,
            Set<String> billIds,
            Set<String> paymentIds,
            SyncCallback callback,
            String successMsg
    ) {
        cleanupCollection(fs, base + "rooms", roomIds, callback, () ->
                cleanupCollection(fs, base + "tenants", tenantIds, callback, () ->
                        cleanupCollection(fs, base + "contracts", contractIds, callback, () ->
                                cleanupCollection(fs, base + "bills", billIds, callback, () ->
                                        cleanupCollection(fs, base + "payments", paymentIds, callback, () -> {
                                            if (callback != null) {
                                                callback.onSuccess(successMsg);
                                            }
                                        })
                                )
                        )
                )
        );
    }

    public static void backupAll(Context context, SyncCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            if (callback != null) {
                callback.onFailure("Ban chua dang nhap tai khoan dam may!");
            }
            return;
        }

        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                FirebaseFirestore fs = FirebaseFirestore.getInstance();
                String base = "users/" + uid + "/";

                List<RoomEntity> rooms = db.roomDao().getAllRoomsSync();
                List<TenantEntity> tenants = db.tenantDao().getAllTenantsSync();
                List<ContractEntity> contracts = db.contractDao().getAllContractsSync();
                List<BillEntity> bills = db.billDao().getAllBillsSync();
                List<PaymentEntity> payments = db.paymentDao().getAllPaymentsSync();
                SettingEntity setting = db.settingDao().getSetting();

                Set<String> roomIds = new HashSet<>();
                Set<String> tenantIds = new HashSet<>();
                Set<String> contractIds = new HashSet<>();
                Set<String> billIds = new HashSet<>();
                Set<String> paymentIds = new HashSet<>();

                List<Map.Entry<com.google.firebase.firestore.DocumentReference, Map<String, Object>>> writes = new ArrayList<>();

                if (setting != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("electricPrice", setting.electricPrice);
                    data.put("waterPrice", setting.waterPrice);
                    data.put("trashFee", setting.trashFee);
                    data.put("wifiPrice", setting.wifiPrice);
                    data.put("serviceFee", setting.serviceFee);
                    data.put("pinEnabled", setting.pinEnabled);
                    data.put("pinCode", setting.pinCode);
                    data.put("bankCode", setting.bankCode);
                    data.put("bankAccount", setting.bankAccount);
                    data.put("hostelName", setting.hostelName);
                    data.put("landlordName", setting.landlordName);
                    data.put("landlordPhone", setting.landlordPhone);
                    data.put("hostelAddress", setting.hostelAddress);
                    writes.add(new AbstractMap.SimpleEntry<>(fs.document(base + "settings/config"), data));
                }

                for (RoomEntity room : rooms) {
                    String docId = String.valueOf(room.roomId);
                    roomIds.add(docId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("roomId", room.roomId);
                    data.put("roomName", room.roomName);
                    data.put("price", room.price);
                    data.put("status", room.status);
                    data.put("note", room.note);
                    data.put("imageUrl", room.imageUrl);
                    writes.add(new AbstractMap.SimpleEntry<>(fs.collection(base + "rooms").document(docId), data));
                }

                for (TenantEntity tenant : tenants) {
                    String docId = String.valueOf(tenant.tenantId);
                    tenantIds.add(docId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("tenantId", tenant.tenantId);
                    data.put("fullName", tenant.fullName);
                    data.put("phone", tenant.phone);
                    data.put("cccd", tenant.cccd);
                    data.put("address", tenant.address);
                    writes.add(new AbstractMap.SimpleEntry<>(fs.collection(base + "tenants").document(docId), data));
                }

                for (ContractEntity contract : contracts) {
                    String docId = String.valueOf(contract.contractId);
                    contractIds.add(docId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("contractId", contract.contractId);
                    data.put("roomId", contract.roomId);
                    data.put("tenantId", contract.tenantId);
                    data.put("rentPrice", contract.rentPrice);
                    data.put("deposit", contract.deposit);
                    data.put("startDate", contract.startDate);
                    data.put("endDate", contract.endDate);
                    data.put("status", contract.status);
                    data.put("useWifi", contract.useWifi);
                    data.put("useTrash", contract.useTrash);
                    data.put("useServiceFee", contract.useServiceFee);
                    writes.add(new AbstractMap.SimpleEntry<>(fs.collection(base + "contracts").document(docId), data));
                }

                for (BillEntity bill : bills) {
                    String docId = String.valueOf(bill.billId);
                    billIds.add(docId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("billId", bill.billId);
                    data.put("contractId", bill.contractId);
                    data.put("month", bill.month);
                    data.put("oldElectric", bill.oldElectric);
                    data.put("newElectric", bill.newElectric);
                    data.put("oldWater", bill.oldWater);
                    data.put("newWater", bill.newWater);
                    data.put("electricPrice", bill.electricPrice);
                    data.put("waterPrice", bill.waterPrice);
                    data.put("rentPrice", bill.rentPrice);
                    data.put("serviceFee", bill.serviceFee);
                    data.put("totalAmount", bill.totalAmount);
                    data.put("paymentStatus", bill.paymentStatus);
                    data.put("dueDate", bill.dueDate);
                    data.put("paidAt", bill.paidAt);
                    data.put("meterUpdated", bill.meterUpdated);
                    data.put("electricUsed", bill.electricUsed);
                    data.put("waterUsed", bill.waterUsed);
                    writes.add(new AbstractMap.SimpleEntry<>(fs.collection(base + "bills").document(docId), data));
                }

                for (PaymentEntity payment : payments) {
                    String docId = String.valueOf(payment.paymentId);
                    paymentIds.add(docId);

                    Map<String, Object> data = new HashMap<>();
                    data.put("paymentId", payment.paymentId);
                    data.put("billId", payment.billId);
                    data.put("amount", payment.amount);
                    data.put("paymentDate", payment.paymentDate);
                    writes.add(new AbstractMap.SimpleEntry<>(fs.collection(base + "payments").document(docId), data));
                }

                List<WriteBatch> batches = new ArrayList<>();
                WriteBatch current = fs.batch();
                int count = 0;
                for (Map.Entry<com.google.firebase.firestore.DocumentReference, Map<String, Object>> entry : writes) {
                    current.set(entry.getKey(), entry.getValue());
                    count++;
                    if (count >= BATCH_LIMIT) {
                        batches.add(current);
                        current = fs.batch();
                        count = 0;
                    }
                }
                if (count > 0) {
                    batches.add(current);
                }

                final String successMsg =
                        "Day du lieu len Dam May thanh cong!\n"
                                + rooms.size() + " phong | "
                                + tenants.size() + " khach | "
                                + contracts.size() + " hop dong | "
                                + bills.size() + " hoa don | "
                                + payments.size() + " lan thanh toan";

                Log.d(TAG, "Backup in " + batches.size() + " batch(es)");
                commitBatchesSequentially(
                        batches,
                        0,
                        new SyncCallback() {
                            @Override
                            public void onSuccess(String message) {
                                cleanupDeletedCloudDocs(
                                        fs,
                                        base,
                                        roomIds,
                                        tenantIds,
                                        contractIds,
                                        billIds,
                                        paymentIds,
                                        callback,
                                        successMsg
                                );
                            }

                            @Override
                            public void onFailure(String error) {
                                if (callback != null) {
                                    callback.onFailure(error);
                                }
                            }
                        },
                        successMsg
                );
            } catch (Exception e) {
                Log.e(TAG, "Exception backup: " + e.getMessage());
                if (callback != null) {
                    callback.onFailure("Loi he thong: " + e.getMessage());
                }
            }
        });
    }

    public static void restoreAll(Context context, SyncCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            if (callback != null) {
                callback.onFailure("Ban chua dang nhap tai khoan dam may!");
            }
            return;
        }

        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        String base = "users/" + uid + "/";

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> tSetting =
                fs.document(base + "settings/config").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tRooms =
                fs.collection(base + "rooms").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tTenants =
                fs.collection(base + "tenants").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tContracts =
                fs.collection(base + "contracts").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tBills =
                fs.collection(base + "bills").get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tPayments =
                fs.collection(base + "payments").get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(tSetting, tRooms, tTenants, tContracts, tBills, tPayments)
                .addOnCompleteListener(allTask ->
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(context);
                                db.clearAllTables();

                                if (tSetting.isSuccessful() && tSetting.getResult().exists()) {
                                    DocumentSnapshot doc = tSetting.getResult();
                                    SettingEntity s = new SettingEntity();
                                    s.electricPrice = doc.getDouble("electricPrice") != null ? doc.getDouble("electricPrice") : 0.0;
                                    s.waterPrice = doc.getDouble("waterPrice") != null ? doc.getDouble("waterPrice") : 0.0;
                                    s.trashFee = doc.getDouble("trashFee") != null ? doc.getDouble("trashFee") : 0.0;
                                    s.wifiPrice = doc.getDouble("wifiPrice") != null ? doc.getDouble("wifiPrice") : 0.0;
                                    s.serviceFee = doc.getDouble("serviceFee") != null ? doc.getDouble("serviceFee") : 0.0;
                                    s.pinEnabled = Boolean.TRUE.equals(doc.getBoolean("pinEnabled"));
                                    s.pinCode = doc.getString("pinCode");
                                    s.bankCode = doc.getString("bankCode");
                                    s.bankAccount = doc.getString("bankAccount");
                                    s.hostelName = doc.getString("hostelName");
                                    s.landlordName = doc.getString("landlordName");
                                    s.landlordPhone = doc.getString("landlordPhone");
                                    s.hostelAddress = doc.getString("hostelAddress");
                                    db.settingDao().insert(s);
                                }

                                if (tRooms.isSuccessful()) {
                                    for (DocumentSnapshot doc : tRooms.getResult()) {
                                        RoomEntity r = new RoomEntity();
                                        r.roomId = getInt(doc, "roomId");
                                        r.roomName = doc.getString("roomName");
                                        r.price = getDouble(doc, "price");
                                        r.status = RoomStatus.TRONG;
                                        r.note = doc.getString("note");
                                        r.imageUrl = doc.getString("imageUrl");
                                        db.roomDao().insert(r);
                                    }
                                }

                                if (tTenants.isSuccessful()) {
                                    for (DocumentSnapshot doc : tTenants.getResult()) {
                                        TenantEntity t = new TenantEntity();
                                        t.tenantId = getInt(doc, "tenantId");
                                        t.fullName = doc.getString("fullName");
                                        t.phone = doc.getString("phone");
                                        t.cccd = doc.getString("cccd");
                                        t.address = doc.getString("address");
                                        db.tenantDao().insert(t);
                                    }
                                }

                                if (tContracts.isSuccessful()) {
                                    for (DocumentSnapshot doc : tContracts.getResult()) {
                                        ContractEntity c = new ContractEntity();
                                        c.contractId = getInt(doc, "contractId");
                                        c.roomId = getInt(doc, "roomId");
                                        c.tenantId = getInt(doc, "tenantId");
                                        c.rentPrice = getDouble(doc, "rentPrice");
                                        c.deposit = doc.getDouble("deposit") != null ? doc.getDouble("deposit") : 0.0;
                                        c.startDate = doc.getLong("startDate") != null ? doc.getLong("startDate") : 0L;
                                        c.endDate = doc.getLong("endDate");
                                        c.status = doc.getString("status");
                                        c.useWifi = Boolean.TRUE.equals(doc.getBoolean("useWifi"));
                                        c.useTrash = Boolean.TRUE.equals(doc.getBoolean("useTrash"));
                                        c.useServiceFee = Boolean.TRUE.equals(doc.getBoolean("useServiceFee"));
                                        db.contractDao().insert(c);
                                    }
                                }

                                if (tBills.isSuccessful()) {
                                    for (DocumentSnapshot doc : tBills.getResult()) {
                                        BillEntity b = new BillEntity();
                                        b.billId = getInt(doc, "billId");
                                        b.contractId = getInt(doc, "contractId");
                                        b.month = doc.getString("month");
                                        b.oldElectric = getInt(doc, "oldElectric");
                                        b.newElectric = getInt(doc, "newElectric");
                                        b.oldWater = getInt(doc, "oldWater");
                                        b.newWater = getInt(doc, "newWater");
                                        b.electricPrice = doc.getDouble("electricPrice") != null ? doc.getDouble("electricPrice") : 0.0;
                                        b.waterPrice = doc.getDouble("waterPrice") != null ? doc.getDouble("waterPrice") : 0.0;
                                        b.rentPrice = doc.getDouble("rentPrice") != null ? doc.getDouble("rentPrice") : 0.0;
                                        b.serviceFee = doc.getDouble("serviceFee") != null ? doc.getDouble("serviceFee") : 0.0;
                                        b.totalAmount = doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0.0;
                                        b.paymentStatus = doc.getString("paymentStatus");
                                        b.dueDate = doc.getLong("dueDate") != null ? doc.getLong("dueDate") : 0L;
                                        b.paidAt = doc.getLong("paidAt");
                                        b.meterUpdated = Boolean.TRUE.equals(doc.getBoolean("meterUpdated"));
                                        b.electricUsed = getInt(doc, "electricUsed");
                                        b.waterUsed = getInt(doc, "waterUsed");
                                        db.billDao().insert(b);
                                    }
                                }

                                if (tPayments.isSuccessful()) {
                                    for (DocumentSnapshot doc : tPayments.getResult()) {
                                        PaymentEntity p = new PaymentEntity();
                                        p.paymentId = getInt(doc, "paymentId");
                                        p.billId = getInt(doc, "billId");
                                        p.amount = getDouble(doc, "amount");
                                        p.paymentDate = doc.getLong("paymentDate") != null ? doc.getLong("paymentDate") : 0L;
                                        db.paymentDao().insert(p);
                                    }
                                }

                                for (RoomEntity room : db.roomDao().getAllRoomsSync()) {
                                    int activeCount = db.contractDao().roomHasActiveContract(room.roomId);
                                    db.roomDao().updateStatus(
                                            room.roomId,
                                            activeCount > 0 ? RoomStatus.DANG_THUE : RoomStatus.TRONG
                                    );
                                }

                                int roomCount = tRooms.isSuccessful() ? tRooms.getResult().size() : 0;
                                int tenantCount = tTenants.isSuccessful() ? tTenants.getResult().size() : 0;
                                int contractCount = tContracts.isSuccessful() ? tContracts.getResult().size() : 0;
                                int billCount = tBills.isSuccessful() ? tBills.getResult().size() : 0;
                                int paymentCount = tPayments.isSuccessful() ? tPayments.getResult().size() : 0;

                                if (callback != null) {
                                    callback.onSuccess(
                                            "Tai du lieu tu Dam May thanh cong!\n"
                                                    + roomCount + " phong | "
                                                    + tenantCount + " khach | "
                                                    + contractCount + " hop dong | "
                                                    + billCount + " hoa don | "
                                                    + paymentCount + " lan thanh toan"
                                    );
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception Restore: " + e.getMessage());
                                if (callback != null) {
                                    callback.onFailure("Loi phuc hoi: " + e.getMessage());
                                }
                            }
                        })
                );
    }

    private static double getDouble(DocumentSnapshot doc, String field) {
        Double value = doc.getDouble(field);
        return value != null ? value : 0.0;
    }

    private static int getInt(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value != null ? value.intValue() : 0;
    }

    public static void updateRoomToCloud(RoomEntity room) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }

        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", room.roomId);
        data.put("roomName", room.roomName);
        data.put("price", room.price);
        data.put("status", room.status);
        data.put("note", room.note);
        data.put("imageUrl", room.imageUrl);

        fs.collection("users/" + uid + "/rooms")
                .document(String.valueOf(room.roomId))
                .set(data, SetOptions.merge());
    }

    public static void deleteRoomFromCloud(int roomId) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users/" + uid + "/rooms")
                .document(String.valueOf(roomId))
                .delete()
                .addOnFailureListener(e -> Log.w(TAG, "deleteRoomFromCloud failed: " + e.getMessage()));
    }
}
