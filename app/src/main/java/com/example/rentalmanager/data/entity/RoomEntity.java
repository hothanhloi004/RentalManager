package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "rooms",
        indices = {@Index(value = {"roomName"}, unique = true)}
)
public class RoomEntity {

    @PrimaryKey(autoGenerate = true)
    public int roomId;

    public String roomName;
    public double price;
    public String status; // TRONG / DANG_THUE
    public String note;
    public String imageUrl;
}