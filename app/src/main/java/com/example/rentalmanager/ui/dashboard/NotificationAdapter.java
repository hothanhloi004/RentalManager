package com.example.rentalmanager.ui.dashboard;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.model.InquiryModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<InquiryModel> list = new ArrayList<>();
    private final Context context;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onMarkReadClick(InquiryModel item);
        void onDeleteClick(InquiryModel item);
    }

    public NotificationAdapter(Context context, OnNotificationClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void submitList(List<InquiryModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InquiryModel item = list.get(position);

        holder.tvSenderName.setText(item.getName());
        holder.tvRoomName.setText(item.getRoomName() != null ? item.getRoomName() : "Phòng quan tâm");
        holder.tvPhone.setText(item.getPhone());

        if (item.getNote() != null && !item.getNote().isEmpty()) {
            holder.tvNote.setVisibility(View.VISIBLE);
            holder.tvNote.setText("Lời nhắn: " + item.getNote());
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        if (item.getCreatedAt() != null) {
            long timeMillis = item.getCreatedAt().toDate().getTime();
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(timeMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        if (item.isRead()) {
            holder.viewUnreadDot.setVisibility(View.GONE);
            holder.tvBadge.setVisibility(View.GONE);
            holder.btnMarkRead.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.6f);
        } else {
            holder.viewUnreadDot.setVisibility(View.VISIBLE);
            holder.tvBadge.setVisibility(View.VISIBLE);
            holder.btnMarkRead.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(1.0f);
        }

        holder.btnMarkRead.setOnClickListener(v -> {
            if (listener != null) listener.onMarkReadClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewUnreadDot;
        TextView tvSenderName, tvBadge, tvRoomName, tvPhone, tvNote, tvTime;
        ImageButton btnDelete;
        MaterialButton btnMarkRead;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
        }
    }
}
