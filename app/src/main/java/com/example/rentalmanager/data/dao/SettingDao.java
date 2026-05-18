package com.example.rentalmanager.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rentalmanager.data.entity.SettingEntity;

@Dao
public interface SettingDao {

    @Insert
    void insert(SettingEntity setting);

    @Update
    void update(SettingEntity setting);

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    SettingEntity getSetting();
}