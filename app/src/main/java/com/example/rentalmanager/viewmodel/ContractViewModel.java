package com.example.rentalmanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.rentalmanager.data.entity.ContractEntity;
import com.example.rentalmanager.data.model.ContractWithInfo;
import com.example.rentalmanager.data.repository.ContractRepository;
import com.example.rentalmanager.util.ResultState;

import java.util.List;

public class ContractViewModel extends AndroidViewModel {

    private final ContractRepository repository;

    private final LiveData<List<ContractWithInfo>> allContracts;

    public ContractViewModel(@NonNull Application application) {
        super(application);

        repository = new ContractRepository(application);

        allContracts = repository.getContractsWithInfo();
    }

    public LiveData<List<ContractWithInfo>> getAllContracts() {
        return allContracts;
    }

    public LiveData<ContractEntity> getById(int id) {
        return repository.getById(id);
    }

    public LiveData<ResultState> createContract(int roomId,
                                                int tenantId,
                                                long startDate,
                                                Long endDate,
                                                double deposit,
                                                double rentPrice,
                                                boolean useWifi,
                                                boolean useTrash,
                                                boolean useServiceFee) {

        return repository.createContract(
                roomId,
                tenantId,
                startDate,
                endDate,
                deposit,
                rentPrice,
                useWifi,
                useTrash,
                useServiceFee
        );
    }

    public LiveData<ResultState> endContract(int contractId, String nextRoomStatus) {
        return repository.endContract(contractId, nextRoomStatus);
    }

    public LiveData<ResultState> updateServiceFlags(int contractId, boolean useWifi, boolean useTrash, boolean useServiceFee) {
        return repository.updateServiceFlags(contractId, useWifi, useTrash, useServiceFee);
    }
}
