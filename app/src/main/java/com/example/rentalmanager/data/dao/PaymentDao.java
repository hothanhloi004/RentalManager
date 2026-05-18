package com.example.rentalmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.rentalmanager.data.entity.PaymentEntity;

import java.util.List;

@Dao
public interface PaymentDao {

    @Insert
    void insert(PaymentEntity payment);

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    List<PaymentEntity> getAllPaymentsSync();

    @Query("SELECT * FROM payments WHERE billId = :billId ORDER BY paymentDate DESC")
    LiveData<List<PaymentEntity>> getPaymentsByBill(int billId);

    @Query("SELECT IFNULL(SUM(amount),0) FROM payments WHERE billId = :billId")
    double sumPayments(int billId);
}
