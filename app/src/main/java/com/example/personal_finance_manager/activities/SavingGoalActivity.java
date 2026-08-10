package com.example.personal_finance_manager.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.adapters.SavingGoalAdapter;
import com.example.personal_finance_manager.api.ApiClient;
import com.example.personal_finance_manager.models.SavingGoal;
import com.example.personal_finance_manager.models.User;
import com.example.personal_finance_manager.models.Wallet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavingGoalActivity extends AppCompatActivity {

    private RecyclerView rvSavingGoals;
    private SavingGoalAdapter adapter;
    private List<SavingGoal> goalList = new ArrayList<>();
    private FloatingActionButton fabAddGoal;
    
    private ImageView ivUserAvatarHeader;
    private TextView tvHeaderUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saving_goal);

        setupToolbar();
        initViews();
        setupRecyclerView();
        loadUserProfile();
        loadSavingGoals();

        fabAddGoal.setOnClickListener(v -> showGoalDialog(null));
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
        rvSavingGoals = findViewById(R.id.rvSavingGoals);
        fabAddGoal = findViewById(R.id.fabAddGoal);
        ivUserAvatarHeader = findViewById(R.id.ivUserAvatarHeader);
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername);
    }

    private void setupRecyclerView() {
        adapter = new SavingGoalAdapter(goalList, new SavingGoalAdapter.OnGoalClickListener() {
            @Override
            public void onAddMoneyClick(SavingGoal goal) {
                showAddMoneyDialog(goal);
            }

            @Override
            public void onEditClick(SavingGoal goal) {
                showGoalDialog(goal);
            }
        });
        rvSavingGoals.setLayoutManager(new LinearLayoutManager(this));
        rvSavingGoals.setAdapter(adapter);
    }

    private void loadUserProfile() {
        ApiClient.getApiService(this).getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvHeaderUsername.setText(user.getUsername());
                    if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
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

    private void loadSavingGoals() {
        ApiClient.getApiService(this).getSavingGoals().enqueue(new Callback<List<SavingGoal>>() {
            @Override
            public void onResponse(Call<List<SavingGoal>> call, Response<List<SavingGoal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    goalList.clear();
                    goalList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else if (response.code() == 404) {
                    Toast.makeText(SavingGoalActivity.this, "Lỗi 404: Server chưa hỗ trợ API mục tiêu", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<SavingGoal>> call, Throwable t) {
                Toast.makeText(SavingGoalActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGoalDialog(SavingGoal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_saving_goal, null);
        
        EditText etName = view.findViewById(R.id.etGoalName);
        EditText etTargetAmount = view.findViewById(R.id.etTargetAmount);
        EditText etDeadline = view.findViewById(R.id.etDeadline);

        if (goal != null) {
            builder.setTitle("Sửa mục tiêu");
            etName.setText(goal.getName());
            etTargetAmount.setText(String.valueOf(goal.getTargetAmount()));
            etDeadline.setText(goal.getDeadline());
        } else {
            builder.setTitle("Thêm mục tiêu mới");
        }

        etDeadline.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view1, year, month, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
                etDeadline.setText(date);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        builder.setView(view);
        
        if (goal != null) {
            builder.setNeutralButton("Xóa", (dialog, which) -> showDeleteConfirmation(goal.getId()));
        }
        
        builder.setPositiveButton("Lưu", null);
        builder.setNegativeButton("Hủy", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String targetStr = etTargetAmount.getText().toString().trim();
            String deadline = etDeadline.getText().toString().trim();

            if (name.isEmpty() || targetStr.isEmpty() || deadline.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            double target;
            try {
                target = Double.parseDouble(targetStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (goal == null) {
                // Tạo một Map để gửi lên Server thay vì đối tượng SavingGoal
                // Điều này giúp loại bỏ trường 'id' để server tự tạo ID
                SavingGoal newGoal = new SavingGoal();
                newGoal.setId(""); // Để trống ID để Server tự tạo
                newGoal.setName(name);
                newGoal.setTargetAmount(target);
                newGoal.setCurrentAmount(0);
                newGoal.setDeadline(deadline);
                addGoal(newGoal);
            } else {
                goal.setName(name);
                goal.setTargetAmount(target);
                goal.setDeadline(deadline);
                updateGoal(goal.getId(), goal);
            }
            dialog.dismiss();
        });
    }

    private void showAddMoneyDialog(SavingGoal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Gửi tiết kiệm cho: " + goal.getName());
        
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_saving_money, null);
        Spinner spinnerWallet = view.findViewById(R.id.spinnerWallet);
        EditText etAmount = view.findViewById(R.id.etAmount);

        ApiClient.getApiService(this).getWallets().enqueue(new Callback<List<Wallet>>() {
            @Override
            public void onResponse(Call<List<Wallet>> call, Response<List<Wallet>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ArrayAdapter<Wallet> walletAdapter = new ArrayAdapter<>(SavingGoalActivity.this, android.R.layout.simple_spinner_item, response.body());
                    walletAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerWallet.setAdapter(walletAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<Wallet>> call, Throwable t) {}
        });

        builder.setView(view)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    Wallet selectedWallet = (Wallet) spinnerWallet.getSelectedItem();

                    if (amountStr.isEmpty() || selectedWallet == null) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount = Double.parseDouble(amountStr);
                    addMoneyToGoal(goal.getId(), selectedWallet.getId(), amount);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addGoal(SavingGoal goal) {
        ApiClient.getApiService(this).addSavingGoal(goal).enqueue(new Callback<SavingGoal>() {
            @Override
            public void onResponse(Call<SavingGoal> call, Response<SavingGoal> response) {
                if (response.isSuccessful()) {
                    loadSavingGoals();
                    Toast.makeText(SavingGoalActivity.this, "Đã thêm mục tiêu thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = "Lỗi " + response.code();
                    if (response.code() == 404) {
                        errorMsg += ": Đường dẫn API không tồn tại trên Server";
                    }
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (IOException e) { e.printStackTrace(); }
                    Toast.makeText(SavingGoalActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<SavingGoal> call, Throwable t) {
                Toast.makeText(SavingGoalActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateGoal(String id, SavingGoal goal) {
        ApiClient.getApiService(this).updateSavingGoal(id, goal).enqueue(new Callback<SavingGoal>() {
            @Override
            public void onResponse(Call<SavingGoal> call, Response<SavingGoal> response) {
                if (response.isSuccessful()) {
                    loadSavingGoals();
                    Toast.makeText(SavingGoalActivity.this, "Đã cập nhật mục tiêu", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SavingGoalActivity.this, "Lỗi khi cập nhật mục tiêu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SavingGoal> call, Throwable t) {
                Toast.makeText(SavingGoalActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteGoal(String id) {
        ApiClient.getApiService(this).deleteSavingGoal(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    loadSavingGoals();
                    Toast.makeText(SavingGoalActivity.this, "Đã xóa mục tiêu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void addMoneyToGoal(String goalId, String walletId, double amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("walletId", walletId);
        body.put("amount", amount);

        ApiClient.getApiService(this).addMoneyToGoal(goalId, body).enqueue(new Callback<SavingGoal>() {
            @Override
            public void onResponse(Call<SavingGoal> call, Response<SavingGoal> response) {
                if (response.isSuccessful()) {
                    loadSavingGoals();
                    Toast.makeText(SavingGoalActivity.this, "Đã gửi tiết kiệm thành công", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SavingGoalActivity.this, "Lỗi: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SavingGoal> call, Throwable t) {
                Toast.makeText(SavingGoalActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(String id) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa mục tiêu này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteGoal(id))
                .setNegativeButton("Hủy", null)
                .show();
    }
}
