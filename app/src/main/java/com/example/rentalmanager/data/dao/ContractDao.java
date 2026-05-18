package com.example.rentalmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rentalmanager.data.entity.ContractEntity;
import com.example.rentalmanager.util.ContractStatus;
import com.example.rentalmanager.data.model.ActiveContractInfo;
import com.example.rentalmanager.data.model.ContractWithInfo;
import java.util.List;

@Dao
public interface ContractDao {

    @Insert
    long insert(ContractEntity contract);

    @Update
    void update(ContractEntity contract);

    @Query("SELECT * FROM contracts ORDER BY startDate DESC")
    LiveData<List<ContractEntity>> getAllContracts();

    @Query("SELECT * FROM contracts ORDER BY startDate DESC")
    List<ContractEntity> getAllContractsSync();

    @Query("SELECT * FROM contracts WHERE contractId = :id LIMIT 1")
    LiveData<ContractEntity> getById(int id);

    @Query("SELECT * FROM contracts WHERE contractId = :id LIMIT 1")
    ContractEntity getContractByIdSync(int id);

    @Query("SELECT COUNT(*) FROM contracts WHERE roomId = :roomId AND status = '" + ContractStatus.HIEU_LUC + "'")
    int roomHasActiveContract(int roomId);

    @Query("SELECT COUNT(*) FROM contracts WHERE tenantId = :tenantId AND status = '" + ContractStatus.HIEU_LUC + "'")
    int tenantHasActiveContract(int tenantId);

    @Query("SELECT * FROM contracts WHERE roomId = :roomId AND status = '" + ContractStatus.HIEU_LUC + "' LIMIT 1")
    LiveData<ContractEntity> getActiveContractByRoom(int roomId);

    @Query("SELECT * FROM contracts WHERE status = 'HIEU_LUC'")
    LiveData<List<ContractEntity>> getActiveContracts();

    @Query("SELECT contracts.contractId AS contractId, rooms.roomName AS roomName, tenants.fullName AS tenantName, contracts.rentPrice AS rentPrice, contracts.useWifi AS useWifi, contracts.useTrash AS useTrash, contracts.useServiceFee AS useServiceFee FROM contracts INNER JOIN rooms ON contracts.roomId = rooms.roomId INNER JOIN tenants ON contracts.tenantId = tenants.tenantId WHERE contracts.status = 'HIEU_LUC'")
    LiveData<List<ActiveContractInfo>> getActiveContractInfo();

    @Query("SELECT contracts.contractId, contracts.startDate, contracts.endDate, rooms.roomName AS roomName, tenants.fullName AS tenantName, tenants.phone AS tenantPhone, contracts.rentPrice, contracts.deposit, contracts.status, contracts.useWifi, contracts.useTrash, contracts.useServiceFee FROM contracts INNER JOIN rooms ON contracts.roomId = rooms.roomId INNER JOIN tenants ON contracts.tenantId = tenants.tenantId ORDER BY contracts.startDate DESC")
    LiveData<List<ContractWithInfo>> getContractsWithInfo();

    @Query("UPDATE contracts SET status = 'KET_THUC' WHERE contractId = :contractId")
    void endContract(int contractId);

    @Query("UPDATE contracts SET useWifi=:useWifi, useTrash=:useTrash, useServiceFee=:useServiceFee WHERE contractId=:contractId")
    void updateServiceFlags(int contractId, boolean useWifi, boolean useTrash, boolean useServiceFee);

    @Query("SELECT roomId FROM contracts WHERE contractId = :contractId")
    int getRoomIdByContract(int contractId);

    @Query("SELECT contracts.contractId AS contractId, rooms.roomName AS roomName, tenants.fullName AS tenantName, contracts.rentPrice AS rentPrice, contracts.useWifi AS useWifi, contracts.useTrash AS useTrash, contracts.useServiceFee AS useServiceFee FROM contracts INNER JOIN rooms ON contracts.roomId = rooms.roomId INNER JOIN tenants ON contracts.tenantId = tenants.tenantId WHERE contracts.status = 'HIEU_LUC'")
    List<ActiveContractInfo> getActiveContractInfoSync();

    @Query("SELECT COUNT(*) FROM contracts WHERE status = 'HIEU_LUC'")
    int countActiveContracts();

    // Hợp đồng sắp hết hạn (cho ReminderWorker)
    @Query("SELECT * FROM contracts WHERE status = 'HIEU_LUC' AND endDate BETWEEN :now AND :deadline")
    List<ContractEntity> getExpiringContracts(long now, long deadline);
}