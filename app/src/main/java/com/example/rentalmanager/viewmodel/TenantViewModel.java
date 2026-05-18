package com.example.rentalmanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.rentalmanager.data.entity.TenantEntity;
import com.example.rentalmanager.data.repository.TenantRepository;
import com.example.rentalmanager.util.ResultState;

import java.util.List;
import com.example.rentalmanager.util.ResultState;
import androidx.lifecycle.LiveData;
public class TenantViewModel extends AndroidViewModel {

    private final TenantRepository repository;
    private final LiveData<List<TenantEntity>> allTenants;

    public TenantViewModel(@NonNull Application application) {
        super(application);
        repository = new TenantRepository(application);
        allTenants = repository.getAllTenants();
    }

    public LiveData<List<TenantEntity>> getAllTenants() {
        return allTenants;
    }

    public LiveData<List<TenantEntity>> getAvailableTenants() {
        return repository.getAvailableTenants();
    }

    public LiveData<ResultState> insert(TenantEntity tenant) {
        return repository.insert(tenant);
    }

    public LiveData<ResultState> update(TenantEntity tenant) {
        return repository.update(tenant);
    }

    public LiveData<ResultState> delete(TenantEntity tenant) {
        return repository.delete(tenant);
    }
}