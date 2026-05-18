package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "contracts",
        foreignKeys = {
                @ForeignKey(
                        entity = RoomEntity.class,
                        parentColumns = "roomId",
                        childColumns = "roomId",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = TenantEntity.class,
                        parentColumns = "tenantId",
                        childColumns = "tenantId",
                        onDelete = ForeignKey.RESTRICT
                )
        },
        indices = {
                @Index("roomId"),
                @Index("tenantId")
        }
)
public class ContractEntity {

    @PrimaryKey(autoGenerate = true)
    public int contractId;

    public int roomId;
    public int tenantId;

    public long startDate;
    public Long endDate;

    public double deposit;
    public double rentPrice;

    public String status; // HIEU_LUC / KET_THUC

    // Dịch vụ khách đăng ký dùng
    public boolean useWifi = true;
    public boolean useTrash = true;
    public boolean useServiceFee = true;
}