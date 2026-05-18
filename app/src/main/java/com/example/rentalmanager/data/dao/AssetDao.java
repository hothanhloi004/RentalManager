package com.example.rentalmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rentalmanager.data.entity.AssetEntity;

import java.util.List;

@Dao
public interface AssetDao {

    @Insert
    void insert(AssetEntity asset);

    @Update
    void update(AssetEntity asset);

    @Delete
    void delete(AssetEntity asset);

    @Query("SELECT * FROM assets WHERE roomId = :roomId ORDER BY createdAt ASC")
    LiveData<List<AssetEntity>> getByRoom(int roomId);

    @Query("SELECT * FROM assets WHERE roomId = :roomId ORDER BY createdAt ASC")
    List<AssetEntity> getByRoomSync(int roomId);
}
