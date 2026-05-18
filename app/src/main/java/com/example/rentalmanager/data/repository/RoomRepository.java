package com.example.rentalmanager.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.rentalmanager.data.dao.RoomDao;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.RoomEntity;

import java.util.List;
import java.util.concurrent.Executor;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.data.model.RoomWithTenant;

public class RoomRepository {

    private final RoomDao roomDao;
    private final LiveData<List<RoomEntity>> allRooms;
    private final Executor executorService;

    public RoomRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        roomDao = db.roomDao();
        allRooms = roomDao.getAllRooms();
        executorService = AppExecutors.getInstance().diskIO();
    }

    public LiveData<List<RoomEntity>> getAllRooms() {
        return allRooms;
    }

    public LiveData<List<RoomWithTenant>> getRoomsWithTenant() {
        return roomDao.getRoomsWithTenant();
    }

    // INSERT
    public void insert(String name,
                       double price,
                       String note,
                       MutableLiveData<Boolean> result) {

        executorService.execute(() -> {

            if (roomDao.countByName(name) > 0) {
                result.postValue(false);
                return;
            }

            RoomEntity room = new RoomEntity();
            room.roomName = name;
            room.price = price;
            room.status = "TRONG";
            room.note = note;

            roomDao.insert(room);
            result.postValue(true);
        });
    }

    // UPDATE WITH CHECK
    public void updateWithCheck(RoomEntity oldRoom,
                                String newName,
                                double newPrice,
                                String newNote,
                                MutableLiveData<Boolean> result) {

        executorService.execute(() -> {

            if (!oldRoom.roomName.equals(newName)
                    && roomDao.countByName(newName) > 0) {

                result.postValue(false);
                return;
            }

            oldRoom.roomName = newName;
            oldRoom.price = newPrice;
            oldRoom.note = newNote;

            roomDao.update(oldRoom);
            result.postValue(true);
        });
    }

    public void delete(RoomEntity room) {
        executorService.execute(() -> {
            try {
                roomDao.delete(room);
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .collection("rooms").document(String.valueOf(room.roomId))
                            .delete();
                }
            } catch (Exception e) {
                // Ignore crash due to SQLiteConstraintException (e.g. room has tied contracts/bills)
                e.printStackTrace();
            }
        });
    }
    public LiveData<Integer> getTotalRooms() {
        return roomDao.countTotalRooms();
    }

    public LiveData<Integer> getOccupiedRooms() {
        return roomDao.countOccupiedRooms();
    }

    public LiveData<Integer> getEmptyRooms() {
        return roomDao.countEmptyRooms();
    }
    public LiveData<List<RoomEntity>> getAvailableRooms() {
        return roomDao.getAvailableRooms();
    }
}