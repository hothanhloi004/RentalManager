package com.example.rentalmanager.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rentalmanager.data.dao.ContractDao;
import com.example.rentalmanager.data.dao.RoomDao;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.ContractEntity;
import com.example.rentalmanager.util.ContractStatus;
import com.example.rentalmanager.util.ResultState;
import com.example.rentalmanager.util.RoomStatus;
import com.example.rentalmanager.data.model.ActiveContractInfo;
import com.example.rentalmanager.data.model.ContractWithInfo;
import java.util.List;
import java.util.concurrent.Executor;
import com.example.rentalmanager.util.AppExecutors;

public class ContractRepository {

    private final ContractDao contractDao;
    private final RoomDao roomDao;
    private final Executor executor;

    public ContractRepository(Application application) {

        AppDatabase db = AppDatabase.getInstance(application);

        contractDao = db.contractDao();
        roomDao = db.roomDao();
        executor = AppExecutors.getInstance().diskIO();
    }

    // ===============================
    // GET ALL
    // ===============================

    public LiveData<List<ContractEntity>> getAllContracts() {
        return contractDao.getAllContracts();
    }

    public LiveData<ContractEntity> getById(int id) {
        return contractDao.getById(id);
    }
    public LiveData<List<ContractEntity>> getActiveContracts() {
        return contractDao.getActiveContracts();
    }

    public LiveData<List<ActiveContractInfo>> getActiveContractInfo() {
        return contractDao.getActiveContractInfo();
    }

    // ===============================
    // CREATE CONTRACT
    // ===============================

    public LiveData<ResultState> createContract(int roomId,
                                                int tenantId,
                                                long startDate,
                                                Long endDate,
                                                double deposit,
                                                double rentPrice,
                                                boolean useWifi,
                                                boolean useTrash,
                                                boolean useServiceFee) {

        MutableLiveData<ResultState> result = new MutableLiveData<>();

        executor.execute(() -> {

            if (contractDao.roomHasActiveContract(roomId) > 0) {
                result.postValue(new ResultState(false, "Phòng đang có hợp đồng hiệu lực"));
                return;
            }

            if (contractDao.tenantHasActiveContract(tenantId) > 0) {
                result.postValue(new ResultState(false, "Người thuê đang thuê phòng khác"));
                return;
            }

            ContractEntity contract = new ContractEntity();
            contract.roomId = roomId;
            contract.tenantId = tenantId;
            contract.startDate = startDate;
            contract.deposit = deposit;
            contract.rentPrice = rentPrice;
            contract.status = ContractStatus.HIEU_LUC;
            contract.endDate = endDate;
            contract.useWifi = useWifi;
            contract.useTrash = useTrash;
            contract.useServiceFee = useServiceFee;

            contractDao.insert(contract);

            roomDao.updateStatus(roomId, RoomStatus.DANG_THUE);

            result.postValue(new ResultState(true, "Tạo hợp đồng thành công"));
        });

        return result;
    }

    // ===============================
    // END CONTRACT
    // ===============================

    public LiveData<ResultState> endContract(int contractId, String nextRoomStatus) {

        MutableLiveData<ResultState> result = new MutableLiveData<>();

        executor.execute(() -> {

            // lấy roomId của hợp đồng
            int roomId = contractDao.getRoomIdByContract(contractId);

            // kết thúc hợp đồng
            contractDao.endContract(contractId);

            roomDao.updateStatus(roomId, nextRoomStatus);

            result.postValue(new ResultState(true, "Đã kết thúc hợp đồng"));
        });

        return result;
    }
    public LiveData<List<ContractWithInfo>> getContractsWithInfo() {
        return contractDao.getContractsWithInfo();
    }

    // ===============================
    // UPDATE SERVICES
    // ===============================
    public LiveData<ResultState> updateServiceFlags(int contractId, boolean useWifi, boolean useTrash, boolean useServiceFee) {
        MutableLiveData<ResultState> result = new MutableLiveData<>();
        executor.execute(() -> {
            try {
                contractDao.updateServiceFlags(contractId, useWifi, useTrash, useServiceFee);
                result.postValue(new ResultState(true, "Đã cập nhật dịch vụ"));
            } catch (Exception e) {
                result.postValue(new ResultState(false, "Lỗi khi cập nhật dịch vụ"));
            }
        });
        return result;
    }
}
