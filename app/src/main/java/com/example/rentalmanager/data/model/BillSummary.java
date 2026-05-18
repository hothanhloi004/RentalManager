package com.example.rentalmanager.data.model;

public class BillSummary {

    public double totalAmount;

    public double totalPaid;

    public double remaining;

    @androidx.room.Ignore
    public String dueDate;
}