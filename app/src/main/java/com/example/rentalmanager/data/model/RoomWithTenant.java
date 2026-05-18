package com.example.rentalmanager.data.model;

import androidx.room.ColumnInfo;

public class RoomWithTenant {

    // ===== ROOM =====
    public int roomId;
    public String roomName;
    public double price;
    public String status;
    public String note;

    // ===== CONTRACT =====
    @ColumnInfo(name = "c_contractId")
    public Integer contractId;

    @ColumnInfo(name = "c_tenantId")
    public Integer tenantId;

    @ColumnInfo(name = "c_startDate")
    public Long startDate;

    @ColumnInfo(name = "c_endDate")
    public Long endDate;

    @ColumnInfo(name = "c_status")
    public String contractStatus;

    // ===== TENANT =====
    @ColumnInfo(name = "t_fullName")
    public String tenantName;
}