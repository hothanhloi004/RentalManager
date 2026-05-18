package com.example.rentalmanager.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "settings")
public class SettingEntity {

    @PrimaryKey
    public int id = 1;

    public double electricPrice = 3500;  // đ/kWh
    public double waterPrice = 20000;    // đ/m³
    public double serviceFee = 0;        // đ/tháng (phí dịch vụ chung)
    public double trashFee = 30000;      // đ/tháng (phí rác)
    public double wifiPrice = 0;         // đ/tháng (WiFi)

    // PIN lock
    public String pinCode = "";          // 6 ký tự, rỗng = tắt PIN
    public boolean pinEnabled = false;

    // Thông tin ngân hàng (VietQR)
    public String bankCode = "MB";        // Mã ngân hàng, nhập theo danh mục VietQR
    public String bankAccount = "";       // Số tài khoản

    // Thông tin chủ trọ / khu trọ
    public String hostelName = "";        // Tên khu trọ
    public String landlordName = "";      // Tên chủ trọ
    public String landlordPhone = "";     // Số điện thoại chủ trọ
    public String hostelAddress = "";     // Địa chỉ khu trọ
}