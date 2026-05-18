package com.example.personal_finance_manager.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.api.ApiClient;
import com.example.personal_finance_manager.api.TransactionRepository;
import com.example.personal_finance_manager.models.Transaction;
import com.example.personal_finance_manager.models.User;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private PieChart chartExpense, chartIncome;
    private BarChart barChartMonthly;
    
    private ImageView ivUserAvatarHeader;
    private TextView tvHeaderUsername;
    private View layoutUserHeader;

    private TransactionRepository transactionRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        transactionRepository = new TransactionRepository(this);

        setupToolbar();
        initViews();
        loadUserProfile();
        loadStatistics();
        
        if (layoutUserHeader != null) {
            layoutUserHeader.setOnClickListener(v -> {
                startActivity(new Intent(StatisticsActivity.this, ProfileActivity.class));
            });
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void initViews() {
        chartExpense = findViewById(R.id.chartExpense);
        chartIncome = findViewById(R.id.chartIncome);
        barChartMonthly = findViewById(R.id.barChartMonthly);
        
        layoutUserHeader = findViewById(R.id.layoutHeader);
        ivUserAvatarHeader = findViewById(R.id.ivUserAvatarHeader);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);

        setupPieChartStyle(chartExpense, "Chưa có chi tiêu");
        setupPieChartStyle(chartIncome, "Chưa có thu nhập");
        setupBarChartStyle();
    }

    private void loadUserProfile() {
        ApiClient.getApiService(this).getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    if (tvHeaderUsername != null) tvHeaderUsername.setText(user.getUsername());
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty() && ivUserAvatarHeader != null) {
                        try {
                            byte[] decodedString = Base64.decode(user.getAvatar(), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivUserAvatarHeader.setImageBitmap(decodedByte);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void setupPieChartStyle(PieChart chart, String noDataText) {
        chart.getDescription().setEnabled(false);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.WHITE);
        chart.setTransparentCircleRadius(61f);
        chart.setCenterTextSize(14f);
        chart.setNoDataText(noDataText);
        chart.setNoDataTextColor(Color.GRAY);
        chart.getLegend().setEnabled(true);
        chart.getLegend().setWordWrapEnabled(true);
    }

    private void setupBarChartStyle() {
        barChartMonthly.getDescription().setEnabled(false);
        barChartMonthly.setDrawGridBackground(false);
        barChartMonthly.setDrawBarShadow(false);
        barChartMonthly.setHighlightFullBarEnabled(false);
        barChartMonthly.setNoDataText("Chưa có dữ liệu hàng tháng");

        XAxis xAxis = barChartMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
    }

    private void loadStatistics() {
        transactionRepository.getTransactions(new TransactionRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> data) {
                processData(data);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(StatisticsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processData(List<Transaction> transactions) {
        Map<String, Double> expenseMap = new HashMap<>();
        Map<String, Double> incomeMap = new HashMap<>();
        
        Map<String, Double> monthlyIncome = new TreeMap<>();
        Map<String, Double> monthlyExpense = new TreeMap<>();

        for (Transaction t : transactions) {
            // Lấy tháng từ chuỗi "yyyy-MM-dd"
            String month = "Khác";
            if (t.getDate() != null && t.getDate().length() >= 7) {
                month = t.getDate().substring(0, 7); // Lấy "yyyy-MM"
            }

            String cat = t.getCategory() != null ? t.getCategory() : "Khác";

            if ("EXPENSE".equals(t.getType())) {
                expenseMap.put(cat, (expenseMap.getOrDefault(cat, 0.0)) + t.getAmount());
                monthlyExpense.put(month, monthlyExpense.getOrDefault(month, 0.0) + t.getAmount());
            } else if ("INCOME".equals(t.getType())) {
                incomeMap.put(cat, (incomeMap.getOrDefault(cat, 0.0)) + t.getAmount());
                monthlyIncome.put(month, monthlyIncome.getOrDefault(month, 0.0) + t.getAmount());
            }
        }

        updatePieChart(chartExpense, expenseMap, "Tổng Chi Tiêu", Color.parseColor("#C62828"));
        updatePieChart(chartIncome, incomeMap, "Tổng Thu Nhập", Color.parseColor("#2E7D32"));
        updateBarChart(monthlyIncome, monthlyExpense);
    }

    private void updatePieChart(PieChart chart, Map<String, Double> dataMap, String label, int centerTextColor) {
        List<PieEntry> entries = new ArrayList<>();
        double total = 0;

        for (Map.Entry<String, Double> entry : dataMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            total += entry.getValue();
        }

        if (entries.isEmpty()) {
            chart.setData(null);
            chart.setCenterText("Trống");
            chart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.setCenterText(label + "\n" + String.format(Locale.getDefault(), "%,.0f đ", total));
        chart.setCenterTextColor(centerTextColor);
        chart.animateY(1000);
        chart.invalidate();
    }

    private void updateBarChart(Map<String, Double> incomeData, Map<String, Double> expenseData) {
        List<String> months = new ArrayList<>();
        months.addAll(incomeData.keySet());
        for (String m : expenseData.keySet()) {
            if (!months.contains(m)) months.add(m);
        }
        Collections.sort(months);

        if (months.isEmpty()) return;

        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < months.size(); i++) {
            String m = months.get(i);
            incomeEntries.add(new BarEntry(i, incomeData.getOrDefault(m, 0.0).floatValue()));
            expenseEntries.add(new BarEntry(i, expenseData.getOrDefault(m, 0.0).floatValue()));
        }

        BarDataSet setIncome = new BarDataSet(incomeEntries, "Thu nhập");
        setIncome.setColor(Color.parseColor("#2E7D32"));
        
        BarDataSet setExpense = new BarDataSet(expenseEntries, "Chi tiêu");
        setExpense.setColor(Color.parseColor("#C62828"));

        float groupSpace = 0.08f;
        float barSpace = 0.03f;
        float barWidth = 0.43f;

        BarData data = new BarData(setIncome, setExpense);
        data.setBarWidth(barWidth);
        
        barChartMonthly.setData(data);
        barChartMonthly.getXAxis().setAxisMinimum(0);
        barChartMonthly.getXAxis().setAxisMaximum(months.size());
        barChartMonthly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(months));
        
        barChartMonthly.groupBars(0, groupSpace, barSpace);
        barChartMonthly.animateY(1000);
        barChartMonthly.invalidate();
    }
}
