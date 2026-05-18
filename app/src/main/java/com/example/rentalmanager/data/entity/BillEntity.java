package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "bills",
        foreignKeys = {
                @ForeignKey(
                        entity = ContractEntity.class,
                        parentColumns = "contractId",
                        childColumns = "contractId",
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = {"contractId"}),
                @Index(value = {"contractId", "month"}, unique = true)
        }
)
public class BillEntity {

    @PrimaryKey(autoGenerate = true)
    public int billId;

    public int contractId;

    public String month;

    public int oldElectric;
    public int newElectric;

    public int oldWater;
    public int newWater;

    public int electricUsed;
    public int waterUsed;

    public double electricPrice;
    public double waterPrice;

    public double rentPrice;
    public double serviceFee;

    public double totalAmount;

    public String paymentStatus;

    public long dueDate;

    public Long paidAt;
    public boolean meterUpdated;
}