package com.example.rentalmanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.rentalmanager.data.model.DashboardStats;
import com.example.rentalmanager.data.repository.DashboardRepository;

public class DashboardViewModel extends AndroidViewModel {

    private final DashboardRepository repo;

    public MutableLiveData<DashboardStats> stats = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application app) {
        super(app);
        repo = new DashboardRepository(app);
    }

    public void loadDashboard(String month) {

        repo.loadDashboard(month, result -> stats.postValue(result));
    }
}