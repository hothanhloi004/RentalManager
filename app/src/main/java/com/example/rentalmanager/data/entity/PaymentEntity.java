package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(
        tableName = "payments",
        foreignKeys = @ForeignKey(
                entity = BillEntity.class,
                parentColumns = "billId",
                childColumns = "billId",
                onDelete = CASCADE
        ),
        indices = @Index("billId")
)
public class PaymentEntity {

    @PrimaryKey(autoGenerate = true)
    public int paymentId;

    public int billId;

    public double amount;      // số tiền trả
    public long paymentDate;   // thời điểm trả
}