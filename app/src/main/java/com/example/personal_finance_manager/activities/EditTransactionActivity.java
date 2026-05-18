package com.example.personal_finance_manager.activities;

import android.app.AlertDialog;
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
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditTransactionActivity extends AppCompatActivity {

    private TextInputEditText etAmount, etDescription;
    private Spinner spinnerCategory, spinnerWallet;
    private RadioGroup rgType;
    private Button btnUpdate, btnDelete;
    private TokenManager tokenManager;
    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private ArrayAdapter<Category> categoryAdapter;
    private ArrayAdapter<Wallet> walletAdapter;
    private Transaction currentTransaction;

    private ImageView ivUserAvatarHeader;
    private TextView tvHeaderUsername;

    // Repositories
    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private WalletRepository walletRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_transaction);

        tokenManager = new TokenManager(this);
        transactionRepository = new TransactionRepository(this);
        categoryRepository = new CategoryRepository(this);
        walletRepository = new WalletRepository(this);
        
        String transactionJson = getIntent().getStringExtra("transaction");
        if (transactionJson != null) {
            currentTransaction = new Gson().fromJson(transactionJson, Transaction.class);
        }

        setupToolbar();
        initViews();
        loadUserProfile();
        loadCategories();
        loadWallets();
        loadTransactions();

        btnUpdate.setOnClickListener(v -> updateTransaction());
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
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
        etAmount = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerWallet = findViewById(R.id.spinnerWallet);
        rgType = findViewById(R.id.rgType);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);

        ivUserAvatarHeader = findViewById(R.id.ivUserAvatarHeader);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);

        if (currentTransaction != null) {
            etAmount.setText(String.format(Locale.US, "%.0f", currentTransaction.getAmount()));
            etDescription.setText(currentTransaction.getTitle());
            if ("INCOME".equals(currentTransaction.getType())) {
                rgType.check(R.id.rbIncome);
            } else {
                rgType.check(R.id.rbExpense);
            }
        }

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        walletAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wallets);
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
                categories.clear();
                categories.addAll(data);
                categoryAdapter.notifyDataSetChanged();
                
                if (currentTransaction != null) {
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getName().equals(currentTransaction.getCategory())) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EditTransactionActivity.this, "Sử dụng danh mục offline", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWallets() {
        walletRepository.getWallets(new WalletRepository.DataCallback<List<Wallet>>() {
            @Override
            public void onSuccess(List<Wallet> data) {
                wallets.clear();
                wallets.addAll(data);
                walletAdapter.notifyDataSetChanged();

                if (currentTransaction != null && currentTransaction.getWalletId() != null) {
                    for (int i = 0; i < wallets.size(); i++) {
                        if (wallets.get(i).getId().equals(currentTransaction.getWalletId())) {
                            spinnerWallet.setSelection(i);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onError(String message) {}
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

    private void updateTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        Wallet selectedWallet = (Wallet) spinnerWallet.getSelectedItem();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedWallet == null) {
            Toast.makeText(this, "Chưa chọn ví", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "INCOME" : "EXPENSE";

        // Kiểm tra ngân sách trước khi cập nhật
        if (selectedCategory != null) {
            checkBudget(selectedCategory, amount, type);
        }

        currentTransaction.setAmount(amount);
        currentTransaction.setTitle(description);
        currentTransaction.setType(type);
        currentTransaction.setWalletId(selectedWallet.getId());
        if (selectedCategory != null) {
            currentTransaction.setCategory(selectedCategory.getName());
            currentTransaction.setCategoryName(selectedCategory.getName());
            currentTransaction.setCategoryId(selectedCategory.getId());
        }

        transactionRepository.updateTransaction(currentTransaction, new TransactionRepository.DataCallback<Transaction>() {
            @Override
            public void onSuccess(Transaction data) {
                Toast.makeText(EditTransactionActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EditTransactionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkBudget(Category category, double amount, String type) {
        if (category.getBudget() <= 0) return;

        double currentTotal = 0;
        for (Transaction t : allTransactions) {
            // Không tính giao dịch hiện tại đang sửa
            if (t.getId().equals(currentTransaction.getId())) continue;

            if (category.getName().equals(t.getCategoryName()) && type.equals(t.getType())) {
                currentTotal += t.getAmount();
            }
        }

        double newTotal = currentTotal + amount;
        if (newTotal > category.getBudget()) {
            if ("EXPENSE".equals(type)) {
                Toast.makeText(this, "⚠️ Cảnh báo: Vượt ngưỡng chi tiêu hàng tháng cho " + category.getName(), Toast.LENGTH_LONG).show();
            } else if ("INCOME".equals(type)) {
                Toast.makeText(this, "🎉 Thông báo: Vượt chỉ tiêu thu nhập cho " + category.getName(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTransaction())
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTransaction() {
        transactionRepository.deleteTransaction(currentTransaction.getId(), new TransactionRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(EditTransactionActivity.this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EditTransactionActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
