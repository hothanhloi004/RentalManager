package com.example.rentalmanager.ui.contract;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.model.ContractWithInfo;
import com.example.rentalmanager.util.ContractStatus;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ContractAdapter extends ListAdapter<ContractWithInfo, ContractAdapter.ViewHolder> {

    public ContractAdapter() {
        super(new DiffUtil.ItemCallback<ContractWithInfo>() {
            @Override
            public boolean areItemsTheSame(@NonNull ContractWithInfo oldItem, @NonNull ContractWithInfo newItem) {
                return oldItem.contractId == newItem.contractId;
            }

            @Override
            public boolean areContentsTheSame(@NonNull ContractWithInfo oldItem, @NonNull ContractWithInfo newItem) {
                return oldItem.roomName.equals(newItem.roomName) &&
                        oldItem.tenantName.equals(newItem.tenantName) &&
                        oldItem.deposit == newItem.deposit &&
                        oldItem.rentPrice == newItem.rentPrice &&
                        oldItem.status.equals(newItem.status);
            }
        });
    }

    private OnEndClickListener endListener;
    private OnViewClickListener viewListener;
    private OnEditServiceClickListener editServiceListener;

    public interface OnEndClickListener {
        void onEndClick(int contractId);
    }

    public interface OnViewClickListener {
        void onViewClick(ContractWithInfo contract);
    }

    public interface OnEditServiceClickListener {
        void onEditServiceClick(ContractWithInfo contract);
    }

    public void setOnEndClickListener(OnEndClickListener listener) {
        this.endListener = listener;
    }

    public void setOnViewClickListener(OnViewClickListener listener) {
        this.viewListener = listener;
    }

    public void setOnEditServiceClickListener(OnEditServiceClickListener listener) {
        this.editServiceListener = listener;
    }

    public void setData(List<ContractWithInfo> data) {
        submitList(data != null ? new ArrayList<>(data) : null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contract, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContractWithInfo c = getItem(position);
        NumberFormat format = NumberFormat.getInstance(new Locale("vi","VN"));

        String deposit = format.format(c.deposit) + "đ";

        holder.tvRoom.setText(c.roomName);
        holder.tvTenant.setText(c.tenantName);
        holder.tvDeposit.setText(deposit);

        // Service icons - dynamic visibility
        holder.layoutWifi.setVisibility(c.useWifi ? View.VISIBLE : View.GONE);
        holder.layoutTrash.setVisibility(c.useTrash ? View.VISIBLE : View.GONE);
        holder.layoutServiceFee.setVisibility(c.useServiceFee ? View.VISIBLE : View.GONE);

        Context ctx = holder.itemView.getContext();
        if (ContractStatus.HIEU_LUC.equals(c.status)) {
            holder.chipStatus.setText("Hiệu lực");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_active_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_active));
            holder.btnEnd.setVisibility(View.VISIBLE);
            holder.btnEditService.setVisibility(View.VISIBLE);
            holder.btnQuickCall.setVisibility(View.VISIBLE);
        } else {
            holder.chipStatus.setText("Đã kết thúc");
            holder.chipStatus.setChipBackgroundColorResource(R.color.outline_variant);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.on_surface_variant));
            holder.btnEnd.setVisibility(View.GONE);
            holder.btnEditService.setVisibility(View.GONE);
            holder.btnQuickCall.setVisibility(View.GONE);
        }

        holder.btnEditService.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ContractWithInfo contract = getItem(pos);
            if (editServiceListener != null) {
                editServiceListener.onEditServiceClick(contract);
            }
        });

        holder.btnEnd.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (endListener != null) {
                endListener.onEndClick(getItem(pos).contractId);
            }
        });

        holder.btnQuickCall.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ContractWithInfo contract = getItem(pos);
            if (contract.tenantPhone != null && !contract.tenantPhone.trim().isEmpty()) {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
                intent.setData(android.net.Uri.parse("tel:" + contract.tenantPhone.trim()));
                ctx.startActivity(intent);
            } else {
                Toast.makeText(ctx, "Khách chưa có số điện thoại", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ContractWithInfo contract = getItem(pos);
            if (viewListener != null) {
                viewListener.onViewClick(contract);
            } else {
                Toast.makeText(v.getContext(), "Xem hợp đồng: " + contract.roomName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoom;
        TextView tvTenant;
        TextView tvDeposit;
        Chip chipStatus;
        MaterialButton btnEnd;
        MaterialButton btnEditService;
        LinearLayout layoutWifi;
        LinearLayout layoutTrash;
        LinearLayout layoutServiceFee;
        android.widget.ImageButton btnQuickCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvTenant = itemView.findViewById(R.id.tvTenant);
            tvDeposit = itemView.findViewById(R.id.tvDeposit);
            chipStatus = itemView.findViewById(R.id.tvStatus);
            btnEnd = itemView.findViewById(R.id.btnEnd);
            btnEditService = itemView.findViewById(R.id.btnEditService);
            layoutWifi = itemView.findViewById(R.id.layoutWifi);
            layoutTrash = itemView.findViewById(R.id.layoutTrash);
            layoutServiceFee = itemView.findViewById(R.id.layoutServiceFee);
            btnQuickCall = itemView.findViewById(R.id.btnQuickCall);
        }
    }
}