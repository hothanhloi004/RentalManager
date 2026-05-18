package com.example.rentalmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rentalmanager.data.entity.TenantEntity;

import java.util.List;

@Dao
public interface TenantDao {

    @Insert
    void insert(TenantEntity tenant);

    @Update
    void update(TenantEntity tenant);

    @Delete
    void delete(TenantEntity tenant);

    @Query("SELECT * FROM tenants ORDER BY fullName ASC")
    LiveData<List<TenantEntity>> getAllTenants();

    @Query("SELECT * FROM tenants ORDER BY fullName ASC")
    List<TenantEntity> getAllTenantsSync();

    @Query("SELECT * FROM tenants WHERE tenantId = :id LIMIT 1")
    TenantEntity getById(int id);

    @Query("SELECT COUNT(*) FROM contracts WHERE tenantId = :tenantId AND status = 'HIEU_LUC'")
    int hasActiveContract(int tenantId);

    @Query("SELECT COUNT(*) FROM tenants WHERE cccd = :cccd")
    int countByCccd(String cccd);

    @Query("SELECT COUNT(*) FROM tenants WHERE phone = :phone")
    int countByPhone(String phone);

    @Query("SELECT * FROM tenants WHERE tenantId NOT IN " +
            "(SELECT tenantId FROM contracts WHERE status = 'HIEU_LUC') " +
            "ORDER BY fullName ASC")
    LiveData<List<TenantEntity>> getAvailableTenants();
}