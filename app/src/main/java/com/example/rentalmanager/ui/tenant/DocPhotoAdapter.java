package com.example.rentalmanager.ui.tenant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rentalmanager.R;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocPhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_PHOTO = 1;

    public interface Listener {
        void onDelete(File file);
        void onView(File file);
    }

    private final List<File> photos = new ArrayList<>();
    private final List<RowItem> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setPhotos(List<File> list) {
        photos.clear();
        if (list != null) {
            photos.addAll(list);
        }
        rebuildItems();
        notifyDataSetChanged();
    }

    public void addPhoto(File f) {
        if (f == null) {
            return;
        }
        photos.add(0, f);
        rebuildItems();
        notifyDataSetChanged();
    }

    public void removePhoto(File file) {
        if (file == null) {
            return;
        }
        for (int i = 0; i < photos.size(); i++) {
            if (file.equals(photos.get(i))) {
                photos.remove(i);
                break;
            }
        }
        rebuildItems();
        notifyDataSetChanged();
    }

    public int getSpanSize(int position, int spanCount) {
        if (position < 0 || position >= items.size()) {
            return 1;
        }
        return items.get(position).viewType == VIEW_TYPE_HEADER ? spanCount : 1;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_photo_section, parent, false);
            return new HeaderVH(view);
        }

        View view = inflater.inflate(R.layout.item_doc_photo, parent, false);
        return new PhotoVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RowItem item = items.get(position);

        if (holder instanceof HeaderVH) {
            HeaderVH headerVH = (HeaderVH) holder;
            headerVH.tvSectionTitle.setText(item.title);
            headerVH.tvSectionCount.setText(item.count + " ảnh");
            return;
        }

        PhotoVH photoVH = (PhotoVH) holder;
        File file = item.file;
        Glide.with(photoVH.imgDoc.getContext())
                .load(file)
                .into(photoVH.imgDoc);

        String label = getCategoryLabel(getCategoryKey(file));
        photoVH.tvPhotoTag.setText(label);
        photoVH.tvPhotoTag.setVisibility(View.VISIBLE);

        photoVH.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(file);
            }
        });

        photoVH.imgDoc.setOnClickListener(v -> {
            if (listener != null) {
                listener.onView(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void rebuildItems() {
        items.clear();

        Map<String, List<File>> grouped = new LinkedHashMap<>();
        grouped.put("WEB", new ArrayList<>());
        grouped.put("TRUOCTHUE", new ArrayList<>());
        grouped.put("DONGHO", new ArrayList<>());
        grouped.put("KHAC", new ArrayList<>());

        for (File photo : photos) {
            grouped.get(getCategoryKey(photo)).add(photo);
        }

        for (Map.Entry<String, List<File>> entry : grouped.entrySet()) {
            List<File> files = entry.getValue();
            if (files.isEmpty()) {
                continue;
            }

            items.add(RowItem.header(getCategoryLabel(entry.getKey()), files.size()));
            for (File file : files) {
                items.add(RowItem.photo(file));
            }
        }
    }

    private String getCategoryKey(File file) {
        String fileName = file == null ? null : file.getName();
        if (fileName == null) {
            return "KHAC";
        }
        if (fileName.startsWith("TAG_WEB_")) {
            return "WEB";
        }
        if (fileName.startsWith("TAG_TRUOCTHUE_")) {
            return "TRUOCTHUE";
        }
        if (fileName.startsWith("TAG_DONGHO_")) {
            return "DONGHO";
        }
        return "KHAC";
    }

    private String getCategoryLabel(String categoryKey) {
        if ("WEB".equals(categoryKey)) {
            return "Ảnh đăng web";
        }
        if ("TRUOCTHUE".equals(categoryKey)) {
            return "Ảnh trước thuê";
        }
        if ("DONGHO".equals(categoryKey)) {
            return "Ảnh đồng hồ";
        }
        return "Khác";
    }

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView tvSectionTitle;
        TextView tvSectionCount;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvSectionCount = itemView.findViewById(R.id.tvSectionCount);
        }
    }

    static class PhotoVH extends RecyclerView.ViewHolder {
        ImageView imgDoc;
        ImageButton btnDelete;
        TextView tvPhotoTag;

        PhotoVH(@NonNull View v) {
            super(v);
            imgDoc = v.findViewById(R.id.imgDoc);
            btnDelete = v.findViewById(R.id.btnDelete);
            tvPhotoTag = v.findViewById(R.id.tvPhotoTag);
        }
    }

    private static class RowItem {
        final int viewType;
        final String title;
        final int count;
        final File file;

        private RowItem(int viewType, String title, int count, File file) {
            this.viewType = viewType;
            this.title = title;
            this.count = count;
            this.file = file;
        }

        static RowItem header(String title, int count) {
            return new RowItem(VIEW_TYPE_HEADER, title, count, null);
        }

        static RowItem photo(File file) {
            return new RowItem(VIEW_TYPE_PHOTO, null, 0, file);
        }
    }
}
