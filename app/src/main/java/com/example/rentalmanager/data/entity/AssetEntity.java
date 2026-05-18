package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
    tableName = "assets",
    foreignKeys = @ForeignKey(
        entity = RoomEntity.class,
        parentColumns = "roomId",
        childColumns = "roomId",
        onDelete = CASCADE
    ),
    indices = @Index("roomId")
)
public class AssetEntity {

    @PrimaryKey(autoGenerate = true)
    public int assetId;

    public int roomId;
    public String name;
    public int quantity;
    public String note;
    public long createdAt;
}
