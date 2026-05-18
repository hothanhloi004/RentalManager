package com.example.rentalmanager.data.model;

import androidx.room.ColumnInfo;

public class BillWithInfo {

    public int billId;
    public int contractId;
    public String month;

    public int oldElectric;
    public int newElectric;
    public int oldWater;
    public int newWater;

    public double electricPrice;
    public double waterPrice;
    public double rentPrice;
    public double serviceFee;

    public double totalAmount;
    public String paymentStatus;
    public long dueDate;
    public Long paidAt;

    @ColumnInfo(name = "roomName")
    public String roomName;

    @ColumnInfo(name = "tenantName")
    public String tenantName;

    @ColumnInfo(name = "totalPaid")
    public double totalPaid;
    public boolean meterUpdated;
}