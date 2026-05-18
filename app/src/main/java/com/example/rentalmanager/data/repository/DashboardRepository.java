package com.example.rentalmanager.data.repository;

import android.app.Application;

import com.example.rentalmanager.data.dao.BillDao;
import com.example.rentalmanager.data.dao.ContractDao;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.model.DashboardStats;

import java.util.concurrent.Executor;
import com.example.rentalmanager.util.AppExecutors;

public class DashboardRepository {

    private final BillDao billDao;
    private final ContractDao contractDao;
    private final Executor executor = AppExecutors.getInstance().diskIO();

    public DashboardRepository(Application app) {

        AppDatabase db = AppDatabase.getInstance(app);

        billDao = db.billDao();
        contractDao = db.contractDao();
    }

    public void loadDashboard(String month, DashboardCallback callback) {

        executor.execute(() -> {

            DashboardStats stats = new DashboardStats();

            stats.revenue = billDao.getRevenueByMonth(month);
            stats.debt = billDao.getDebtByMonth(month);
            stats.unpaidBills = billDao.countUnpaidBills(month);
            stats.electricUsed = billDao.getElectricUsedByMonth(month);
            stats.waterUsed = billDao.getWaterUsedByMonth(month);
            stats.rentingRooms = contractDao.countActiveContracts();
            stats.overdueBills = billDao.countOverdueBillsByMonth(System.currentTimeMillis(), month);
            stats.meterNotUpdatedBills = billDao.countMeterNotUpdatedBills(month);

            callback.onResult(stats);
        });
    }

    public interface DashboardCallback {
        void onResult(DashboardStats stats);
    }

}
