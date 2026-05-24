package com.example.personal_finance_manager.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.api.ApiClient;
import com.example.personal_finance_manager.api.CategoryRepository;
import com.example.personal_finance_manager.api.TransactionRepository;
import com.example.personal_finance_manager.api.WalletRepository;
import com.example.personal_finance_manager.models.Category;
import com.example.personal_finance_manager.models.Transaction;
import com.example.personal_finance_manager.models.User;
import com.example.personal_finance_manager.models.Wallet;
import com.example.personal_finance_manager.utils.TokenManager;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionActivity extends AppCompatActivity {

    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory, spinnerWallet;
    private RadioGroup rgType;
    private Button btnSave;
    private TokenManager tokenManager;
    private List<Category> allCategories = new ArrayList<>();
    private List<Category> filteredCategories = new ArrayList<>();
    private List<Wallet> allWallets = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private ArrayAdapter<Category> categoryAdapter;
    private ArrayAdapter<Wallet> walletAdapter;
    
    private ImageView ivUserAvatarHeader;
    private TextView tvHeaderUsername;

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private WalletRepository walletRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        tokenManager = new TokenManager(this);
        transactionRepository = new TransactionRepository(this);
        categoryRepository = new CategoryRepository(this);
        walletRepository = new WalletRepository(this);

        setupToolbar();
        initViews();
        loadUserProfile();
        loadCategories();
        loadWallets();
        loadTransactions();

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedType = (checkedId == R.id.rbIncome) ? "INCOME" : "EXPENSE";
            updateCategorySpinner(selectedType);
        });

        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thêm Giao Dịch");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void initViews() {
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerWallet = findViewById(R.id.spinnerWallet);
        rgType = findViewById(R.id.rgType);
        btnSave = findViewById(R.id.btnSave);
        
        ivUserAvatarHeader = findViewById(R.id.ivUserAvatarHeader);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredCategories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        walletAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allWallets);
        walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(walletAdapter);
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

    private void loadCategories() {
        categoryRepository.getCategories(new CategoryRepository.DataCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> data) {
                allCategories.clear();
                allCategories.addAll(data);
                String currentType = (rgType.getCheckedRadioButtonId() == R.id.rbIncome) ? "INCOME" : "EXPENSE";
                updateCategorySpinner(currentType);
            }

            @Override
            public void onError(String message) {
                if (allCategories.isEmpty()) {
                    Toast.makeText(AddTransactionActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadWallets() {
        walletRepository.getWallets(new WalletRepository.DataCallback<List<Wallet>>() {
            @Override
            public void onSuccess(List<Wallet> data) {
                allWallets.clear();
                allWallets.addAll(data);
                walletAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddTransactionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTransactions() {
        transactionRepository.getTransactions(new TransactionRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> data) {
                allTransactions.clear();
                allTransactions.addAll(data);
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void updateCategorySpinner(String type) {
        filteredCategories.clear();
        for (Category cat : allCategories) {
            if (type.equals(cat.getType())) {
                filteredCategories.add(cat);
            }
        }
        categoryAdapter.notifyDataSetChanged();
        if (!filteredCategories.isEmpty()) {
            spinnerCategory.setSelection(0);
        }
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        Wallet selectedWallet = (Wallet) spinnerWallet.getSelectedItem();
        
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedWallet == null) {
            Toast.makeText(this, "Vui lòng chọn ví", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String type = (rgType.getCheckedRadioButtonId() == R.id.rbIncome) ? "INCOME" : "EXPENSE";

        if (!selectedCategory.getType().equals(type)) {
            Toast.makeText(this, "Danh mục không phù hợp với loại giao dịch!", Toast.LENGTH_LONG).show();
            return;
        }
        
        checkBudget(selectedCategory, amount, type);

        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Transaction transaction = new Transaction();
        transaction.setId("temp_" + System.currentTimeMillis());
        transaction.setAmount(amount);
        transaction.setTitle(description);
        transaction.setCategory(selectedCategory.getName());
        transaction.setCategoryName(selectedCategory.getName());
        transaction.setCategoryId(selectedCategory.getId());
        transaction.setType(type);
        transaction.setDate(dateStr);
        transaction.setWalletId(selectedWallet.getId());

        transactionRepository.addTransaction(transaction, new TransactionRepository.DataCallback<Transaction>() {
            @Override
            public void onSuccess(Transaction data) {
                Toast.makeText(AddTransactionActivity.this, "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(AddTransactionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkBudget(Category category, double amount, String type) {
        if (category.getBudget() <= 0) return;

        double currentTotal = 0;
        for (Transaction t : allTransactions) {
            if (category.getName().equals(t.getCategoryName()) && type.equals(t.getType())) {
                currentTotal += t.getAmount();
            }
        }

        double newTotal = currentTotal + amount;
        if (newTotal > category.getBudget()) {
            if ("EXPENSE".equals(type)) {
                Toast.makeText(this, "⚠️ Cảnh báo: Vượt ngưỡng chi tiêu hàng tháng cho " + category.getName(), Toast.LENGTH_LONG).show();
            } else if ("INCOME".equals(type)) {
                Toast.makeText(this, "🎉 Thông báo: Vượt chỉ tiêu ngân sách thu nhập cho " + category.getName(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
