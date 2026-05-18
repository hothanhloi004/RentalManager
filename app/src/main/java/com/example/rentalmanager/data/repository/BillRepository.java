package com.example.rentalmanager.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.rentalmanager.data.dao.BillDao;
import com.example.rentalmanager.data.dao.PaymentDao;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.entity.PaymentEntity;
import com.example.rentalmanager.data.model.BillWithInfo;
import com.example.rentalmanager.util.BillStatus;
import com.example.rentalmanager.data.dao.ContractDao;
import com.example.rentalmanager.data.model.ActiveContractInfo;
import com.example.rentalmanager.data.model.BillSummary;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;
import com.example.rentalmanager.util.AppExecutors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class BillRepository {
    private final ContractDao contractDao;
    private final BillDao billDao;
    private final PaymentDao paymentDao;
    private final Executor executorService;
    private final AppDatabase db;

    public BillRepository(Application application) {

        db = AppDatabase.getInstance(application);

        billDao = db.billDao();
        paymentDao = db.paymentDao();
        contractDao = db.contractDao();

        executorService = AppExecutors.getInstance().diskIO();
    }

    // ==========================
    // GET DATA
    // ==========================

    public LiveData<List<BillEntity>> getAllBills() {
        return billDao.getAllBills();
    }

    public LiveData<List<BillEntity>> getUnpaidBills() {
        return billDao.getUnpaidBills();
    }

    public LiveData<List<BillWithInfo>> getBillsWithInfo() {
        return billDao.getBillsWithInfo();
    }

    // ==========================
    // CREATE BILL
    // ==========================

    public void createBill(BillEntity bill, OperationCallback callback) {

        executorService.execute(() -> {

            if (bill.newElectric < bill.oldElectric) {
                callback.onComplete(false, "Chỉ số điện không hợp lệ");
                return;
            }

            if (bill.newWater < bill.oldWater) {
                callback.onComplete(false, "Chỉ số nước không hợp lệ");
                return;
            }

            // 1. CHUẨN HÓA MONTH TRƯỚC
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-M", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

                Date date = inputFormat.parse(bill.month);
                bill.month = outputFormat.format(date);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onComplete(false, "Lỗi định dạng tháng");
                return;
            }

            // 2. CHECK SAU KHI CHUẨN HÓA
            int count = billDao.countByContractAndMonth(
                    bill.contractId,
                    bill.month
            );

            if (count > 0) {
                callback.onComplete(false, "Tháng này đã có hóa đơn");
                return;
            }

            bill.paymentStatus = BillStatus.CHUA_THANH_TOAN;
            bill.paidAt = null;
            bill.meterUpdated = true;

            billDao.insert(bill);

            callback.onComplete(true, "Tạo hóa đơn thành công");
        });
    }

    // ==========================
    // ADD PAYMENT (TRẢ TIỀN)
    // ==========================

    public void addPayment(int billId, double amount, OperationCallback callback) {

        executorService.execute(() -> {

            BillEntity bill = billDao.getBillByIdSync(billId);

            if (bill == null) {
                callback.onComplete(false, "Không tìm thấy hóa đơn");
                return;
            }

            double totalPaid = paymentDao.sumPayments(billId);
            double remaining = bill.totalAmount - totalPaid;

            // ❌ không cho trả dư
            if (amount > remaining) {
                callback.onComplete(false, "Số tiền vượt quá số còn nợ");
                return;
            }

            long paymentTime = System.currentTimeMillis();

            PaymentEntity payment = new PaymentEntity();
            payment.billId = billId;
            payment.amount = amount;
            payment.paymentDate = paymentTime;

            paymentDao.insert(payment);

            double newTotalPaid = totalPaid + amount;

            String status;

            if (newTotalPaid == 0) {
                status = BillStatus.CHUA_THANH_TOAN;
            } else if (newTotalPaid < bill.totalAmount) {
                status = BillStatus.DONG_THIEU;
            } else {
                status = BillStatus.DA_THANH_TOAN;
            }

            Long paidAt = BillStatus.DA_THANH_TOAN.equals(status) ? paymentTime : null;
            billDao.updatePaymentState(billId, status, paidAt);

            callback.onComplete(true, "Thanh toán thành công");
        });
    }

    public void generateBills(String month, OperationCallback callback){

        executorService.execute(() -> {

            boolean hasInserted = false;

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-M", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

                Date parsedDate = inputFormat.parse(month);
                String normalizedMonth = outputFormat.format(parsedDate);

                List<ActiveContractInfo> contracts =
                        contractDao.getActiveContractInfoSync();

                // Lấy Setting 1 lần duy nhất bên ngoài vòng lặp
                com.example.rentalmanager.data.entity.SettingEntity setting =
                        db.settingDao().getSetting();

                for (ActiveContractInfo c : contracts) {

                    int count = billDao.countByContractAndMonth(
                            c.contractId,
                            normalizedMonth
                    );

                    if (count > 0) continue;
                    
                    double ePrice = (setting != null && setting.electricPrice > 0) ? setting.electricPrice : 3500;
                    double wPrice = (setting != null && setting.waterPrice > 0) ? setting.waterPrice : 20000;
                    double fixedFees = calculateServiceFee(
                            setting,
                            c.useWifi,
                            c.useTrash,
                            c.useServiceFee
                    );

                    BillEntity bill = new BillEntity();

                    bill.contractId = c.contractId;
                    bill.month = normalizedMonth;

                    BillEntity lastBill = db.billDao().getLastBill(c.contractId);
                    int previousE = (lastBill != null) ? lastBill.newElectric : 0;
                    int previousW = (lastBill != null) ? lastBill.newWater : 0;

                    bill.oldElectric = previousE;
                    bill.newElectric = previousE;
                    bill.oldWater = previousW;
                    bill.newWater = previousW;
                    
                    bill.electricPrice = ePrice;
                    bill.waterPrice = wPrice;
                    bill.serviceFee = fixedFees;
                    bill.rentPrice = c.rentPrice;

                    bill.totalAmount = c.rentPrice + fixedFees;

                    bill.paymentStatus = BillStatus.CHUA_THANH_TOAN;
                    bill.meterUpdated = false;
                    bill.paidAt = null;

                    // ✅ set dueDate đúng tháng
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                        Date monthDate = sdf.parse(normalizedMonth);

                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(monthDate);

                        calendar.set(Calendar.DAY_OF_MONTH,
                                calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

                        bill.dueDate = calendar.getTimeInMillis();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        billDao.insert(bill);
                        hasInserted = true; // ✅ có tạo
                    } catch (Exception e) {
                        e.printStackTrace(); // trùng thì bỏ qua
                    }
                }

                // 🔥 TRẢ KẾT QUẢ
                if (hasInserted) {
                    callback.onComplete(true, "Đã tạo hóa đơn tháng " + month);
                } else {
                    callback.onComplete(false, "Tháng này đã có hóa đơn rồi");
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onComplete(false, "Lỗi tạo hóa đơn");
            }
        });
    }

    public interface OperationCallback {
        void onComplete(boolean success, String message);
    }

    public void updateBillMeter(int billId,int oldE,int newE,int oldW,int newW){

        executorService.execute(() -> {

            BillEntity bill = billDao.getBillByIdSync(billId);

            if(bill == null) return;

            int electricUsed = Math.max(0, newE - oldE);
            int waterUsed = Math.max(0, newW - oldW);

            double electricPrice = bill.electricPrice;
            double waterPrice = bill.waterPrice;
            double rentPrice = bill.rentPrice;
            double serviceFee = bill.serviceFee;

            com.example.rentalmanager.data.entity.SettingEntity setting = db.settingDao().getSetting();

            if (rentPrice <= 0) {
                com.example.rentalmanager.data.entity.ContractEntity c = db.contractDao().getContractByIdSync(bill.contractId);
                if (c != null && c.rentPrice > 0) {
                    rentPrice = c.rentPrice;
                }
            }

            com.example.rentalmanager.data.entity.ContractEntity contract =
                    db.contractDao().getContractByIdSync(bill.contractId);

            // Tính lại phí dịch vụ theo đúng cờ đăng ký của hợp đồng,
            // tránh trường hợp hợp đồng cũ chưa bật dịch vụ nhưng vẫn bị cộng toàn bộ.
            if (contract != null) {
                serviceFee = calculateServiceFee(
                        setting,
                        contract.useWifi,
                        contract.useTrash,
                        contract.useServiceFee
                );
            }

            if (electricPrice <= 0 || waterPrice <= 0) {
                if (setting != null) {
                    if (electricPrice <= 0) electricPrice = setting.electricPrice > 0 ? setting.electricPrice : 3500;
                    if (waterPrice <= 0) waterPrice = setting.waterPrice > 0 ? setting.waterPrice : 20000;
                } else {
                    if (electricPrice <= 0) electricPrice = 3500;
                    if (waterPrice <= 0) waterPrice = 20000;
                }
            }

            double total =
                    rentPrice + serviceFee +
                            (electricUsed * electricPrice) +
                            (waterUsed * waterPrice);

            billDao.updateBillMeterFull(
                    billId,
                    oldE,
                    newE,
                    oldW,
                    newW,
                    total,
                    electricPrice,
                    waterPrice,
                    rentPrice,
                    serviceFee
            );

            double totalPaid = paymentDao.sumPayments(billId);
            String status;
            if (totalPaid == 0) {
                status = BillStatus.CHUA_THANH_TOAN;
            } else if (totalPaid < total) {
                status = BillStatus.DONG_THIEU;
            } else {
                status = BillStatus.DA_THANH_TOAN;
            }

            Long paidAt = BillStatus.DA_THANH_TOAN.equals(status)
                    ? (bill.paidAt != null ? bill.paidAt : System.currentTimeMillis())
                    : null;
            billDao.updatePaymentState(billId, status, paidAt);
        });
    }
    public BillEntity getBillById(int billId){
        return billDao.getBillById(billId);
    }
    public void getLastBill(int contractId, BillCallback callback){

        executorService.execute(() -> {

            BillEntity bill = billDao.getLastBill(contractId);

            callback.onResult(bill);
        });
    }

    public interface BillCallback{
        void onResult(BillEntity bill);
    }
    public BillSummary getBillSummary(int contractId) {
        return billDao.getBillSummary(contractId);
    }

    private double calculateServiceFee(
            com.example.rentalmanager.data.entity.SettingEntity setting,
            boolean useWifi,
            boolean useTrash,
            boolean useServiceFee
    ) {
        if (setting == null) {
            return 0;
        }

        double total = 0;
        if (useTrash) {
            total += setting.trashFee;
        }
        if (useWifi) {
            total += setting.wifiPrice;
        }
        if (useServiceFee) {
            total += setting.serviceFee;
        }
        return total;
    }
}
