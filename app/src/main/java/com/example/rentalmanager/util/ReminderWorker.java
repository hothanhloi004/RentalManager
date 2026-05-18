package com.example.rentalmanager.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.entity.ContractEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderWorker extends Worker {

    public ReminderWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        NotificationHelper.createChannel(ctx);
        AppDatabase db = AppDatabase.getInstance(ctx);

        long now = System.currentTimeMillis();

        // 1. Hoá đơn quá hạn
        List<BillEntity> overdue = db.billDao().getOverdueBills(now);
        if (!overdue.isEmpty()) {
            NotificationHelper.sendNotification(ctx, 1001,
                    "⚠ Có " + overdue.size() + " hoá đơn quá hạn",
                    "Vui lòng kiểm tra và nhắc khách thanh toán.");
        }

        // 2. Hợp đồng sắp hết hạn (≤ 30 ngày)
        long in30days = now + 30L * 24 * 60 * 60 * 1000;
        List<ContractEntity> contracts = db.contractDao().getExpiringContracts(now, in30days);
        if (contracts != null && !contracts.isEmpty()) {
            NotificationHelper.sendNotification(ctx, 1002,
                    "Hợp đồng sắp hết hạn",
                    "Xem xét gia hạn hoặc chấm dứt hợp đồng.");
        }

        return Result.success();
    }
}
