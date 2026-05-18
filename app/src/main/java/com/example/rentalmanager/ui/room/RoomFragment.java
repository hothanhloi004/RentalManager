package com.example.rentalmanager.ui.room;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.model.RoomWithTenant;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.util.CurrencyInputHelper;
import com.example.rentalmanager.util.SearchDebouncer;
import com.example.rentalmanager.util.TextUtil;
import com.example.rentalmanager.viewmodel.RoomViewModel;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomFragment extends Fragment {

    private RoomViewModel viewModel;
    private RoomAdapter adapter;

    private TextView txtTotal;
    private TextView txtOccupancy;
    private TextView txtOccupied;
    private TextView txtEmpty;

    private Spinner spSort;
    private Spinner spStatusFilter;
    private Spinner spPriceFilter;
    private ChipGroup chipGroupFilter;
    private EditText etSearchRoom;

    private List<RoomWithTenant> currentRooms = new ArrayList<>();
    private final Map<Integer, String> roomSearchIndex = new HashMap<>();
    private final SearchDebouncer searchDebouncer = new SearchDebouncer(180L);
    private final Executor filterExecutor = AppExecutors.getInstance().networkIO();
    private final AtomicInteger filterGeneration = new AtomicInteger();

    private int totalRooms = 0;
    private int occupiedRooms = 0;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_room, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.roomRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        spPriceFilter = view.findViewById(R.id.spPriceFilter);
        txtTotal = view.findViewById(R.id.txtTotalRooms);
        txtOccupancy = view.findViewById(R.id.txtOccupancy);
        txtOccupied = view.findViewById(R.id.txtOccupied);
        txtEmpty = view.findViewById(R.id.txtEmpty);
        spSort = view.findViewById(R.id.spSort);
        spStatusFilter = view.findViewById(R.id.spStatusFilter);

        adapter = new RoomAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(RoomViewModel.class);

        setupSpinners();

        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilterAndSort());

        etSearchRoom = view.findViewById(R.id.etSearchRoom);
        etSearchRoom.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchDebouncer.submit(RoomFragment.this::applyFilterAndSort);
            }
        });

        viewModel.getRoomsWithTenant().observe(getViewLifecycleOwner(), rooms -> {
            currentRooms = rooms != null ? rooms : new ArrayList<>();
            rebuildRoomSearchIndex();
            applyFilterAndSort();
        });

        viewModel.getTotalRooms().observe(getViewLifecycleOwner(), total -> {
            totalRooms = total != null ? total : 0;
            updateDashboard();
        });

        viewModel.getOccupiedRooms().observe(getViewLifecycleOwner(), occupied -> {
            occupiedRooms = occupied != null ? occupied : 0;
            updateDashboard();
        });

        adapter.setOnRoomClickListener(this::showEditDialog);

        adapter.setOnRoomDeleteListener(room -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Xóa phòng")
                    .setMessage("Bạn có chắc muốn xóa \"" + room.roomName + "\" không?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        RoomEntity entity = new RoomEntity();
                        entity.roomId = room.roomId;
                        entity.roomName = room.roomName;
                        entity.price = room.price;
                        entity.status = room.status;
                        entity.note = room.note;
                        viewModel.delete(entity);
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        adapter.setOnRoomLongClickListener(room -> {
            String[] options;
            if ("DANG_THUE".equals(room.status)) {
                options = new String[]{"Sửa ghi chú"};
            } else {
                options = new String[]{"Chỉnh sửa phòng", "Xóa phòng"};
            }

            new AlertDialog.Builder(getContext())
                    .setTitle(room.roomName)
                    .setItems(options, (d, w) -> {
                        if ("DANG_THUE".equals(room.status)) {
                            showEditDialog(cloneRoom(room));
                        } else if (w == 0) {
                            showEditDialog(cloneRoom(room));
                        } else {
                            new AlertDialog.Builder(getContext())
                                    .setTitle("Xóa phòng")
                                    .setMessage("Bạn có chắc muốn xóa phòng này?")
                                    .setPositiveButton("Xóa", (d2, w2) -> viewModel.delete(room))
                                    .setNegativeButton("Hủy", null)
                                    .show();
                        }
                    })
                    .show();
        });

        View fab = view.findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddDialog());

        return view;
    }

    private RoomWithTenant cloneRoom(RoomWithTenant room) {
        RoomWithTenant copy = new RoomWithTenant();
        copy.roomId = room.roomId;
        copy.roomName = room.roomName;
        copy.price = room.price;
        copy.status = room.status;
        copy.note = room.note;
        return copy;
    }

    private RoomWithTenant cloneRoom(RoomEntity room) {
        RoomWithTenant copy = new RoomWithTenant();
        copy.roomId = room.roomId;
        copy.roomName = room.roomName;
        copy.price = room.price;
        copy.status = room.status;
        copy.note = room.note;
        return copy;
    }

    private void setupSpinners() {
        String[] statusOptions = {
                "Tất cả",
                "Chỉ phòng trống",
                "Chỉ đang thuê"
        };

        String[] sortOptions = {
                "Theo tên",
                "Giá tăng dần",
                "Giá giảm dần"
        };

        String[] priceOptions = {
                "Tất cả mức giá",
                "Dưới 3 triệu",
                "3 - 5 triệu",
                "5 - 10 triệu",
                "Trên 10 triệu"
        };

        spStatusFilter.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                statusOptions));

        spSort.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                sortOptions));

        spPriceFilter.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                priceOptions));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilterAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spStatusFilter.setOnItemSelectedListener(listener);
        spSort.setOnItemSelectedListener(listener);
        spPriceFilter.setOnItemSelectedListener(listener);
    }

    private void applyFilterAndSort() {
        if (etSearchRoom == null) return;

        final String query = TextUtil.removeAccents(etSearchRoom.getText().toString().trim());
        final List<RoomWithTenant> roomsSnapshot = new ArrayList<>(currentRooms);
        final int sortPos = spSort.getSelectedItemPosition();
        final int pricePos = spPriceFilter.getSelectedItemPosition();
        final List<Integer> checkedIds = chipGroupFilter.getCheckedChipIds();
        final int checkedId = checkedIds.isEmpty() ? R.id.chipAll : checkedIds.get(0);
        final int requestId = filterGeneration.incrementAndGet();

        filterExecutor.execute(() -> {
            List<RoomWithTenant> filtered = new ArrayList<>();

            for (RoomWithTenant room : roomsSnapshot) {
                if (!query.isEmpty()) {
                    String indexed = roomSearchIndex.get(room.roomId);
                    if (indexed == null || !indexed.contains(query)) continue;
                }

                if (checkedId == R.id.chipVacant && !"TRONG".equals(room.status)) continue;
                if (checkedId == R.id.chipOccupied && !"DANG_THUE".equals(room.status)) continue;
                if (checkedId == R.id.chipMaintenance && !"BAO_TRI".equals(room.status)) continue;

                double price = room.price;
                if (pricePos == 1 && price >= 3_000_000) continue;
                if (pricePos == 2 && (price < 3_000_000 || price > 5_000_000)) continue;
                if (pricePos == 3 && (price < 5_000_000 || price > 10_000_000)) continue;
                if (pricePos == 4 && price <= 10_000_000) continue;

                filtered.add(room);
            }

            if (sortPos == 1) {
                filtered.sort((a, b) -> Double.compare(a.price, b.price));
            } else if (sortPos == 2) {
                filtered.sort((a, b) -> Double.compare(b.price, a.price));
            } else {
                filtered.sort((a, b) -> a.roomName.compareToIgnoreCase(b.roomName));
            }

            if (!isAdded() || requestId != filterGeneration.get()) return;

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || requestId != filterGeneration.get()) return;
                adapter.setRooms(filtered);
            });
        });
    }

    private void rebuildRoomSearchIndex() {
        roomSearchIndex.clear();
        for (RoomWithTenant room : currentRooms) {
            String roomName = TextUtil.removeAccents(room.roomName == null ? "" : room.roomName);
            String tenantName = TextUtil.removeAccents(room.tenantName == null ? "" : room.tenantName);
            roomSearchIndex.put(room.roomId, roomName + "|" + tenantName);
        }
    }

    private void updateDashboard() {
        txtTotal.setText("Tổng phòng: " + totalRooms);
        txtOccupied.setText("Đang thuê: " + occupiedRooms);
        txtEmpty.setText("Còn trống: " + (totalRooms - occupiedRooms));

        if (totalRooms == 0) {
            txtOccupancy.setText("Tỷ lệ lấp đầy: 0%");
            txtOccupancy.setTextColor(Color.BLACK);
            return;
        }

        int percent = (int) ((occupiedRooms * 100.0) / totalRooms);
        txtOccupancy.setText("Tỷ lệ lấp đầy: " + percent + "%");

        if (percent < 50) {
            txtOccupancy.setTextColor(Color.parseColor("#2E7D32"));
        } else if (percent < 80) {
            txtOccupancy.setTextColor(Color.parseColor("#F57C00"));
        } else {
            txtOccupancy.setTextColor(Color.parseColor("#C62828"));
        }
    }

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_room, null);

        EditText edtName = dialogView.findViewById(R.id.edtRoomName);
        EditText edtPrice = dialogView.findViewById(R.id.edtRoomPrice);
        EditText edtNote = dialogView.findViewById(R.id.edtRoomNote);

        attachPriceFormatter(edtPrice);

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String priceStr = edtPrice.getText().toString().replaceAll("[^\\d]", "");
                    String note = edtNote.getText().toString().trim();

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Vui lòng nhập đầy đủ thông tin",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = Double.parseDouble(priceStr);
                    viewModel.addRoom(name, price, note);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditDialog(RoomWithTenant room) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_room, null);

        EditText edtName = dialogView.findViewById(R.id.edtRoomName);
        EditText edtPrice = dialogView.findViewById(R.id.edtRoomPrice);
        EditText edtNote = dialogView.findViewById(R.id.edtRoomNote);
        com.google.android.material.switchmaterial.SwitchMaterial switchMaintenance =
                dialogView.findViewById(R.id.switchMaintenance);

        attachPriceFormatter(edtPrice);

        edtName.setText(room.roomName);
        if (room.price > 0) {
            edtPrice.setText(java.text.NumberFormat.getInstance(new Locale("vi", "VN")).format(room.price));
        } else {
            edtPrice.setText("0");
        }
        edtNote.setText(room.note);

        if ("DANG_THUE".equals(room.status)) {
            edtName.setEnabled(false);
            edtPrice.setEnabled(false);
        } else {
            switchMaintenance.setVisibility(View.VISIBLE);
            switchMaintenance.setChecked("BAO_TRI".equals(room.status));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) {
            tvTitle.setText("DANG_THUE".equals(room.status) ? "Sửa ghi chú phòng" : "Chỉnh sửa phòng");
        }

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Cập nhật", (dialog, which) -> {
                    String name = edtName.getText().toString().trim();
                    String priceStr = edtPrice.getText().toString().replaceAll("[^\\d]", "");
                    String note = edtNote.getText().toString().trim();

                    if (name.isEmpty() || priceStr.isEmpty()) {
                        Toast.makeText(requireContext(),
                                "Vui lòng nhập đầy đủ thông tin",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double price = Double.parseDouble(priceStr);

                    RoomEntity updated = new RoomEntity();
                    updated.roomId = room.roomId;
                    updated.roomName = name;
                    updated.price = price;
                    updated.note = note;
                    updated.status = "DANG_THUE".equals(room.status)
                            ? "DANG_THUE"
                            : (switchMaintenance.isChecked() ? "BAO_TRI" : "TRONG");

                    viewModel.updateRoom(updated, name, price, note);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void attachPriceFormatter(EditText edtPrice) {
        CurrencyInputHelper.attach(edtPrice);
    }

    @Override
    public void onDestroyView() {
        searchDebouncer.cancel();
        super.onDestroyView();
    }
}
