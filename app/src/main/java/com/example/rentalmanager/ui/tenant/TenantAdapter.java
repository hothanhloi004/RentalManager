package com.example.rentalmanager.ui.tenant;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.entity.TenantEntity;

import java.util.ArrayList;
import java.util.List;

public class TenantAdapter extends ListAdapter<TenantEntity, TenantAdapter.ViewHolder> {

    public TenantAdapter() {
        super(new DiffUtil.ItemCallback<TenantEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull TenantEntity oldItem, @NonNull TenantEntity newItem) {
                return oldItem.tenantId == newItem.tenantId;
            }

            @Override
            public boolean areContentsTheSame(@NonNull TenantEntity oldItem, @NonNull TenantEntity newItem) {
                return oldItem.fullName.equals(newItem.fullName) &&
                        oldItem.phone.equals(newItem.phone) &&
                        oldItem.cccd.equals(newItem.cccd);
            }
        });
    }

    public interface OnTenantClickListener {
        void onEdit(TenantEntity tenant);
        void onDelete(TenantEntity tenant);
    }

    private OnTenantClickListener listener;

    public void setOnTenantClickListener(OnTenantClickListener l) { this.listener = l; }

    public void setData(List<TenantEntity> data) {
        submitList(data != null ? new ArrayList<>(data) : null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tenant, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TenantEntity t = getItem(position);
        String name = t.fullName != null ? t.fullName : "---";
        holder.tvName.setText(name);
        holder.tvPhone.setText(t.phone != null ? t.phone : "---");
        
        // Vẫn hiển thị CCCD vì Data Object không chứa thông tin Phòng
        holder.tvCccd.setText("CCCD: " + (t.cccd != null ? t.cccd : "---"));

        // Single click → open TenantDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TenantDetailActivity.class);
            intent.putExtra(TenantDetailActivity.EXTRA_TENANT_ID, t.tenantId);
            v.getContext().startActivity(intent);
        });

        // Quick call button
        holder.btnMore.setOnClickListener(v -> {
            String phone = t.phone;
            if (phone != null && !phone.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + phone));
                v.getContext().startActivity(callIntent);
            }
        });

        // Long click → popup Edit/Delete
        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add(0, 1, 0, "✏️ Chỉnh sửa");
            popup.getMenu().add(0, 2, 1, "🗑️ Xóa");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1 && listener != null) { listener.onEdit(t); return true; }
                if (item.getItemId() == 2 && listener != null) { listener.onDelete(t); return true; }
                return false;
            });
            popup.show();
            return true;
        });
    }



    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvCccd;
        ImageView tvAvatar, btnMore;
        ViewHolder(View v) {
            super(v);
            tvName   = v.findViewById(R.id.tvName);
            tvPhone  = v.findViewById(R.id.tvPhone);
            tvCccd   = v.findViewById(R.id.tvCccd);
            tvAvatar = v.findViewById(R.id.tvAvatar);
            btnMore  = v.findViewById(R.id.btnMore);
        }
    }
}