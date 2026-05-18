package com.example.rentalmanager.ui.report;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rentalmanager.R;
import com.example.rentalmanager.data.database.AppDatabase;
import com.example.rentalmanager.util.AppExecutors;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
public class ReportFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_report, container, false);

        ImageButton btnBack = v.findViewById(R.id.btnBackReport);
        if (btnBack != null) {
            btnBack.setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
            );
        }

        BarChart barChart = v.findViewById(R.id.barChartRevenue);
        PieChart pieChart = v.findViewById(R.id.pieChartRooms);

        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            // ===== 6 tháng doanh thu =====
            List<BarEntry> barEntries = new ArrayList<>();
            List<String> monthLabels = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            SimpleDateFormat labelFmt = new SimpleDateFormat("MM/yy", Locale.getDefault());
            NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

            String maxMonthStr = db.billDao().getMaxMonth();
            if (maxMonthStr != null && !maxMonthStr.isEmpty()) {
                try {
                    java.util.Date d = sdf.parse(maxMonthStr);
                    if (d != null && d.after(cal.getTime())) {
                        cal.setTime(d);
                    }
                } catch (Exception ignored) {}
            }

            for (int i = 5; i >= 0; i--) {
                Calendar c = (Calendar) cal.clone();
                c.add(Calendar.MONTH, -i);
                String month = sdf.format(c.getTime());
                String label = labelFmt.format(c.getTime());
                double revenue = db.billDao().getRevenueByMonth(month);
                barEntries.add(new BarEntry(5 - i, (float) (revenue / 1_000_000f)));
                monthLabels.add(label);
            }

            // ===== Tình trạng phòng =====
            int total    = db.roomDao().countTotalRoomsSync();
            int occupied = db.roomDao().countOccupiedRoomsSync();
            int empty    = total - occupied;

            // ===== Hợp đồng + Nợ toàn cục (Used logically if needed, but not on UI) =====
            int activeContracts = db.contractDao().countActiveContracts();
            String thisMonth = sdf.format(cal.getTime());
            double debt = db.billDao().getTotalDebt();

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;


                // --- BAR CHART ---
                BarDataSet barDataSet = new BarDataSet(barEntries, "Triệu đồng");
                barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                barDataSet.setValueTextSize(10f);
                BarData barData = new BarData(barDataSet);
                barData.setBarWidth(0.6f);
                barChart.setData(barData);
                barChart.getDescription().setEnabled(false);
                barChart.getLegend().setEnabled(false);
                barChart.setFitBars(true);
                barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(monthLabels));
                barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barChart.getXAxis().setGranularity(1f);
                barChart.getXAxis().setDrawGridLines(false);
                barChart.getAxisRight().setEnabled(false);
                barChart.getAxisLeft().setAxisMinimum(0f);
                barChart.animateY(800);
                barChart.invalidate();

                // --- PIE CHART ---
                List<PieEntry> pieEntries = new ArrayList<>();
                List<Integer> pieColors = new ArrayList<>();
                List<Integer> textColors = new ArrayList<>();

                if (occupied > 0) {
                    pieEntries.add(new PieEntry(occupied, "Đang thuê"));
                    pieColors.add(Color.parseColor("#6366F1"));
                    textColors.add(Color.WHITE);
                }
                if (empty > 0) {
                    pieEntries.add(new PieEntry(empty, "Trống"));
                    pieColors.add(Color.parseColor("#D1D5DB"));
                    textColors.add(Color.DKGRAY);
                }

                PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
                pieDataSet.setColors(pieColors);
                pieDataSet.setValueTextSize(14f);
                pieDataSet.setValueTextColors(textColors);
                PieData pieData = new PieData(pieDataSet);
                pieData.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) Math.round(value));
                    }
                });
                pieChart.setData(pieData);
                pieChart.getDescription().setEnabled(false);
                pieChart.setHoleRadius(45f);
                pieChart.setTransparentCircleRadius(50f);
                pieChart.setCenterText(total + " phòng");
                pieChart.setCenterTextSize(14f);
                pieChart.getLegend().setEnabled(true);
                pieChart.animateY(800);
                pieChart.invalidate();

                // --- SUMMARY CARDS ---
                // Summary cards have been removed from UI
            });
        });

        return v;
    }
}
