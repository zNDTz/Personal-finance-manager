package com.example.personal_finance_manager;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.activities.AddTransactionActivity;
import com.example.personal_finance_manager.activities.CategoryActivity;
import com.example.personal_finance_manager.activities.EditTransactionActivity;
import com.example.personal_finance_manager.activities.LoginActivity;
import com.example.personal_finance_manager.activities.ProfileActivity;
import com.example.personal_finance_manager.activities.SavingGoalActivity;
import com.example.personal_finance_manager.activities.StatisticsActivity;
import com.example.personal_finance_manager.activities.WalletActivity;
import com.example.personal_finance_manager.adapters.TransactionAdapter;
import com.example.personal_finance_manager.api.ApiClient;
import com.example.personal_finance_manager.api.AppDatabase;
import com.example.personal_finance_manager.api.CategoryRepository;
import com.example.personal_finance_manager.api.TransactionRepository;
import com.example.personal_finance_manager.api.WalletRepository;
import com.example.personal_finance_manager.models.Category;
import com.example.personal_finance_manager.models.Transaction;
import com.example.personal_finance_manager.models.User;
import com.example.personal_finance_manager.models.Wallet;
import com.example.personal_finance_manager.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> fullTransactionList = new ArrayList<>();
    private List<Transaction> displayList = new ArrayList<>();
    private List<Wallet> walletList = new ArrayList<>();
    private TokenManager tokenManager;
    private FloatingActionButton fabAdd;
    private BottomNavigationView bottomNavigationView;
    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpense, tvBalanceLabel;
    private EditText etSearch;
    private ImageButton btnFilter;
    private Spinner spinnerWalletFilter;
    
    private ImageView ivUserAvatarHeader;
    private TextView tvHeaderUsername;
    private View layoutUserHeader;

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private WalletRepository walletRepository;

    private String filterType = "ALL";
    private String filterCategory = "Tất cả";
    private String filterStartDate = "";
    private String filterEndDate = "";
    private String filterWalletId = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);
        if (tokenManager.getToken() == null) {
            goToLogin();
            return;
        }

        transactionRepository = new TransactionRepository(this);
        categoryRepository = new CategoryRepository(this);
        walletRepository = new WalletRepository(this);

        initViews();
        setupRecyclerView();
        setupBottomNav();
        setupSearchAndFilter();
        loadUserProfile();

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddTransactionActivity.class));
        });

        if (layoutUserHeader != null) {
            layoutUserHeader.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            });
        }
    }

    private void goToLogin() {
        tokenManager.clearToken();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).clearAllTables();
        });
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tokenManager.getToken() != null) {
            loadWallets();
            loadTransactions();
            loadUserProfile();
            bottomNavigationView.setSelectedItemId(R.id.nav_transactions);
        }
    }

    private void initViews() {
        rvTransactions = findViewById(R.id.rvTransactions);
        fabAdd = findViewById(R.id.fab_add);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvBalanceLabel = findViewById(R.id.tvBalanceLabel);
        etSearch = findViewById(R.id.etSearch);
        btnFilter = findViewById(R.id.btnFilter);
        spinnerWalletFilter = findViewById(R.id.spinnerWalletFilter);
        
        layoutUserHeader = findViewById(R.id.layoutHeader);
        ivUserAvatarHeader = findViewById(R.id.ivUserAvatarHeader);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFilter.setOnClickListener(v -> showFilterDialog());

        spinnerWalletFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    filterWalletId = "ALL";
                } else {
                    filterWalletId = walletList.get(position - 1).getId();
                }
                applyFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadWallets() {
        walletRepository.getWallets(new WalletRepository.DataCallback<List<Wallet>>() {
            @Override
            public void onSuccess(List<Wallet> data) {
                walletList.clear();
                walletList.addAll(data);
                
                List<String> walletNames = new ArrayList<>();
                walletNames.add("Tất cả ví");
                for (Wallet w : data) {
                    walletNames.add(w.getName());
                }
                
                ArrayAdapter<String> walletAdapter = new ArrayAdapter<>(MainActivity.this, 
                    android.R.layout.simple_spinner_item, walletNames);
                walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerWalletFilter.setAdapter(walletAdapter);

                applyFilters();
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_advanced_filter, null);
        
        RadioGroup rgType = view.findViewById(R.id.rgFilterType);
        Spinner spinnerCategory = view.findViewById(R.id.spinnerFilterCategory);
        TextView tvStartDate = view.findViewById(R.id.tvStartDate);
        TextView tvEndDate = view.findViewById(R.id.tvEndDate);
        Button btnReset = view.findViewById(R.id.btnResetFilter);

        if ("INCOME".equals(filterType)) rgType.check(R.id.rbFilterIncome);
        else if ("EXPENSE".equals(filterType)) rgType.check(R.id.rbFilterExpense);
        else rgType.check(R.id.rbFilterAll);

        tvStartDate.setText(filterStartDate.isEmpty() ? "Từ ngày" : filterStartDate);
        tvEndDate.setText(filterEndDate.isEmpty() ? "Đến ngày" : filterEndDate);

        categoryRepository.getCategories(new CategoryRepository.DataCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> categories) {
                List<String> categoryNames = new ArrayList<>();
                categoryNames.add("Tất cả");
                for (Category c : categories) categoryNames.add(c.getName());
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, 
                    android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
                
                int pos = categoryNames.indexOf(filterCategory);
                if (pos >= 0) spinnerCategory.setSelection(pos);
            }
            @Override
            public void onError(String message) {}
        });

        tvStartDate.setOnClickListener(v -> showDatePicker(tvStartDate, true));
        tvEndDate.setOnClickListener(v -> showDatePicker(tvEndDate, false));

        btnReset.setOnClickListener(v -> {
            rgType.check(R.id.rbFilterAll);
            spinnerCategory.setSelection(0);
            tvStartDate.setText("Từ ngày");
            tvEndDate.setText("Đến ngày");
            filterStartDate = "";
            filterEndDate = "";
        });

        builder.setView(view)
                .setTitle("Lọc nâng cao")
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    int checkedId = rgType.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbFilterIncome) filterType = "INCOME";
                    else if (checkedId == R.id.rbFilterExpense) filterType = "EXPENSE";
                    else filterType = "ALL";

                    filterCategory = spinnerCategory.getSelectedItem().toString();
                    filterStartDate = tvStartDate.getText().toString().equals("Từ ngày") ? "" : tvStartDate.getText().toString();
                    filterEndDate = tvEndDate.getText().toString().equals("Đến ngày") ? "" : tvEndDate.getText().toString();

                    applyFilters();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDatePicker(TextView textView, boolean isStartDate) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            textView.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        List<Transaction> filtered = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Transaction t : fullTransactionList) {
            String title = t.getTitle() != null ? t.getTitle().toLowerCase() : "";
            String category = t.getCategoryName() != null ? t.getCategoryName().toLowerCase() : "";
            
            boolean matchesQuery = title.contains(query) || category.contains(query);
            if (!matchesQuery) continue;

            if (!filterType.equals("ALL") && !t.getType().equals(filterType)) continue;

            if (!filterCategory.equals("Tất cả") && !filterCategory.equals(t.getCategoryName())) continue;

            if (!filterWalletId.equals("ALL") && (t.getWalletId() == null || !t.getWalletId().equals(filterWalletId))) continue;

            try {
                if (!filterStartDate.isEmpty()) {
                    Date start = sdf.parse(filterStartDate);
                    Date transDate = sdf.parse(t.getDate());
                    if (transDate.before(start)) continue;
                }
                if (!filterEndDate.isEmpty()) {
                    Date end = sdf.parse(filterEndDate);
                    Date transDate = sdf.parse(t.getDate());
                    if (transDate.after(end)) continue;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            filtered.add(t);
        }

        displayList.clear();
        displayList.addAll(filtered);
        adapter.notifyDataSetChanged();
        calculateSummary(filtered);
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

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(displayList, transaction -> {
            Intent intent = new Intent(MainActivity.this, EditTransactionActivity.class);
            intent.putExtra("transaction", new Gson().toJson(transaction));
            startActivity(intent);
        });
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void loadTransactions() {
        transactionRepository.getTransactions(new TransactionRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> data) {
                fullTransactionList.clear();
                fullTransactionList.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                if (fullTransactionList.isEmpty()) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void calculateSummary(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpense = 0;
        double initialBalance = 0;

        if (filterWalletId.equals("ALL")) {
            for (Wallet w : walletList) {
                initialBalance += w.getBalance();
            }
        } else {
            for (Wallet w : walletList) {
                if (w.getId().equals(filterWalletId)) {
                    initialBalance = w.getBalance();
                    break;
                }
            }
        }

        for (Transaction t : transactions) {
            if ("INCOME".equals(t.getType())) {
                totalIncome += t.getAmount();
            } else if ("EXPENSE".equals(t.getType())) {
                totalExpense += t.getAmount();
            }
        }

        double currentBalance = initialBalance + totalIncome - totalExpense;

        if (tvBalanceLabel != null) {
            if (currentBalance < 0) {
                tvBalanceLabel.setText("Số dư âm");
            } else {
                tvBalanceLabel.setText("Số dư ví");
            }
        }

        tvTotalBalance.setText(String.format(Locale.getDefault(), "%,.0f đ", currentBalance));
        tvTotalIncome.setText(String.format(Locale.getDefault(), "%,.0f đ", totalIncome));
        tvTotalExpense.setText(String.format(Locale.getDefault(), "%,.0f đ", totalExpense));
    }

    private void setupBottomNav() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_transactions) {
                return true;
            } else if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, StatisticsActivity.class));
                return true;
            } else if (id == R.id.nav_saving_goals) {
                startActivity(new Intent(this, SavingGoalActivity.class));
                return true;
            } else if (id == R.id.nav_categories) {
                startActivity(new Intent(this, CategoryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.nav_logout) {
                goToLogin();
                return true;
            }
            return false;
        });
    }
}
