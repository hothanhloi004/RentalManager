package com.example.rentalmanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.model.ActiveContractInfo;
import com.example.rentalmanager.data.repository.BillRepository;
import com.example.rentalmanager.data.repository.ContractRepository;
import com.example.rentalmanager.data.model.BillWithInfo;

import java.util.List;

public class BillViewModel extends AndroidViewModel {

    private final BillRepository repository;
    private final ContractRepository contractRepository;

    private final LiveData<List<BillEntity>> allBills;
    private final LiveData<List<ActiveContractInfo>> activeContracts;

    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public BillViewModel(@NonNull Application application) {
        super(application);

        repository = new BillRepository(application);
        contractRepository = new ContractRepository(application);

        allBills = repository.getAllBills();
        activeContracts = contractRepository.getActiveContractInfo();
    }

    public LiveData<List<BillEntity>> getAllBills() {
        return allBills;
    }

    public LiveData<List<ActiveContractInfo>> getActiveContracts() {
        return activeContracts;
    }

    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void createBill(BillEntity bill) {
        repository.createBill(bill, (success, msg) -> {
            operationResult.postValue(success);
            message.postValue(msg);
        });
    }


    public void addPayment(int billId, double amount) {

        repository.addPayment(billId, amount, (success, msg) -> {
            operationResult.postValue(success);
            message.postValue(msg);
        });
    }
    public LiveData<List<BillWithInfo>> getBillsWithInfo() {
        return repository.getBillsWithInfo();
    }
    public void generateBills(String month) {
        repository.generateBills(month, (success, msg) -> {
            operationResult.postValue(success);
            message.postValue(msg);
        });
    }
    public void getLastBill(int contractId, BillRepository.BillCallback callback){
        repository.getLastBill(contractId, callback);
    }

    public void updateBillMeter(int billId,int oldE,int newE,int oldW,int newW){
        repository.updateBillMeter(billId,oldE,newE,oldW,newW);
    }
    public BillEntity getBillById(int billId){
        return repository.getBillById(billId);
    }
}