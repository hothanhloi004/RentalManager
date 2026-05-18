package com.example.rentalmanager.ui.bill;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.data.entity.BillEntity;
import com.example.rentalmanager.data.model.ActiveContractInfo;
import com.example.rentalmanager.data.model.BillWithInfo;
import com.example.rentalmanager.util.AppExecutors;
import com.example.rentalmanager.util.CurrencyInputHelper;
import com.example.rentalmanager.util.SearchDebouncer;
import com.example.rentalmanager.viewmodel.BillViewModel;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public class BillFragment extends Fragment {

    private BillViewModel viewModel;
    private List<BillWithInfo> allBills = new ArrayList<>();
    private final Map<Integer, String> billSearchIndex = new HashMap<>();
    private String currentSearch = "";
    private int currentStatusFilter = 0; // 0=all,1=unpaid,2=paid,3=overdue
    private final SearchDebouncer searchDebouncer = new SearchDebouncer();

    private Spinner spContract;
    private List<ActiveContractInfo> contractList = new ArrayList<>();

    private EditText edtOldElectric, edtNewElectric;
    private com.google.android.material.textfield.TextInputEditText edtOldWater, edtNewWater;

    private TextView txtDueDate;
    private Button btnCreate;
    private com.google.android.material.button.MaterialButton btnAutofillLastMeter;

    private View cardCreateForm;
    private com.google.android.material.button.MaterialButton btnToggleCreateForm;
    private androidx.core.widget.NestedScrollView billScrollView;

    private RecyclerView recyclerView;
    private BillAdapter billAdapter;

    private long selectedDueDate = 0;
    private final Executor ioExecutor = AppExecutors.getInstance().diskIO();
    private final Executor filterExecutor = AppExecutors.getInstance().networkIO();
    private final AtomicInteger filterGeneration = new AtomicInteger();
    private int lastAutofilledContractId = -1;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bill, container, false);

        viewModel = new ViewModelProvider(this).get(BillViewModel.class);

        spContract = view.findViewById(R.id.spContract);

        edtOldElectric = view.findViewById(R.id.edtOldElectric);
        edtNewElectric = view.findViewById(R.id.edtNewElectric);
        edtOldWater = view.findViewById(R.id.edtOldWater);
        edtNewWater = view.findViewById(R.id.edtNewWater);
        txtDueDate = view.findViewById(R.id.txtDueDate);
        btnCreate = view.findViewById(R.id.btnCreateBill);
        btnAutofillLastMeter = view.findViewById(R.id.btnAutofillLastMeter);

        view.findViewById(R.id.btnPickDueDate)
                .setOnClickListener(v -> showDatePicker());

        btnCreate.setOnClickListener(v -> createBill());
        btnAutofillLastMeter.setOnClickListener(v -> autofillLastMeter(true));
        Button btnGenerate = view.findViewById(R.id.btnGenerateBills);

        btnGenerate.setOnClickListener(v -> showMonthPickerDialog());

        android.widget.EditText etSearch = view.findViewById(R.id.etSearchBill);
        ChipGroup chipGroupFilter = view.findViewById(R.id.chipGroupBillFilter);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(android.text.Editable e) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                currentSearch = s.toString();
                searchDebouncer.submit(this::applySearchFilterSafe);
            }

            private void applySearchFilterSafe() {
                if (isAdded()) {
                    applyBillFilter();
                }
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chipBillAll) currentStatusFilter = 0;
            else if (id == R.id.chipBillUnpaid) currentStatusFilter = 1;
            else if (id == R.id.chipBillPaid) currentStatusFilter = 2;
            else if (id == R.id.chipBillOverdue) currentStatusFilter = 3;
            else if (id == R.id.chipBillUnclosed) currentStatusFilter = 4;
            applyBillFilter();
        });

        cardCreateForm = view.findViewById(R.id.cardCreateForm);
        btnToggleCreateForm = view.findViewById(R.id.btnToggleCreateForm);
        billScrollView = view.findViewById(R.id.billScrollView);

        btnToggleCreateForm.setOnClickListener(v -> {
            boolean isVisible = cardCreateForm.getVisibility() == View.VISIBLE;
            cardCreateForm.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            if (!isVisible) {
                cardCreateForm.post(() -> {
                    cardCreateForm.requestFocus();
                    cardCreateForm.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
                });
            }
            btnToggleCreateForm.setText(isVisible ? "T\u1ea1o h\u00f3a \u0111\u01a1n / Ch\u1ed1t \u0111i\u1ec7n n\u01b0\u1edbc" : "\u0110\u00f3ng b\u1ea3ng t\u1ea1o h\u00f3a \u0111\u01a1n");
        });

        recyclerView = view.findViewById(R.id.billRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        if (recyclerView.getItemAnimator() instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        }

        billAdapter = new BillAdapter();
        recyclerView.setAdapter(billAdapter);

        viewModel.getBillsWithInfo().observe(getViewLifecycleOwner(), bills -> {
            allBills = bills != null ? bills : new ArrayList<>();
            rebuildBillSearchIndex();
            applyBillFilter();
        });

        observeContracts();
        observeResult();
        billAdapter.setListener(new BillAdapter.BillListener() {
            @Override
            public void onPay(BillWithInfo bill) {
                if (!bill.meterUpdated) {
                    showEditBillDialog(bill);
                } else {
                    showPaymentDialog(bill);
                }
            }

            @Override
            public void onViewDetail(BillWithInfo bill) {
                showBillDetailDialog(bill);
            }
        });

        return view;
    }

    private void observeContracts() {
        viewModel.getActiveContracts().observe(
                getViewLifecycleOwner(),
                contracts -> {
                    if (contracts == null || contracts.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.bill_no_active_contract), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    contractList = contracts;

                    List<String> displayList = new ArrayList<>();
                    for (ActiveContractInfo c : contracts) {
                        displayList.add(c.roomName + " - " + c.tenantName);
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(requireContext(),
                                    android.R.layout.simple_spinner_dropdown_item,
                                    displayList);

                    spContract.setAdapter(adapter);
                    spContract.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position < 0 || position >= contractList.size()) {
                                return;
                            }

                            int selectedContractId = contractList.get(position).contractId;
                            if (selectedContractId != lastAutofilledContractId) {
                                autofillLastMeter(false);
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                    if (!contracts.isEmpty()) {
                        spContract.post(() -> autofillLastMeter(false));
                    }
                });
    }

    private void observeResult() {
        viewModel.getOperationResult().observe(
                getViewLifecycleOwner(),
                success -> {
                    if (success != null && success) {
                        clearForm();
                    }
                });

        viewModel.getMessage().observe(
                getViewLifecycleOwner(),
                msg -> {
                    if (msg != null) {
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createBill() {
        if (contractList.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.bill_no_contract_to_create), Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDueDate);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String month = sdf.format(cal.getTime());
        String oldE = edtOldElectric.getText().toString().trim();
        String newE = edtNewElectric.getText().toString().trim();
        String oldW = edtOldWater.getText().toString().trim();
        String newW = edtNewWater.getText().toString().trim();

        if (TextUtils.isEmpty(oldE) || TextUtils.isEmpty(newE)
                || TextUtils.isEmpty(oldW) || TextUtils.isEmpty(newW) || selectedDueDate == 0) {
            Toast.makeText(getContext(), getString(R.string.bill_fill_all_info), Toast.LENGTH_SHORT).show();
            return;
        }

        int oE = Integer.parseInt(oldE), nE = Integer.parseInt(newE);
        int oW = Integer.parseInt(oldW), nW = Integer.parseInt(newW);

        edtNewElectric.setError(null);
        edtNewWater.setError(null);

        if (nE < oE) {
            edtNewElectric.setError("Chỉ số điện mới phải lớn hơn hoặc bằng số cũ");
            edtNewElectric.requestFocus();
            return;
        }

        if (nW < oW) {
            edtNewWater.setError("Chỉ số nước mới phải lớn hơn hoặc bằng số cũ");
            edtNewWater.requestFocus();
            return;
        }

        ActiveContractInfo selected = contractList.get(spContract.getSelectedItemPosition());

        ioExecutor.execute(() -> {
            com.example.rentalmanager.data.entity.SettingEntity setting =
                    AppDatabase.getInstance(requireContext()).settingDao().getSetting();
            double ePrice = (setting != null && setting.electricPrice > 0) ? setting.electricPrice : 3500;
            double wPrice = (setting != null && setting.waterPrice > 0) ? setting.waterPrice : 20000;
            double svcFee = 0;
            if (setting != null) {
                if (selected.useTrash) svcFee += setting.trashFee;
                if (selected.useWifi) svcFee += setting.wifiPrice;
                if (selected.useServiceFee) svcFee += setting.serviceFee;
            }

            BillEntity bill = new BillEntity();
            bill.contractId = selected.contractId;
            bill.month = month;
            bill.oldElectric = oE;
            bill.newElectric = nE;
            bill.oldWater = oW;
            bill.newWater = nW;
            bill.electricPrice = ePrice;
            bill.waterPrice = wPrice;
            bill.serviceFee = svcFee;
            bill.rentPrice = selected.rentPrice;
            bill.totalAmount = selected.rentPrice + ((nE - oE) * ePrice) + ((nW - oW) * wPrice) + svcFee;
            bill.dueDate = selectedDueDate;
            bill.paymentStatus = "CHUA_THANH_TOAN";
            bill.paidAt = null;
            viewModel.createBill(bill);
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog =
                new DatePickerDialog(requireContext(),
                        (view, year, month, day) -> {
                            Calendar selected = Calendar.getInstance();
                            selected.set(year, month, day);

                            selectedDueDate = selected.getTimeInMillis();

                            SimpleDateFormat sdf =
                                    new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                            txtDueDate.setText("H\u1ea1n: " + sdf.format(selected.getTime()));
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private void clearForm() {
        edtOldElectric.setText("");
        edtNewElectric.setText("");
        edtOldWater.setText("");
        edtNewWater.setText("");
        edtNewElectric.setError(null);
        edtNewWater.setError(null);
        txtDueDate.setText("H\u1ea1n: ch\u01b0a ch\u1ecdn");
        selectedDueDate = 0;
    }

    private void autofillLastMeter(boolean showToast) {
        if (contractList.isEmpty() || spContract.getSelectedItemPosition() < 0) {
            if (showToast) {
                Toast.makeText(getContext(), "Chưa có hợp đồng để lấy chỉ số", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        ActiveContractInfo selected = contractList.get(spContract.getSelectedItemPosition());
        lastAutofilledContractId = selected.contractId;
        btnAutofillLastMeter.setEnabled(false);

        viewModel.getLastBill(selected.contractId, lastBill -> {
            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }

                int oldElectric = lastBill != null ? lastBill.newElectric : 0;
                int oldWater = lastBill != null ? lastBill.newWater : 0;
                int previousElectric = currentIntOrZero(edtOldElectric);
                int previousWater = currentIntOrZero(edtOldWater);

                edtOldElectric.setText(String.valueOf(oldElectric));
                edtOldWater.setText(String.valueOf(oldWater));

                String currentNewElectric = edtNewElectric.getText() != null
                        ? edtNewElectric.getText().toString().trim()
                        : "";
                String currentNewWater = edtNewWater.getText() != null
                        ? edtNewWater.getText().toString().trim()
                        : "";

                if (currentNewElectric.isEmpty() || currentNewElectric.equals(String.valueOf(previousElectric))) {
                    edtNewElectric.setText(String.valueOf(oldElectric));
                }
                if (currentNewWater.isEmpty() || currentNewWater.equals(String.valueOf(previousWater))) {
                    edtNewWater.setText(String.valueOf(oldWater));
                }

                btnAutofillLastMeter.setEnabled(true);
                if (showToast) {
                    Toast.makeText(
                            getContext(),
                            lastBill != null ? "Đã lấy số điện nước tháng trước" : "Chưa có hóa đơn cũ, điền mặc định 0",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });
    }

    private int currentIntOrZero(TextView textView) {
        if (textView == null || textView.getText() == null) {
            return 0;
        }

        String value = textView.getText().toString().trim();
        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void applyBillFilter() {
        final List<BillWithInfo> billsSnapshot = new ArrayList<>(allBills);
        final String q = com.example.rentalmanager.util.TextUtil.removeAccents(currentSearch.trim());
        final int statusFilter = currentStatusFilter;
        final int requestId = filterGeneration.incrementAndGet();

        filterExecutor.execute(() -> {
            List<BillWithInfo> filtered = new ArrayList<>();
            for (BillWithInfo b : billsSnapshot) {
                boolean isThieu = "DONG_THIEU".equals(b.paymentStatus) && b.totalPaid > 0;
                boolean isChuaTra = "CHUA_THANH_TOAN".equals(b.paymentStatus)
                        || ("DONG_THIEU".equals(b.paymentStatus) && b.totalPaid <= 0);

                if (statusFilter == 1) {
                    if (!isChuaTra || !b.meterUpdated) continue;
                }
                if (statusFilter == 2 && !"DA_THANH_TOAN".equals(b.paymentStatus)) continue;
                if (statusFilter == 3 && !isThieu) continue;
                if (statusFilter == 4 && b.meterUpdated) continue;

                if (!q.isEmpty()) {
                    String indexed = billSearchIndex.get(b.billId);
                    if (indexed == null || !indexed.contains(q)) continue;
                }
                filtered.add(b);
            }

            java.util.Collections.sort(filtered, (b1, b2) -> {
                if (statusFilter == 2) {
                    long p1 = b1.paidAt != null ? b1.paidAt : 0;
                    long p2 = b2.paidAt != null ? b2.paidAt : 0;
                    if (p1 != p2) return Long.compare(p2, p1);
                }

                if (statusFilter == 0) {
                    boolean b1Paid = "DA_THANH_TOAN".equals(b1.paymentStatus);
                    boolean b2Paid = "DA_THANH_TOAN".equals(b2.paymentStatus);

                    if (b1Paid != b2Paid) {
                        return b1Paid ? 1 : -1;
                    }

                    if (b1Paid) {
                        long p1 = b1.paidAt != null ? b1.paidAt : 0;
                        long p2 = b2.paidAt != null ? b2.paidAt : 0;
                        if (p1 != p2) return Long.compare(p2, p1);
                    } else if (b1.meterUpdated != b2.meterUpdated) {
                        return b1.meterUpdated ? 1 : -1;
                    }
                }

                if (statusFilter == 1 && b1.meterUpdated != b2.meterUpdated) {
                    return b1.meterUpdated ? 1 : -1;
                }

                return Long.compare(b2.dueDate, b1.dueDate);
            });

            if (!isAdded() || requestId != filterGeneration.get()) return;

            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || requestId != filterGeneration.get()) return;
                billAdapter.setData(filtered);
            });
        });
    }

    private void rebuildBillSearchIndex() {
        billSearchIndex.clear();
        for (BillWithInfo b : allBills) {
            String room = b.roomName != null
                    ? com.example.rentalmanager.util.TextUtil.removeAccents(b.roomName.toLowerCase(Locale.ROOT))
                    : "";
            String tenant = b.tenantName != null
                    ? com.example.rentalmanager.util.TextUtil.removeAccents(b.tenantName.toLowerCase(Locale.ROOT))
                    : "";
            String month = b.month != null
                    ? com.example.rentalmanager.util.TextUtil.removeAccents(b.month.toLowerCase(Locale.ROOT))
                    : "";
            billSearchIndex.put(b.billId, room + "|" + tenant + "|" + month);
        }
    }

    @Override
    public void onDestroyView() {
        searchDebouncer.cancel();
        super.onDestroyView();
    }

    private void showEditBillDialog(BillWithInfo bill) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_bill, null);

        TextView txtOldE = view.findViewById(R.id.txtOldElectric);
        TextView txtOldW = view.findViewById(R.id.txtOldWater);
        EditText edtNewE = view.findViewById(R.id.edtNewElectric);
        EditText edtNewW = view.findViewById(R.id.edtNewWater);

        viewModel.getLastBill(bill.contractId, lastBill -> {
            int oldE = (lastBill != null) ? lastBill.newElectric : 0;
            int oldW = (lastBill != null) ? lastBill.newWater : 0;
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                txtOldE.setText(getString(R.string.bill_old_electric_prefix) + oldE);
                txtOldW.setText(getString(R.string.bill_old_water_prefix) + oldW);
                com.google.android.material.textfield.TextInputLayout layNewE = view.findViewById(R.id.layNewElectric);
                com.google.android.material.textfield.TextInputLayout layNewW = view.findViewById(R.id.layNewWater);
                TextView txtPreviewTotal = view.findViewById(R.id.txtPreviewTotal);
                java.text.NumberFormat fmt = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));

                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.bill_input_meter_title)
                        .setView(view)
                        .setPositiveButton(R.string.bill_update, (d, w) -> {
                            String sNewE = edtNewE.getText().toString().trim();
                            String sNewW = edtNewW.getText().toString().trim();
                            if (sNewE.isEmpty() || sNewW.isEmpty()) {
                                Toast.makeText(getContext(), getString(R.string.bill_input_full_meter), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            int newE = Integer.parseInt(sNewE);
                            int newW = Integer.parseInt(sNewW);
                            if (newE < oldE || newW < oldW) {
                                Toast.makeText(getContext(), getString(R.string.bill_new_meter_must_be_greater), Toast.LENGTH_SHORT).show();
                                return;
                            }
                            viewModel.updateBillMeter(bill.billId, oldE, newE, oldW, newW);
                        })
                        .setNegativeButton(R.string.bill_cancel, null)
                        .create();

                Runnable updateTotal = () -> {
                    try {
                        int ne = edtNewE.getText().toString().isEmpty() ? oldE : Integer.parseInt(edtNewE.getText().toString());
                        int nw = edtNewW.getText().toString().isEmpty() ? oldW : Integer.parseInt(edtNewW.getText().toString());

                        boolean hasError = false;
                        if (ne < oldE) {
                            layNewE.setError("Khong duoc nho hon so cu (" + oldE + ")");
                            hasError = true;
                        } else {
                            layNewE.setError(null);
                        }

                        if (nw < oldW) {
                            layNewW.setError("Khong duoc nho hon so cu (" + oldW + ")");
                            hasError = true;
                        } else {
                            layNewW.setError(null);
                        }

                        if (dialog.isShowing()) {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!hasError);
                        }

                        int eu = Math.max(0, ne - oldE);
                        int wu = Math.max(0, nw - oldW);

                        double em = eu * bill.electricPrice;
                        double wm = wu * bill.waterPrice;
                        double rent = bill.rentPrice;
                        double svcFee = bill.serviceFee;

                        if (rent == 0 && svcFee == 0) {
                            rent = bill.totalAmount - (Math.max(0, bill.newElectric - bill.oldElectric) * bill.electricPrice)
                                    - (Math.max(0, bill.newWater - bill.oldWater) * bill.waterPrice);
                            rent = Math.max(0, rent);
                        }

                        TextView txtRentAndService = view.findViewById(R.id.txtRentAndService);
                        if (txtRentAndService != null) {
                            txtRentAndService.setText("Tien phong: " + fmt.format(rent) + " d\n"
                                    + "Phi DV, Rac, Wifi: " + fmt.format(svcFee) + " d");
                        }

                        double finalTotal = rent + svcFee + em + wm;
                        txtPreviewTotal.setText(fmt.format(finalTotal) + " d");
                    } catch (Exception ignored) {
                    }
                };

                android.text.TextWatcher watcher = new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) { updateTotal.run(); }
                };
                edtNewE.addTextChangedListener(watcher);
                edtNewW.addTextChangedListener(watcher);

                dialog.setOnShowListener(d -> updateTotal.run());
                dialog.show();
            });
        });
    }

    private void showPaymentDialog(BillWithInfo bill) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        CurrencyInputHelper.attach(input);

        long remaining = (long) (bill.totalAmount - bill.totalPaid);
        if (remaining > 0) {
            input.setText(String.valueOf(remaining));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.bill_input_payment_title)
                .setView(input)
                .setPositiveButton(R.string.bill_confirm, (d, w) -> {
                    String text = input.getText().toString().replaceAll("[^\\d]", "");
                    if (text.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.bill_input_amount), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount = Double.parseDouble(text);
                    viewModel.addPayment(bill.billId, amount);
                })
                .setNegativeButton(R.string.bill_cancel, null)
                .show();
    }

    private void showBillDetailDialog(BillWithInfo bill) {
        ioExecutor.execute(() -> {
            if (!isAdded()) return;
            com.example.rentalmanager.data.entity.SettingEntity setting =
                    AppDatabase.getInstance(requireContext()).settingDao().getSetting();

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
                View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_bill_detail, null);

                TextView txtElectric = view.findViewById(R.id.txtElectric);
                TextView txtWater = view.findViewById(R.id.txtWater);
                TextView txtRent = view.findViewById(R.id.txtRent);
                TextView txtServiceFee = view.findViewById(R.id.txtServiceFee);
                TextView txtTotal = view.findViewById(R.id.txtTotal);

                int eUsed = bill.newElectric - bill.oldElectric;
                int wUsed = bill.newWater - bill.oldWater;

                double ePrice = bill.electricPrice;
                double wPrice = bill.waterPrice;
                if (ePrice <= 0 || wPrice <= 0) {
                    if (setting != null) {
                        if (ePrice <= 0) ePrice = setting.electricPrice > 0 ? setting.electricPrice : 3500;
                        if (wPrice <= 0) wPrice = setting.waterPrice > 0 ? setting.waterPrice : 20000;
                    } else {
                        if (ePrice <= 0) ePrice = 3500;
                        if (wPrice <= 0) wPrice = 20000;
                    }
                }

                double eMoney = eUsed * ePrice;
                double wMoney = wUsed * wPrice;
                double rent = bill.rentPrice;
                double serviceFee = bill.serviceFee;
                if (rent == 0 && serviceFee == 0) {
                    rent = bill.totalAmount - eMoney - wMoney;
                }

                txtElectric.setText("Chi so cu: " + bill.oldElectric + "\n"
                        + "Chi so moi: " + bill.newElectric + "\n"
                        + "Tieu thu: " + eUsed + " x " + format.format(ePrice) + " = " + format.format(eMoney) + " d");

                txtWater.setText("Chi so cu: " + bill.oldWater + "\n"
                        + "Chi so moi: " + bill.newWater + "\n"
                        + "Tieu thu: " + wUsed + " x " + format.format(wPrice) + " = " + format.format(wMoney) + " d");

                txtRent.setText(format.format(Math.max(rent, 0)) + " d");
                if (txtServiceFee != null) {
                    txtServiceFee.setText(format.format(Math.max(serviceFee, 0)) + " d");
                }
                txtTotal.setText(format.format(bill.totalAmount) + " d");

                com.google.android.material.button.MaterialButton btnQR = view.findViewById(R.id.btnShowQR);
                android.widget.ImageView imgQR = view.findViewById(R.id.imgVietQR);
                android.widget.TextView txtQRNote = view.findViewById(R.id.txtQRNote);

                final double fEPrice = ePrice;
                final double fWPrice = wPrice;

                btnQR.setOnClickListener(vv -> {
                    boolean visible = imgQR.getVisibility() == View.VISIBLE;
                    if (visible) {
                        imgQR.setVisibility(View.GONE);
                        txtQRNote.setVisibility(View.GONE);
                        btnQR.setText(getString(R.string.bill_qr_create));
                    } else {
                        AppDatabase dbQR = AppDatabase.getInstance(requireContext());
                        ioExecutor.execute(() -> {
                            com.example.rentalmanager.data.entity.SettingEntity s = dbQR.settingDao().getSetting();
                            if (!isAdded()) return;
                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                String bank = (s != null && s.bankCode != null && !s.bankCode.isEmpty())
                                        ? s.bankCode.toUpperCase(Locale.ROOT).trim() : "MB";
                                String account = (s != null && s.bankAccount != null && !s.bankAccount.isEmpty())
                                        ? s.bankAccount.trim() : "0000000000";
                                String tName = bill.tenantName == null ? "" : java.text.Normalizer
                                        .normalize(bill.tenantName, java.text.Normalizer.Form.NFD)
                                        .replaceAll("\\p{M}", "").replace("\u0110", "D").replace("\u0111", "d");
                                String rName = bill.roomName == null ? "" : java.text.Normalizer
                                        .normalize(bill.roomName, java.text.Normalizer.Form.NFD)
                                        .replaceAll("\\p{M}", "").replace("\u0110", "D").replace("\u0111", "d");
                                String prefix = rName.toLowerCase(Locale.ROOT).startsWith("phong") ? " " : " phong ";
                                String rawDesc = tName + prefix + rName + " thang " + bill.month.replace("-", "");
                                String desc;
                                try {
                                    desc = java.net.URLEncoder.encode(rawDesc, "UTF-8").replace("+", "%20");
                                } catch (java.io.UnsupportedEncodingException e) {
                                    desc = "";
                                }
                                long amount = (long) bill.totalAmount;
                                String qrUrl = "https://img.vietqr.io/image/" + bank + "-" + account
                                        + "-compact.png?amount=" + amount + "&addInfo=" + desc
                                        + "&accountName=Nha%20Tro";

                                com.bumptech.glide.Glide.with(requireContext())
                                        .load(qrUrl)
                                        .placeholder(android.R.drawable.ic_popup_sync)
                                        .error(android.R.drawable.ic_dialog_alert)
                                        .into(imgQR);
                                imgQR.setVisibility(View.VISIBLE);
                                txtQRNote.setVisibility(View.VISIBLE);
                                btnQR.setText(R.string.bill_qr_hide);
                            });
                        });
                    }
                });

                androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.bill_detail_title_prefix) + bill.month)
                        .setView(view)
                        .setNeutralButton(R.string.bill_share, (d, which) ->
                                com.example.rentalmanager.util.InvoiceShareHelper.share(requireContext(), bill, fEPrice, fWPrice))
                        .create();

                view.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> dialog.dismiss());
                view.findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> {
                    dialog.dismiss();
                    showPaymentDialog(bill);
                });

                final double fEPdf = ePrice, fWPdf = wPrice;
                view.findViewById(R.id.btnExportPdf).setOnClickListener(v ->
                        com.example.rentalmanager.util.InvoicePdfHelper.exportAndShare(
                                requireContext(), bill, fEPdf, fWPdf));

                dialog.show();
            });
        });
    }

    private void showMonthPickerDialog() {
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 4, padding, 0);

        com.google.android.material.textfield.TextInputLayout textInputLayout = 
                new com.google.android.material.textfield.TextInputLayout(requireContext());
        textInputLayout.setHint("Tháng thao tác (YYYY-MM)");
        textInputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        textInputLayout.setBoxCornerRadii(30, 30, 30, 30);
        
        com.google.android.material.textfield.TextInputEditText input = 
                new com.google.android.material.textfield.TextInputEditText(textInputLayout.getContext());
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_DATETIME);

        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(System.currentTimeMillis());
        input.setText(currentMonth);

        textInputLayout.addView(input);
        container.addView(textInputLayout);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tạo hóa đơn hàng loạt")
                .setMessage("Tùy chọn tạo hóa đơn cho tất cả các phòng đang thuê trong tháng bạn chọn bên dưới.")
                .setView(container)
                .setPositiveButton("Bắt đầu tạo", (dialog, which) -> {
                    String month = input.getText().toString().trim();
                    if (month.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.bill_input_month_required), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.generateBills(month);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
