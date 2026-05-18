package com.example.rentalmanager.ui.room;

import android.content.Context;
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
import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.model.RoomWithTenant;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoomAdapter extends ListAdapter<RoomWithTenant, RoomAdapter.RoomViewHolder> {
    private final NumberFormat moneyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat contractDateFormatter =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public RoomAdapter() {
        super(new DiffUtil.ItemCallback<RoomWithTenant>() {
            @Override
            public boolean areItemsTheSame(@NonNull RoomWithTenant oldItem, @NonNull RoomWithTenant newItem) {
                return oldItem.roomId == newItem.roomId;
            }

            @Override
            public boolean areContentsTheSame(@NonNull RoomWithTenant oldItem, @NonNull RoomWithTenant newItem) {
                boolean noteSame = (oldItem.note == null && newItem.note == null)
                        || (oldItem.note != null && oldItem.note.equals(newItem.note));
                return oldItem.roomName.equals(newItem.roomName)
                        && oldItem.price == newItem.price
                        && oldItem.status.equals(newItem.status)
                        && noteSame
                        && ((oldItem.contractId == null && newItem.contractId == null)
                        || (oldItem.contractId != null && oldItem.contractId.equals(newItem.contractId)));
            }
        });
    }

    public interface OnRoomClickListener {
        void onClick(RoomWithTenant room);
    }

    public interface OnRoomLongClickListener {
        void onLongClick(RoomEntity room);
    }

    public interface OnRoomDeleteListener {
        void onDelete(RoomWithTenant room);
    }

    private OnRoomClickListener clickListener;
    private OnRoomLongClickListener longClickListener;
    private OnRoomDeleteListener deleteListener;

    public void setOnRoomClickListener(OnRoomClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnRoomLongClickListener(OnRoomLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnRoomDeleteListener(OnRoomDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setRooms(List<RoomWithTenant> rooms) {
        submitList(rooms != null ? new ArrayList<>(rooms) : null);
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        RoomWithTenant item = getItem(position);

        holder.txtName.setText(item.roomName);
        holder.txtPrice.setText(formatMoney(item.price) + "/tháng");
        if (item.note != null && !item.note.isEmpty()) {
            holder.txtNote.setText("Ghi chú: " + item.note);
            holder.txtNote.setVisibility(View.VISIBLE);
        } else {
            holder.txtNote.setVisibility(View.GONE);
        }

        Context ctx = holder.itemView.getContext();
        if ("TRONG".equals(item.status)) {
            holder.chipStatus.setText("Trống");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_vacant_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_vacant));
        } else if ("BAO_TRI".equals(item.status)) {
            holder.chipStatus.setText("Bảo trì");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_maintenance_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_maintenance));
        } else {
            holder.chipStatus.setText("Đang thuê");
            holder.chipStatus.setChipBackgroundColorResource(R.color.status_active_bg);
            holder.chipStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_active));
        }

        if (item.contractId != null) {
            holder.txtTenant.setText(item.tenantName != null ? item.tenantName : "");
            holder.txtTenant.setVisibility(View.VISIBLE);

            if (item.startDate != null) {
                String dateStr = contractDateFormatter.format(new java.util.Date(item.startDate));
                holder.txtContractInfo.setText("Từ: " + dateStr);
                holder.txtContractInfo.setVisibility(View.VISIBLE);
            } else {
                holder.txtContractInfo.setVisibility(View.GONE);
            }
        } else {
            holder.txtTenant.setText("Chưa có người thuê");
            holder.txtTenant.setVisibility(View.VISIBLE);
            holder.txtContractInfo.setVisibility(View.GONE);
        }

        holder.btnViewDetail.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    holder.itemView.getContext(), RoomDetailActivity.class);
            intent.putExtra(RoomDetailActivity.EXTRA_ROOM_ID, item.roomId);
            holder.itemView.getContext().startActivity(intent);
        });
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(
                    holder.itemView.getContext(), RoomDetailActivity.class);
            intent.putExtra(RoomDetailActivity.EXTRA_ROOM_ID, item.roomId);
            holder.itemView.getContext().startActivity(intent);
        });

        holder.btnEditRoom.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(item);
        });

        holder.btnDeleteRoom.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(item);
        });
        // Ẩn xóa nếu phòng đang thuê
        holder.btnDeleteRoom.setVisibility("DANG_THUE".equals(item.status) ? android.view.View.INVISIBLE : android.view.View.VISIBLE);

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                RoomEntity room = new RoomEntity();
                room.roomId = item.roomId;
                room.roomName = item.roomName;
                room.price = item.price;
                room.status = item.status;
                room.note = item.note;

                longClickListener.onLongClick(room);
            }
            return true;
        });
    }

    private String formatMoney(double amount) {
        return moneyFormatter.format(amount) + " đ";
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtPrice, txtTenant, txtContractInfo, txtNote;
        Chip chipStatus;
        View btnViewDetail, btnEditRoom, btnDeleteRoom;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);

            txtName = itemView.findViewById(R.id.txtRoomName);
            txtPrice = itemView.findViewById(R.id.txtRoomPrice);
            chipStatus = itemView.findViewById(R.id.txtRoomStatus);
            txtTenant = itemView.findViewById(R.id.txtTenant);
            txtContractInfo = itemView.findViewById(R.id.txtContractInfo);
            txtNote = itemView.findViewById(R.id.txtRoomNote);
            btnViewDetail = itemView.findViewById(R.id.btnViewDetail);
            btnEditRoom = itemView.findViewById(R.id.btnEditRoom);
            btnDeleteRoom = itemView.findViewById(R.id.btnDeleteRoom);
        }
    }
}
