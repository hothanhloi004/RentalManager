package com.example.rentalmanager.ui.contract;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.model.ContractWithInfo;
import com.example.rentalmanager.util.ContractStatus;
import com.example.rentalmanager.util.RoomStatus;
import com.example.rentalmanager.util.SearchDebouncer;
import com.example.rentalmanager.util.TextUtil;
import com.example.rentalmanager.viewmodel.ContractViewModel;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContractListFragment extends Fragment {

    private ContractViewModel viewModel;
    private ContractAdapter adapter;
    private List<ContractWithInfo> allContracts = new ArrayList<>();
    private final Map<Integer, String> contractSearchIndex = new HashMap<>();
    private final SearchDebouncer searchDebouncer = new SearchDebouncer();
    private EditText etSearch;
    private ChipGroup chipGroup;
    private static final long EXPIRING_WINDOW_MILLIS = 30L * 24 * 60 * 60 * 1000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contract_list, container, false);

        view.findViewById(R.id.btnAddContract).setOnClickListener(v ->
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ContractFragment())
                        .addToBackStack(null)
                        .commit()
        );

        RecyclerView recyclerView = view.findViewById(R.id.recyclerContracts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ContractAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ContractViewModel.class);

        adapter.setOnViewClickListener(contract ->
                ContractDetailDialog.show(requireContext(), contract)
        );

        adapter.setOnEndClickListener(contractId ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("X\u00e1c nh\u1eadn k\u1ebft th\u00fac h\u1ee3p \u0111\u1ed3ng")
                        .setMessage("B\u1ea1n c\u00f3 ch\u1eafc mu\u1ed1n k\u1ebft th\u00fac h\u1ee3p \u0111\u1ed3ng n\u00e0y kh\u00f4ng?")
                        .setPositiveButton("K\u1ebft th\u00fac", (dialog, which) -> showRoomStatusSuggestionDialog(contractId))
                        .setNegativeButton("H\u1ee7y", null)
                        .show()
        );

        adapter.setOnEditServiceClickListener(contract -> {
            if (getContext() == null) {
                return;
            }

            String[] items = {"D\u00f9ng WiFi", "Thu ti\u1ec1n r\u00e1c", "Ph\u00ed d\u1ecbch v\u1ee5 kh\u00e1c"};
            boolean[] checkedItems = {contract.useWifi, contract.useTrash, contract.useServiceFee};

            new AlertDialog.Builder(requireContext())
                    .setTitle("D\u1ecbch v\u1ee5 ph\u00f2ng " + contract.roomName + "\n(Hi\u1ec7u l\u1ef1c t\u1eeb th\u00e1ng t\u1edbi)")
                    .setMultiChoiceItems(items, checkedItems,
                            (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                    .setPositiveButton("L\u01b0u thay \u0111\u1ed5i", (dialog, which) ->
                            viewModel.updateServiceFlags(
                                    contract.contractId,
                                    checkedItems[0],
                                    checkedItems[1],
                                    checkedItems[2]
                            ).observe(getViewLifecycleOwner(), result ->
                                    Toast.makeText(
                                            requireContext(),
                                            result.success ? "\u0110\u00e3 c\u1eadp nh\u1eadt d\u1ecbch v\u1ee5. Hi\u1ec7u l\u1ef1c t\u1eeb th\u00e1ng t\u1edbi." : result.message,
                                            Toast.LENGTH_SHORT
                                    ).show()
                            )
                    )
                    .setNegativeButton("H\u1ee7y", null)
                    .show();
        });

        viewModel.getAllContracts().observe(getViewLifecycleOwner(), contracts -> {
            allContracts = contracts != null ? contracts : new ArrayList<>();
            rebuildContractSearchIndex();
            applyFilters();
        });

        etSearch = view.findViewById(R.id.etSearchContract);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchDebouncer.submit(() -> {
                    if (isAdded()) {
                        applyFilters();
                    }
                });
            }
        });

        chipGroup = view.findViewById(R.id.chipGroupContractFilter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilters());

        return view;
    }

    private void applyFilters() {
        if (etSearch == null || chipGroup == null) {
            return;
        }

        String query = etSearch.getText() != null
                ? TextUtil.removeAccents(etSearch.getText().toString().trim().toLowerCase(Locale.ROOT))
                : "";
        int selectedChipId = chipGroup.getCheckedChipId();

        List<ContractWithInfo> filtered = new ArrayList<>();
        for (ContractWithInfo c : allContracts) {
            String indexed = contractSearchIndex.get(c.contractId);
            boolean matchSearch = indexed == null || indexed.contains(query);

            boolean matchStatus = true;
            if (selectedChipId == R.id.chipContractActive) {
                matchStatus = ContractStatus.HIEU_LUC.equals(c.status);
            } else if (selectedChipId == R.id.chipContractEnded) {
                matchStatus = "KET_THUC".equals(c.status);
            } else if (selectedChipId == R.id.chipContractExpiring) {
                matchStatus = isExpiringSoon(c);
            }

            if (matchSearch && matchStatus) {
                filtered.add(c);
            }
        }

        if (selectedChipId == R.id.chipContractExpiring) {
            filtered.sort((c1, c2) -> Long.compare(
                    c1.endDate != null ? c1.endDate : Long.MAX_VALUE,
                    c2.endDate != null ? c2.endDate : Long.MAX_VALUE
            ));
        }
        adapter.setData(filtered);
    }

    private void rebuildContractSearchIndex() {
        contractSearchIndex.clear();
        for (ContractWithInfo c : allContracts) {
            String roomName = c.roomName == null ? "" : TextUtil.removeAccents(c.roomName.toLowerCase(Locale.ROOT));
            String tenantName = c.tenantName == null ? "" : TextUtil.removeAccents(c.tenantName.toLowerCase(Locale.ROOT));
            contractSearchIndex.put(c.contractId, roomName + "|" + tenantName);
        }
    }

    private void showRoomStatusSuggestionDialog(int contractId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật trạng thái phòng")
                .setMessage("Khách đã dời đi. Nhắc chuyển phòng sang trạng thái nào?")
                .setPositiveButton("Bảo trì", (dialog, which) -> endContractWithStatus(contractId, RoomStatus.BAO_TRI))
                .setNegativeButton("Báo trống", (dialog, which) -> endContractWithStatus(contractId, RoomStatus.TRONG))
                .show();
    }

    private void endContractWithStatus(int contractId, String nextRoomStatus) {
        viewModel.endContract(contractId, nextRoomStatus)
                .observe(getViewLifecycleOwner(), result ->
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                );
    }

    private boolean isExpiringSoon(ContractWithInfo contract) {
        if (!ContractStatus.HIEU_LUC.equals(contract.status) || contract.endDate == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long deadline = now + EXPIRING_WINDOW_MILLIS;
        return contract.endDate >= now && contract.endDate <= deadline;
    }

    @Override
    public void onDestroyView() {
        searchDebouncer.cancel();
        etSearch = null;
        chipGroup = null;
        super.onDestroyView();
    }
}
