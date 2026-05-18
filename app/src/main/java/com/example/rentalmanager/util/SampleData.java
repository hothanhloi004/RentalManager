package com.example.rentalmanager.util;

import android.content.Context;

import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.ContractEntity;
import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.entity.TenantEntity;


public class SampleData {

    public static void insert(Context context) {

        AppExecutors.getInstance().diskIO().execute(() -> {

            AppDatabase db = AppDatabase.getInstance(context);

            // ===== ROOMS =====
            for (int i = 1; i <= 5; i++) {

                RoomEntity room = new RoomEntity();
                room.roomName = "Phòng " + i;

                // mỗi phòng tăng 100k
                room.price = 3500000 + (i * 100000);

                room.status = "TRONG";
                room.note = "";

                db.roomDao().insert(room);
            }

            // ===== TENANTS =====
            for (int i = 1; i <= 5; i++) {

                TenantEntity tenant = new TenantEntity();
                tenant.fullName = "Khách " + i;
                tenant.phone = "090000000" + i;
                tenant.cccd = "12345678" + i;
                tenant.address = "Bình Dương";

                db.tenantDao().insert(tenant);
            }
            ;

        });
    }
}
