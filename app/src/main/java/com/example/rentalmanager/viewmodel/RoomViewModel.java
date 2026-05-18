package com.example.rentalmanager.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.repository.RoomRepository;
import com.example.rentalmanager.data.model.RoomWithTenant;

import java.util.List;

public class RoomViewModel extends AndroidViewModel {

    private final RoomRepository repository;
    private final LiveData<List<RoomEntity>> allRooms;

    private final MutableLiveData<Boolean> operationResult = new MutableLiveData<>();

    public RoomViewModel(@NonNull Application application) {
        super(application);
        repository = new RoomRepository(application);
        allRooms = repository.getAllRooms();
    }

    public LiveData<List<RoomEntity>> getAllRooms() {
        return allRooms;
    }

    public LiveData<List<RoomWithTenant>> getRoomsWithTenant() {
        return repository.getRoomsWithTenant();
    }
    public LiveData<Boolean> getOperationResult() {
        return operationResult;
    }

    public void addRoom(String name, double price, String note) {
        repository.insert(name, price, note, operationResult);
    }

    public void updateRoom(RoomEntity room,
                           String name,
                           double price,
                           String note) {

        repository.updateWithCheck(room, name, price, note, operationResult);
    }

    public void delete(RoomEntity room) {
        repository.delete(room);
    }
    public LiveData<Integer> getTotalRooms() {
        return repository.getTotalRooms();
    }

    public LiveData<Integer> getOccupiedRooms() {
        return repository.getOccupiedRooms();
    }

    public LiveData<Integer> getEmptyRooms() {
        return repository.getEmptyRooms();
    }
    public LiveData<List<RoomEntity>> getAvailableRooms() {
        return repository.getAvailableRooms();
    }
}