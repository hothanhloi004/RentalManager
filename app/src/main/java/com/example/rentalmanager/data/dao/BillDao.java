package com.example.rentalmanager.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.model.BillSummary;
import com.example.rentalmanager.data.model.BillWithInfo;

import java.util.List;

@Dao
public interface BillDao {
    @Insert
    long insert(BillEntity bill);

    @Update
    void update(BillEntity bill);

    @Query("UPDATE bills SET paymentStatus = :status WHERE billId = :billId")
    void updateStatus(int billId, String status);

    @Query("UPDATE bills SET paymentStatus = :status, paidAt = :paidAt WHERE billId = :billId")
    void updatePaymentState(int billId, String status, Long paidAt);

    @Query("UPDATE bills SET paymentStatus = 'DA_THANH_TOAN', paidAt = :paidAt WHERE billId = :billId")
    void markAsPaid(int billId, long paidAt);

    @Query("UPDATE bills SET oldElectric=:oldE,newElectric=:newE,oldWater=:oldW,newWater=:newW,totalAmount=:total,meterUpdated=1 WHERE billId=:billId")
    void updateBillMeter(int billId,int oldE,int newE,int oldW,int newW,double total);

    @Query("UPDATE bills SET oldElectric=:oldE,newElectric=:newE,oldWater=:oldW,newWater=:newW,totalAmount=:total, electricPrice=:ePrice, waterPrice=:wPrice, rentPrice=:rPrice, serviceFee=:sFee, meterUpdated=1 WHERE billId=:billId")
    void updateBillMeterFull(int billId, int oldE, int newE, int oldW, int newW, double total, double ePrice, double wPrice, double rPrice, double sFee);

    @Delete
    void delete(BillEntity bill);

    @Query("SELECT * FROM bills ORDER BY month DESC")
    LiveData<List<BillEntity>> getAllBills();

    @Query("SELECT * FROM bills ORDER BY month DESC")
    List<BillEntity> getAllBillsSync();

    @Query("SELECT * FROM bills WHERE billId = :billId LIMIT 1")
    BillEntity getBillById(int billId);

    @Query("SELECT * FROM bills WHERE billId = :billId LIMIT 1")
    BillEntity getBillByIdSync(int billId);

    @Query("SELECT COUNT(*) FROM bills WHERE contractId = :contractId AND month = :month")
    int countByContractAndMonth(int contractId, String month);

    @Query("SELECT * FROM bills WHERE paymentStatus = 'CHUA_THANH_TOAN' ORDER BY dueDate ASC")
    LiveData<List<BillEntity>> getUnpaidBills();

    @Query(
        "SELECT bills.billId, bills.contractId, bills.month, bills.oldElectric, bills.newElectric, " +
        "bills.oldWater, bills.newWater, bills.electricPrice, bills.waterPrice, bills.rentPrice, bills.serviceFee, bills.totalAmount, bills.paymentStatus, bills.dueDate, bills.paidAt, " +
        "bills.meterUpdated AS meterUpdated, rooms.roomName AS roomName, tenants.fullName AS tenantName, " +
        "IFNULL(SUM(payments.amount),0) AS totalPaid " +
        "FROM bills " +
        "INNER JOIN contracts ON bills.contractId = contracts.contractId " +
        "INNER JOIN rooms ON contracts.roomId = rooms.roomId " +
        "INNER JOIN tenants ON contracts.tenantId = tenants.tenantId " +
        "LEFT JOIN payments ON payments.billId = bills.billId " +
        "GROUP BY bills.billId ORDER BY bills.dueDate DESC"
    )
    LiveData<List<BillWithInfo>> getBillsWithInfo();

    @Query(
        "SELECT b.billId, b.contractId, b.month, b.oldElectric, b.newElectric, " +
        "b.oldWater, b.newWater, b.electricPrice, b.waterPrice, b.rentPrice, b.serviceFee, b.totalAmount, b.paymentStatus, b.dueDate, b.paidAt, " +
        "b.meterUpdated AS meterUpdated, r.roomName AS roomName, t.fullName AS tenantName, " +
        "IFNULL(SUM(p.amount),0) AS totalPaid " +
        "FROM bills b " +
        "INNER JOIN contracts c ON b.contractId = c.contractId " +
        "INNER JOIN rooms r ON c.roomId = r.roomId " +
        "INNER JOIN tenants t ON c.tenantId = t.tenantId " +
        "LEFT JOIN payments p ON p.billId = b.billId " +
        "WHERE b.billId = (SELECT billId FROM bills WHERE contractId = :contractId ORDER BY month DESC LIMIT 1)"
    )
    BillWithInfo getLatestBillWithInfo(int contractId);

    @Query("SELECT * FROM bills WHERE contractId = :contractId ORDER BY month DESC LIMIT 1")
    BillEntity getLastBill(int contractId);

