package com.example.rentalmanager.ui.contract;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.dao.BillDao;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.model.BillWithInfo;
import com.example.rentalmanager.data.model.ContractWithInfo;
import com.example.rentalmanager.util.AppExecutors;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContractDetailDialog {

    public static void show(Context context, ContractWithInfo c) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_contract_detail, null);

        TextView tvRoom = view.findViewById(R.id.tvRoom);
        TextView tvTenant = view.findViewById(R.id.tvTenant);
        TextView tvRent = view.findViewById(R.id.tvRent);
        TextView tvDeposit = view.findViewById(R.id.tvDeposit);
        TextView tvStart = view.findViewById(R.id.tvStart);
        TextView tvEnd = view.findViewById(R.id.tvEnd);
        TextView tvStatus = view.findViewById(R.id.tvStatus);
        TextView tvTotalBill = view.findViewById(R.id.tvTotalBill);
        TextView tvPaid = view.findViewById(R.id.tvPaid);
        TextView tvDebt = view.findViewById(R.id.tvDebt);

        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        tvRoom.setText("Ph\u00f2ng: " + c.roomName);
        tvTenant.setText("Ng\u01b0\u1eddi thu\u00ea: " + c.tenantName);
        tvRent.setText("Gi\u00e1 thu\u00ea: " + format.format(c.rentPrice) + " \u0111");
        tvDeposit.setText("Ti\u1ec1n c\u1ecdc: " + format.format(c.deposit) + " \u0111");
        tvStart.setText("B\u1eaft \u0111\u1ea7u: " + sdf.format(new Date(c.startDate)));

        if (c.endDate != null) {
            tvEnd.setText("K\u1ebft th\u00fac: " + sdf.format(new Date(c.endDate)));
        } else {
            tvEnd.setText("K\u1ebft th\u00fac: Ch\u01b0a c\u00f3");
        }

        String displayStatus = "HIEU_LUC".equals(c.status)
                ? "\u0110ang hi\u1ec7u l\u1ef1c"
                : ("KET_THUC".equals(c.status) ? "\u0110\u00e3 k\u1ebft th\u00fac" : c.status);
        tvStatus.setText("Tr\u1ea1ng th\u00e1i: " + displayStatus);

        BillDao billDao = AppDatabase.getInstance(context).billDao();
        AppExecutors.getInstance().diskIO().execute(() -> {
            BillWithInfo bill = billDao.getLatestBillWithInfo(c.contractId);
            view.post(() -> {
                if (bill != null) {
                    double remaining = bill.totalAmount - bill.totalPaid;
                    tvTotalBill.setText("T\u1ed5ng h\u00f3a \u0111\u01a1n (" + bill.month + "): " + format.format(bill.totalAmount) + " \u0111");
                    tvPaid.setText("\u0110\u00e3 tr\u1ea3: " + format.format(bill.totalPaid) + " \u0111");
                    tvDebt.setText("C\u00f2n n\u1ee3: " + format.format(remaining) + " \u0111");
                } else {
                    tvTotalBill.setText("Ch\u01b0a c\u00f3 h\u00f3a \u0111\u01a1n");
                    tvPaid.setText("");
                    tvDebt.setText("");
                }
            });
        });

        new AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton("\u0110\u00f3ng", null)
                .show();
    }
}