package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tenants")
public class TenantEntity {

    @PrimaryKey(autoGenerate = true)
    public int tenantId;

    public String fullName;
    public String phone;
    public String cccd;
    public String address;
}