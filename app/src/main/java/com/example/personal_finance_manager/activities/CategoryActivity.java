package com.example.personal_finance_manager.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.adapters.CategoryAdapter;
import com.example.personal_finance_manager.api.ApiClient;
import com.example.personal_finance_manager.api.CategoryRepository;
import com.example.personal_finance_manager.api.TransactionRepository;
import com.example.personal_finance_manager.models.Category;
import com.example.personal_finance_manager.models.Transaction;
import com.example.personal_finance_manager.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();
    private FloatingActionButton fabAddCategory;
    
    private ImageView ivUserAvatarHeader;
    private TextView tvHeaderUsername;
    private View layoutUserHeader;

    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryRepository = new CategoryRepository(this);
        transactionRepository = new TransactionRepository(this);

        setupToolbar();
        initViews();
        setupRecyclerView();
        loadUserProfile();

        fabAddCategory.setOnClickListener(v -> showCategoryDialog(null));
        
        if (layoutUserHeader != null) {
            layoutUserHeader.setOnClickListener(v -> {
                startActivity(new Intent(CategoryActivity.this, ProfileActivity.class));
            });
        }

        loadData();
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
        rvCategories = findViewById(R.id.rvCategories);
        fabAddCategory = findViewById(R.id.fabAddCategory);
        
        layoutUserHeader = findViewById(R.id.layoutHeader);
        ivUserAvatarHeader = findViewById(R.id.ivUserAvatarHeader);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdapter(categoryList, this::showCategoryDialog);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);
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

    private void loadData() {
        transactionRepository.getTransactions(new TransactionRepository.DataCallback<List<Transaction>>() {
            @Override
            public void onSuccess(List<Transaction> transactions) {
                adapter.setTransactions(transactions);
                loadCategories();
            }

            @Override
            public void onError(String message) {
                loadCategories();
            }
        });
    }

    private void loadCategories() {
        categoryRepository.getCategories(new CategoryRepository.DataCallback<List<Category>>() {
            @Override
            public void onSuccess(List<Category> data) {
                categoryList.clear();
                categoryList.addAll(data);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                if (categoryList.isEmpty()) {
                    Toast.makeText(CategoryActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        EditText etName = view.findViewById(R.id.etCategoryName);
        EditText etBudget = view.findViewById(R.id.etCategoryBudget);
        RadioGroup rgType = view.findViewById(R.id.rgCategoryType);

        if (category != null) {
            builder.setTitle("Sửa danh mục");
            etName.setText(category.getName());
            etBudget.setText(String.format(Locale.US, "%.0f", category.getBudget()));
            rgType.check(category.getType().equals("INCOME") ? R.id.rbIncome : R.id.rbExpense);
            
            builder.setNeutralButton("Xóa", (dialog, which) -> showDeleteConfirmation(category.getId()));
        } else {
            builder.setTitle("Thêm danh mục mới");
        }

        builder.setView(view)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String budgetStr = etBudget.getText().toString().trim();
                    double budget = budgetStr.isEmpty() ? 0 : Double.parseDouble(budgetStr);
                    String type = rgType.getCheckedRadioButtonId() == R.id.rbIncome ? "INCOME" : "EXPENSE";
                    
                    if (!name.isEmpty()) {
                        if (category == null) {
                            Category newCat = new Category("temp_" + System.currentTimeMillis(), name, type);
                            newCat.setBudget(budget);
                            addCategory(newCat);
                        } else {
                            category.setName(name);
                            category.setType(type);
                            category.setBudget(budget);
                            updateCategory(category);
                        }
                    } else {
                        Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteConfirmation(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteCategory(id))
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void addCategory(Category category) {
        categoryRepository.addCategory(category, new CategoryRepository.DataCallback<Category>() {
            @Override
            public void onSuccess(Category data) {
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CategoryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategory(Category category) {
        categoryRepository.updateCategory(category, new CategoryRepository.DataCallback<Category>() {
            @Override
            public void onSuccess(Category data) {
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CategoryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCategory(String id) {
        categoryRepository.deleteCategory(id, new CategoryRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadData();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(CategoryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
