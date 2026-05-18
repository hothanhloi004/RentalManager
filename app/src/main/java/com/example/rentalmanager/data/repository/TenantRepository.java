package com.example.rentalmanager.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rentalmanager.data.dao.TenantDao;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.TenantEntity;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.util.ResultState;

import java.util.List;
import java.util.concurrent.Executor;

public class TenantRepository {

    private final TenantDao tenantDao;
    private final Executor executor;

    public TenantRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        tenantDao = db.tenantDao();
        executor = AppExecutors.getInstance().diskIO();
    }

    public LiveData<List<TenantEntity>> getAllTenants() {
        return tenantDao.getAllTenants();
    }

    public LiveData<List<TenantEntity>> getAvailableTenants() {
        return tenantDao.getAvailableTenants();
    }

    public LiveData<ResultState> insert(TenantEntity tenant) {
        MutableLiveData<ResultState> result = new MutableLiveData<>();

        executor.execute(() -> {
            if (tenantDao.countByCccd(tenant.cccd) > 0) {
                result.postValue(new ResultState(false, "CCCD da ton tai"));
                return;
            }

            if (tenantDao.countByPhone(tenant.phone) > 0) {
                result.postValue(new ResultState(false, "SDT da ton tai"));
                return;
            }

            tenantDao.insert(tenant);
            result.postValue(new ResultState(true, "Them thanh cong"));
        });

        return result;
    }

    public LiveData<ResultState> update(TenantEntity tenant) {
        MutableLiveData<ResultState> result = new MutableLiveData<>();

        executor.execute(() -> {
            tenantDao.update(tenant);
            result.postValue(new ResultState(true, "Cap nhat thanh cong"));
        });

        return result;
    }

    public LiveData<ResultState> delete(TenantEntity tenant) {
        MutableLiveData<ResultState> result = new MutableLiveData<>();

        executor.execute(() -> {
            if (tenantDao.hasActiveContract(tenant.tenantId) > 0) {
                result.postValue(new ResultState(
                        false,
                        "Khong the xoa vi nguoi thue dang co hop dong hieu luc"
                ));
                return;
            }

            try {
                tenantDao.delete(tenant);
                result.postValue(new ResultState(true, "Da xoa thanh cong"));
            } catch (Exception e) {
                result.postValue(new ResultState(
                        false,
                        "Khong the xoa vi du lieu dang dinh voi lich su hop dong hoac hoa don cu"
                ));
            }
        });

        return result;
    }
}
