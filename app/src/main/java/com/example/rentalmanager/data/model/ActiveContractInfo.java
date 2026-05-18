package com.example.rentalmanager.data.model;

import androidx.room.ColumnInfo;

public class ActiveContractInfo {

    public int contractId;

    @ColumnInfo(name = "roomName")
    public String roomName;

    @ColumnInfo(name = "tenantName")
    public String tenantName;
    @ColumnInfo(name = "rentPrice")
    public double rentPrice;

    @ColumnInfo(name = "useWifi")
    public boolean useWifi;
    @ColumnInfo(name = "useTrash")
    public boolean useTrash;
    @ColumnInfo(name = "useServiceFee")
    public boolean useServiceFee;
}