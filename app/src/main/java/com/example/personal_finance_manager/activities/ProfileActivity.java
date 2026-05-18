package com.example.personal_finance_manager.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.api.AppDatabase;
import com.example.personal_finance_manager.api.UserRepository;
import com.example.personal_finance_manager.models.User;
import com.example.personal_finance_manager.utils.ThemeManager;
import com.example.personal_finance_manager.utils.TokenManager;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView ivAvatar;
    private TextView tvChangeAvatar;
    private TextInputEditText etUsername, etEmail;
    private TextInputLayout tilUsername, tilEmail;
    private Button btnUpdateProfile, btnLogout, btnManageWallets;
    private User currentUser;
    private TokenManager tokenManager;
    private ThemeManager themeManager;
    private String encodedImage;
    private UserRepository userRepository;
    private MaterialButtonToggleGroup toggleTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tokenManager = new TokenManager(this);
        themeManager = new ThemeManager(this);
        userRepository = new UserRepository(this);
        
        setupToolbar();
        initViews();
        loadProfile();
        setupThemeToggle();

        tvChangeAvatar.setOnClickListener(v -> openGallery());
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnLogout.setOnClickListener(v -> logout());
        btnManageWallets.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, WalletActivity.class));
        });
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
        ivAvatar = findViewById(R.id.ivAvatar);
        tvChangeAvatar = findViewById(R.id.tvChangeAvatar);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        tilUsername = findViewById(R.id.tilUsername);
        tilEmail = findViewById(R.id.tilEmail);
        btnUpdateProfile = findViewById(R.id.btnUpdateProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnManageWallets = findViewById(R.id.btnManageWallets);
        toggleTheme = findViewById(R.id.toggleTheme);
    }

    private void setupThemeToggle() {
        int currentMode = themeManager.getSavedThemeMode();
        if (currentMode == ThemeManager.THEME_LIGHT) {
            toggleTheme.check(R.id.btnThemeLight);
        } else if (currentMode == ThemeManager.THEME_DARK) {
            toggleTheme.check(R.id.btnThemeDark);
        } else {
            toggleTheme.check(R.id.btnThemeSystem);
        }

        toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int mode;
                if (checkedId == R.id.btnThemeLight) {
                    mode = ThemeManager.THEME_LIGHT;
                } else if (checkedId == R.id.btnThemeDark) {
                    mode = ThemeManager.THEME_DARK;
                } else {
                    mode = ThemeManager.THEME_SYSTEM;
                }
                themeManager.setAndApplyTheme(mode);
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh chân dung"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ivAvatar.setImageBitmap(bitmap);
                encodedImage = encodeImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void loadProfile() {
        userRepository.getUserProfile(new UserRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                etUsername.setText(user.getUsername());
                etEmail.setText(user.getEmail());
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    try {
                        byte[] decodedString = Base64.decode(user.getAvatar(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivAvatar.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidEmail(CharSequence target) {
        return (target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    private void updateProfile() {
        if (currentUser == null) return;
        
        String newUsername = etUsername.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        // Reset errors
        tilUsername.setError(null);
        tilEmail.setError(null);

        if (newUsername.isEmpty()) {
            tilUsername.setError("Tên không được để trống");
            return;
        }

        if (newEmail.isEmpty()) {
            tilEmail.setError("Email không được để trống");
            return;
        }

        if (!isValidEmail(newEmail)) {
            tilEmail.setError("Định dạng email không hợp lệ");
            return;
        }

        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);

        if (encodedImage != null) {
            currentUser.setAvatar(encodedImage);
        }

        userRepository.updateProfile(currentUser, new UserRepository.DataCallback<User>() {
            @Override
            public void onSuccess(User data) {
                Toast.makeText(ProfileActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        tokenManager.clearToken();
        
        // Xóa sạch database cục bộ khi đăng xuất để bảo mật dữ liệu
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).clearAllTables();
        });

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