    @Query(
        "SELECT SUM(b.totalAmount) AS totalAmount, IFNULL(SUM(p.amount),0) AS totalPaid, " +
        "SUM(b.totalAmount) - IFNULL(SUM(p.amount),0) AS remaining " +
        "FROM bills b LEFT JOIN payments p ON b.billId = p.billId " +
        "WHERE b.contractId = :contractId"
    )
    BillSummary getBillSummary(int contractId);

    @Query(
        "SELECT IFNULL(SUM(" +
        "CASE " +
        "WHEN b.paymentStatus = 'DA_THANH_TOAN' THEN b.totalAmount " +
        "ELSE IFNULL(p.totalPaid, 0) " +
        "END" +
        "), 0) " +
        "FROM bills b " +
        "LEFT JOIN (" +
        "SELECT billId, SUM(amount) AS totalPaid " +
        "FROM payments GROUP BY billId" +
        ") p ON p.billId = b.billId " +
        "WHERE b.month = :month"
    )
    double getRevenueByMonth(String month);

    @Query(
        "SELECT IFNULL(SUM(" +
        "CASE " +
        "WHEN b.paymentStatus = 'DA_THANH_TOAN' THEN 0 " +
        "ELSE MAX(b.totalAmount - IFNULL(p.totalPaid, 0), 0) " +
        "END" +
        "), 0) " +
        "FROM bills b " +
        "LEFT JOIN (" +
        "SELECT billId, SUM(amount) AS totalPaid " +
        "FROM payments GROUP BY billId" +
        ") p ON p.billId = b.billId " +
        "WHERE b.month = :month"
    )
    double getDebtByMonth(String month);

    @Query(
        "SELECT IFNULL(SUM(" +
        "CASE " +
        "WHEN b.paymentStatus = 'DA_THANH_TOAN' THEN 0 " +
        "ELSE MAX(b.totalAmount - IFNULL(p.totalPaid, 0), 0) " +
        "END" +
        "), 0) " +
        "FROM bills b " +
        "LEFT JOIN (" +
        "SELECT billId, SUM(amount) AS totalPaid " +
        "FROM payments GROUP BY billId" +
        ") p ON p.billId = b.billId"
    )
    double getTotalDebt();

    @Query("SELECT COUNT(*) FROM bills WHERE month = :month AND paymentStatus != 'DA_THANH_TOAN'")
    int countUnpaidBills(String month);

    @Query("SELECT COUNT(*) FROM bills WHERE paymentStatus != 'DA_THANH_TOAN'")
    int countTotalUnpaidBills();

    @Query("SELECT IFNULL(SUM(newElectric - oldElectric),0) FROM bills WHERE month = :month AND meterUpdated = 1")
    int getElectricUsedByMonth(String month);

    @Query("SELECT IFNULL(SUM(newWater - oldWater),0) FROM bills WHERE month = :month AND meterUpdated = 1")
    int getWaterUsedByMonth(String month);

    @Query("SELECT COUNT(*) FROM bills WHERE dueDate < :today AND paymentStatus != 'DA_THANH_TOAN'")
    int countOverdueBills(long today);

    @Query("SELECT COUNT(*) FROM bills WHERE dueDate < :today AND paymentStatus != 'DA_THANH_TOAN' AND month = :month")
    int countOverdueBillsByMonth(long today, String month);

    @Query("SELECT * FROM bills WHERE dueDate < :today AND paymentStatus != 'DA_THANH_TOAN' ORDER BY dueDate ASC")
    List<BillEntity> getOverdueBills(long today);

    @Query("SELECT COUNT(*) FROM bills WHERE meterUpdated = 0 AND month = :month")
    int countMeterNotUpdatedBills(String month);

    @Query("SELECT COUNT(*) FROM bills WHERE meterUpdated = 0")
    int countTotalMeterNotUpdatedBills();

    // LỊCH SỬ HOÁ ĐƠN THEO KHÁCH THUÊ
    @Query(
        "SELECT b.billId, b.contractId, b.month, b.oldElectric, b.newElectric, " +
        "b.oldWater, b.newWater, b.electricPrice, b.waterPrice, b.rentPrice, b.serviceFee, b.totalAmount, b.paymentStatus, b.dueDate, b.paidAt, " +
        "b.meterUpdated AS meterUpdated, r.roomName AS roomName, t.fullName AS tenantName, " +
        "IFNULL(SUM(p.amount),0) AS totalPaid " +
        "FROM bills b " +
        "INNER JOIN contracts c ON b.contractId = c.contractId " +
        "INNER JOIN rooms r ON c.roomId = r.roomId " +
        "INNER JOIN tenants t ON c.tenantId = t.tenantId " +
        "LEFT JOIN payments p ON p.billId = b.billId " +
        "WHERE c.tenantId = :tenantId " +
        "GROUP BY b.billId ORDER BY b.month DESC"
    )
    LiveData<List<BillWithInfo>> getBillsByTenant(int tenantId);

    @Query("SELECT MAX(month) FROM bills")
    String getMaxMonth();

    @Query("SELECT COUNT(*) FROM bills WHERE contractId = :contractId")
    int countBillsByContractSync(int contractId);
}
