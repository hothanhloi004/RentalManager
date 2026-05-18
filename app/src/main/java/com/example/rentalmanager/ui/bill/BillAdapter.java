package com.example.rentalmanager.ui.bill;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.model.BillWithInfo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillAdapter extends ListAdapter<BillWithInfo, BillAdapter.BillViewHolder> {
    private final NumberFormat moneyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat dueDateFormatter =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public BillAdapter() {
        super(new DiffUtil.ItemCallback<BillWithInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull BillWithInfo oldItem, @NonNull BillWithInfo newItem) {
                return oldItem.billId == newItem.billId;
            }

            @Override
            public boolean areContentsTheSame(@NonNull BillWithInfo oldItem, @NonNull BillWithInfo newItem) {
                return oldItem.totalAmount == newItem.totalAmount
                        && oldItem.totalPaid == newItem.totalPaid
                        && oldItem.meterUpdated == newItem.meterUpdated
                        && ((oldItem.paymentStatus == null && newItem.paymentStatus == null)
                        || (oldItem.paymentStatus != null && oldItem.paymentStatus.equals(newItem.paymentStatus)));
            }
        });
        setHasStableIds(true);
    }

    public interface BillListener {
        void onPay(BillWithInfo bill);
        void onViewDetail(BillWithInfo bill);
    }

    private BillListener listener;

    public void setListener(BillListener l) {
        listener = l;
    }

    public void setData(List<BillWithInfo> data) {
        submitList(data != null ? new java.util.ArrayList<>(data) : null);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).billId;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        BillWithInfo item = getItem(position);

        holder.txtTitle.setText(item.roomName + " - " + item.month);
        if (item.tenantName != null && !item.tenantName.isEmpty()) {
            holder.txtTenant.setText(item.tenantName);
            holder.txtTenant.setVisibility(View.VISIBLE);
        } else {
            holder.txtTenant.setVisibility(View.GONE);
        }

        double remaining = item.totalAmount - item.totalPaid;
        double displayedPaid = item.totalPaid;

        if ("DA_THANH_TOAN".equals(item.paymentStatus)) {
            remaining = 0;
            displayedPaid = item.totalAmount;
        }

        int cOnSurface = ContextCompat.getColor(holder.itemView.getContext(), R.color.on_surface);
        int cSuccess = ContextCompat.getColor(holder.itemView.getContext(), R.color.success);
        holder.txtTotalPaid.setText(buildTotalPaidText(item.totalAmount, displayedPaid, cOnSurface, cSuccess));

        if (!item.meterUpdated) {
            String remainStr = moneyFormatter.format(remaining) + "đ";
            holder.txtRemaining.setText(buildEmphasisText("Tạm tính: ", remainStr));
            holder.txtRemaining.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending));
        } else if (remaining > 0) {
            String remainStr = moneyFormatter.format(remaining) + "đ";
            holder.txtRemaining.setText(buildEmphasisText("Còn nợ: ", remainStr));
            holder.txtRemaining.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error));
        } else {
            holder.txtRemaining.setText("Hoàn tất");
            holder.txtRemaining.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success));
        }

        holder.txtDue.setText("Hạn thu: " + dueDateFormatter.format(new Date(item.dueDate)));

        Context ctx = holder.itemView.getContext();

        holder.btnView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(item);
            }
        });

        holder.btnPay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPay(item);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetail(item);
            }
        });

        if (!item.meterUpdated) {
            holder.chipStatus.setText("Chưa chốt");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_overdue_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_overdue));
            holder.btnPay.setText("Chốt số");
            holder.btnPay.setBackgroundTintList(ContextCompat.getColorStateList(ctx, R.color.status_overdue_bg));
            holder.btnPay.setTextColor(ContextCompat.getColor(ctx, R.color.status_overdue));
            holder.btnPay.setIconTint(ContextCompat.getColorStateList(ctx, R.color.status_overdue));
            holder.btnPay.setEnabled(true);
        } else if ("DA_THANH_TOAN".equals(item.paymentStatus)) {
            holder.chipStatus.setText("Hoàn tất");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_active_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_active));
            holder.btnPay.setText("Đã thu");
            holder.btnPay.setBackgroundTintList(ContextCompat.getColorStateList(ctx, R.color.outline));
            holder.btnPay.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant));
            holder.btnPay.setIconTint(ContextCompat.getColorStateList(ctx, R.color.on_surface_variant));
            holder.btnPay.setEnabled(false);
        } else if ("DONG_THIEU".equals(item.paymentStatus) && item.totalPaid > 0) {
            holder.chipStatus.setText("Thiếu");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_pending_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_pending));
            holder.btnPay.setText("Thu thêm");
            holder.btnPay.setBackgroundTintList(ContextCompat.getColorStateList(ctx, R.color.primary));
            holder.btnPay.setTextColor(Color.WHITE);
            holder.btnPay.setIconTint(ContextCompat.getColorStateList(ctx, R.color.white));
            holder.btnPay.setEnabled(true);
        } else {
            if (item.dueDate < System.currentTimeMillis()) {
                holder.chipStatus.setText("Quá hạn");
                holder.chipStatus.setChipBackgroundColorResource(R.color.status_overdue_bg);
                holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_overdue));
            } else {
                holder.chipStatus.setText("Chưa trả");
                holder.chipStatus.setChipBackgroundColorResource(R.color.status_overdue_bg);
                holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_overdue));
            }
            holder.btnPay.setText("Thu tiền");
            holder.btnPay.setBackgroundTintList(ContextCompat.getColorStateList(ctx, R.color.primary));
            holder.btnPay.setTextColor(Color.WHITE);
            holder.btnPay.setIconTint(ContextCompat.getColorStateList(ctx, R.color.white));
            holder.btnPay.setEnabled(true);
        }
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtTenant, txtTotalPaid, txtRemaining, txtDue;
        Chip chipStatus;
        MaterialButton btnPay, btnView;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtBillTitle);
            txtTenant = itemView.findViewById(R.id.txtTenantName);
            txtTotalPaid = itemView.findViewById(R.id.txtTotalPaid);
            txtRemaining = itemView.findViewById(R.id.txtRemaining);
            txtDue = itemView.findViewById(R.id.txtBillDue);
            chipStatus = itemView.findViewById(R.id.txtBillStatus);
            btnPay = itemView.findViewById(R.id.btnPay);
            btnView = itemView.findViewById(R.id.btnViewDetail);
        }
    }

    private CharSequence buildTotalPaidText(double totalAmount, double totalPaid, int totalColor, int paidColor) {
        String totalPrefix = "Tổng: ";
        String totalText = moneyFormatter.format(totalAmount) + "đ";
        String separator = "   Đã trả: ";
        String paidText = moneyFormatter.format(totalPaid) + "đ";

        SpannableStringBuilder builder = new SpannableStringBuilder()
                .append(totalPrefix)
                .append(totalText)
                .append(separator)
                .append(paidText);

        int totalStart = totalPrefix.length();
        int totalEnd = totalStart + totalText.length();
        int paidStart = totalEnd + separator.length();

        builder.setSpan(new StyleSpan(Typeface.BOLD), totalStart, totalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(totalColor), totalStart, totalEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(paidColor), paidStart, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private CharSequence buildEmphasisText(String prefix, String value) {
        SpannableStringBuilder builder = new SpannableStringBuilder(prefix).append(value);
        builder.setSpan(
                new StyleSpan(Typeface.BOLD),
                prefix.length(),
                builder.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return builder;
    }
}
