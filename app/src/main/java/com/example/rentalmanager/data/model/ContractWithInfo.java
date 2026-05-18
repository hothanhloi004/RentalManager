package com.example.rentalmanager.data.model;

import androidx.room.ColumnInfo;

public class ContractWithInfo {

    public int contractId;

    @ColumnInfo(name = "roomName")
    public String roomName;

    @ColumnInfo(name = "tenantName")
    public String tenantName;

    @ColumnInfo(name = "tenantPhone")
    public String tenantPhone;

    public double rentPrice;
    public double deposit;

    public String status;

    public long startDate;
    public Long endDate;

    public boolean useWifi;
    public boolean useTrash;
    public boolean useServiceFee;
}