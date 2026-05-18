package com.example.rentalmanager.ui.tenant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.entity.TenantEntity;
import com.example.rentalmanager.util.ResultState;
import com.example.rentalmanager.util.SearchDebouncer;
import com.example.rentalmanager.viewmodel.TenantViewModel;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
public class TenantFragment extends Fragment {

    private TenantViewModel viewModel;
    private TenantAdapter adapter;
    private List<TenantEntity> allTenants = new ArrayList<>();
    private final Map<Integer, String> tenantSearchIndex = new HashMap<>();
    private final SearchDebouncer searchDebouncer = new SearchDebouncer();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tenant, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerTenants);
        View fab = view.findViewById(R.id.fabAddTenant);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TenantAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(TenantViewModel.class);

        android.widget.EditText etSearch = view.findViewById(R.id.etSearch);
        LinearLayout tvEmpty = view.findViewById(R.id.tvEmpty);

        // Observe data
        viewModel.getAllTenants().observe(getViewLifecycleOwner(), tenants -> {
            this.allTenants = tenants != null ? tenants : new ArrayList<>();
            rebuildTenantSearchIndex();
            filterTenants(etSearch.getText().toString(), tvEmpty);
        });

        // Search logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                searchDebouncer.submit(() -> {
                    if (isAdded()) {
                        filterTenants(query, tvEmpty);
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // FAB Add
        fab.setOnClickListener(v -> {
            new TenantFormDialog()
                    .show(getParentFragmentManager(), "TenantFormDialog");
        });
        // CALLBACK từ Adapter
        adapter.setOnTenantClickListener(new TenantAdapter.OnTenantClickListener() {

            @Override
            public void onEdit(TenantEntity tenant) {
                new TenantFormDialog(tenant)
                        .show(getParentFragmentManager(), "EditTenant");
            }

            @Override
            public void onDelete(TenantEntity tenant) {

                new AlertDialog.Builder(requireContext())
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có chắc muốn xóa người thuê này?")
                        .setPositiveButton("Xóa", (dialog, which) -> {

                            viewModel.delete(tenant)
                                    .observe(getViewLifecycleOwner(), result -> {

                                        if (result.success) {
                                            Toast.makeText(requireContext(),
                                                    result.message,
                                                    Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(requireContext(),
                                                    result.message,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        });

        return view;
    }

    private void filterTenants(String query, View tvEmpty) {
        List<TenantEntity> filtered = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            filtered.addAll(allTenants);
        } else {
            String q = com.example.rentalmanager.util.TextUtil.removeAccents(query.trim().toLowerCase(Locale.ROOT));
            for (TenantEntity t : allTenants) {
                String indexed = tenantSearchIndex.get(t.tenantId);
                if (indexed != null && indexed.contains(q)) {
                    filtered.add(t);
                }
            }
        }
        
        adapter.setData(filtered);
        
        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void rebuildTenantSearchIndex() {
        tenantSearchIndex.clear();
        if (allTenants == null) {
            return;
        }

        for (TenantEntity t : allTenants) {
            String name = t.fullName != null
                    ? com.example.rentalmanager.util.TextUtil.removeAccents(t.fullName.toLowerCase(Locale.ROOT))
                    : "";
            String phone = t.phone != null ? t.phone : "";
            String cccd = t.cccd != null ? t.cccd : "";
            tenantSearchIndex.put(t.tenantId, name + "|" + phone + "|" + cccd);
        }
    }

    @Override
    public void onDestroyView() {
        searchDebouncer.cancel();
        super.onDestroyView();
    }
}
