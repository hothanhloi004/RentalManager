package com.example.rentalmanager.ui.contract;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.entity.RoomEntity;
import com.example.rentalmanager.data.entity.TenantEntity;
import com.example.rentalmanager.util.CurrencyInputHelper;
import com.example.rentalmanager.viewmodel.ContractViewModel;
import com.example.rentalmanager.viewmodel.RoomViewModel;
import com.example.rentalmanager.viewmodel.TenantViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ContractFragment extends Fragment {

    public static final String ARG_PRESELECT_ROOM_ID = "arg_preselect_room_id";

    private Spinner spRoom, spTenant;
    private EditText etDeposit;
    private MaterialButton btnCreate, btnPickEndDate;
    private TextView tvEndDate;
    private Long selectedEndDate = null;

    private ContractViewModel contractVM;
    private RoomViewModel roomVM;
    private TenantViewModel tenantVM;

    private List<RoomEntity> roomList = new ArrayList<>();
    private List<TenantEntity> tenantList = new ArrayList<>();
    private int preselectedRoomId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contract, container, false);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> navigateBackToContractList());

        spRoom = view.findViewById(R.id.spRoom);
        spTenant = view.findViewById(R.id.spTenant);
        etDeposit = view.findViewById(R.id.etDeposit);
        btnCreate = view.findViewById(R.id.btnCreate);
        btnPickEndDate = view.findViewById(R.id.btnPickEndDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);

        MaterialCheckBox cbWifi = view.findViewById(R.id.cbWifi);
        MaterialCheckBox cbTrash = view.findViewById(R.id.cbTrash);
        MaterialCheckBox cbServiceFee = view.findViewById(R.id.cbServiceFee);

        // Mặc định bật sẵn dịch vụ cho hợp đồng mới để thao tác nhanh hơn.
        cbWifi.setChecked(true);
        cbTrash.setChecked(true);
        cbServiceFee.setChecked(true);

        CurrencyInputHelper.attach(etDeposit);

        contractVM = new ViewModelProvider(this).get(ContractViewModel.class);
        roomVM = new ViewModelProvider(this).get(RoomViewModel.class);
        tenantVM = new ViewModelProvider(this).get(TenantViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            preselectedRoomId = args.getInt(ARG_PRESELECT_ROOM_ID, -1);
        }

        loadRooms();
        loadTenants();

        btnPickEndDate.setOnClickListener(v -> showDatePicker());
        btnCreate.setOnClickListener(v ->
                createContract(cbWifi.isChecked(), cbTrash.isChecked(), cbServiceFee.isChecked()));

        return view;
    }

    private void loadRooms() {
        roomVM.getAvailableRooms().observe(getViewLifecycleOwner(), rooms -> {
            roomList = rooms;
            List<String> names = new ArrayList<>();
            for (RoomEntity r : rooms) {
                names.add(r.roomName);
            }
            spRoom.setAdapter(new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names));
            applyPreselectedRoom();
        });
    }

    private void applyPreselectedRoom() {
        if (preselectedRoomId == -1 || roomList == null || roomList.isEmpty()) {
            return;
        }

        for (int i = 0; i < roomList.size(); i++) {
            if (roomList.get(i).roomId == preselectedRoomId) {
                spRoom.setSelection(i);
                preselectedRoomId = -1;
                return;
            }
        }

        Toast.makeText(
                requireContext(),
                "Phòng này hiện không còn ở danh sách phòng trống để tạo hợp đồng.",
                Toast.LENGTH_SHORT
        ).show();
        preselectedRoomId = -1;
    }

    private void loadTenants() {
        tenantVM.getAvailableTenants().observe(getViewLifecycleOwner(), tenants -> {
            tenantList = tenants;
            List<String> names = new ArrayList<>();
            for (TenantEntity t : tenants) {
                names.add(t.fullName);
            }
            spTenant.setAdapter(new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    names));
        });
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 23, 59, 59);
            selectedEndDate = selected.getTimeInMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvEndDate.setText("K\u1ebft th\u00fac: " + sdf.format(selected.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void createContract(boolean useWifi, boolean useTrash, boolean useServiceFee) {
        if (roomList.isEmpty() || tenantList.isEmpty()) {
            Toast.makeText(getContext(), "Kh\u00f4ng c\u00f3 d\u1eef li\u1ec7u h\u1ee3p l\u1ec7", Toast.LENGTH_SHORT).show();
            return;
        }

        int roomPos = spRoom.getSelectedItemPosition();
        int tenantPos = spTenant.getSelectedItemPosition();

        if (roomPos < 0 || tenantPos < 0) {
            Toast.makeText(getContext(), "Vui l\u00f2ng ch\u1ecdn ph\u00f2ng v\u00e0 ng\u01b0\u1eddi thu\u00ea", Toast.LENGTH_SHORT).show();
            return;
        }

        String depositStr = etDeposit.getText().toString().replaceAll("[^\\d]", "");
        if (depositStr.isEmpty()) {
            etDeposit.setError("Vui l\u00f2ng nh\u1eadp ti\u1ec1n \u0111\u1eb7t c\u1ecdc");
            etDeposit.requestFocus();
            return;
        }

        double deposit;
        try {
            deposit = Double.parseDouble(depositStr);
        } catch (NumberFormatException e) {
            etDeposit.setError("S\u1ed1 ti\u1ec1n kh\u00f4ng h\u1ee3p l\u1ec7");
            return;
        }

        RoomEntity room = roomList.get(roomPos);
        TenantEntity tenant = tenantList.get(tenantPos);

        contractVM.createContract(
                room.roomId,
                tenant.tenantId,
                System.currentTimeMillis(),
                selectedEndDate,
                deposit,
                room.price,
                useWifi,
                useTrash,
                useServiceFee
        ).observe(getViewLifecycleOwner(), result -> {
            Toast.makeText(getContext(), result.message, Toast.LENGTH_SHORT).show();
            if (result.success) {
                navigateBackToContractList();
            }
        });
    }

    private void navigateBackToContractList() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
            return;
        }

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ContractListFragment())
                .commit();
    }
}
