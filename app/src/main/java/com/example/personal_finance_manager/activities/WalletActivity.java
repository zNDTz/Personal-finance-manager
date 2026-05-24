package com.example.personal_finance_manager.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.adapters.WalletAdapter;
import com.example.personal_finance_manager.api.WalletRepository;
import com.example.personal_finance_manager.models.Wallet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalletActivity extends AppCompatActivity {

    private RecyclerView rvWallets;
    private WalletAdapter adapter;
    private List<Wallet> walletList = new ArrayList<>();
    private FloatingActionButton fabAddWallet;
    private WalletRepository walletRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        walletRepository = new WalletRepository(this);

        setupToolbar();
        initViews();
        setupRecyclerView();
        loadWallets();

        fabAddWallet.setOnClickListener(v -> showWalletDialog(null));
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
        rvWallets = findViewById(R.id.rvWallets);
        fabAddWallet = findViewById(R.id.fabAddWallet);
    }

    private void setupRecyclerView() {
        adapter = new WalletAdapter(walletList, new WalletAdapter.OnWalletClickListener() {
            @Override
            public void onEditClick(Wallet wallet) {
                showWalletDialog(wallet);
            }

            @Override
            public void onItemClick(Wallet wallet) {
                Toast.makeText(WalletActivity.this, "Bạn đã chọn: " + wallet.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        rvWallets.setLayoutManager(new LinearLayoutManager(this));
        rvWallets.setAdapter(adapter);
    }

    private void loadWallets() {
        walletRepository.getWallets(new WalletRepository.DataCallback<List<Wallet>>() {
            @Override
            public void onSuccess(List<Wallet> data) {
                walletList.clear();
                walletList.addAll(data);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(WalletActivity.this, "Không thể tải danh sách ví: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wallet, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_transfer) {
            if (walletList.size() < 2) {
                Toast.makeText(this, "Cần ít nhất 2 ví để chuyển tiền", Toast.LENGTH_SHORT).show();
            } else {
                showTransferDialog();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTransferDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_transfer, null);
        Spinner spinnerFrom = view.findViewById(R.id.spinnerFromWallet);
        Spinner spinnerTo = view.findViewById(R.id.spinnerToWallet);
        EditText etAmount = view.findViewById(R.id.etTransferAmount);
        EditText etNote = view.findViewById(R.id.etTransferNote);

        List<String> walletNames = new ArrayList<>();
        for (Wallet w : walletList) walletNames.add(w.getName());

        ArrayAdapter<String> walletSpinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, walletNames);
        walletSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spinnerFrom.setAdapter(walletSpinnerAdapter);
        spinnerTo.setAdapter(walletSpinnerAdapter);
        
        spinnerTo.setSelection(1);

        builder.setView(view)
                .setTitle("Chuyển tiền giữa các ví")
                .setPositiveButton("Chuyển", (dialog, which) -> {
                    int fromPos = spinnerFrom.getSelectedItemPosition();
                    int toPos = spinnerTo.getSelectedItemPosition();
                    
                    if (fromPos == toPos) {
                        Toast.makeText(this, "Ví gửi và ví nhận phải khác nhau", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String amountStr = etAmount.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double amount = Double.parseDouble(amountStr);
                    String fromId = walletList.get(fromPos).getId();
                    String toId = walletList.get(toPos).getId();
                    String note = etNote.getText().toString().trim();

                    walletRepository.transferMoney(fromId, toId, amount, note, new WalletRepository.DataCallback<String>() {
                        @Override
                        public void onSuccess(String data) {
                            Toast.makeText(WalletActivity.this, "Chuyển tiền thành công!", Toast.LENGTH_SHORT).show();
                            loadWallets();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(WalletActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showWalletDialog(Wallet wallet) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_wallet, null);
        EditText etName = view.findViewById(R.id.etWalletName);
        EditText etBalance = view.findViewById(R.id.etWalletBalance);

        if (wallet != null) {
            builder.setTitle("Sửa ví");
            etName.setText(wallet.getName());
            etBalance.setText(String.format(Locale.US, "%.0f", wallet.getBalance()));
            
            builder.setNeutralButton("Xóa", (dialog, which) -> deleteWallet(wallet.getId()));
        } else {
            builder.setTitle("Thêm ví mới");
            etBalance.setText("0");
        }

        builder.setView(view)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String balanceStr = etBalance.getText().toString().trim();
                    
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Tên ví không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double balance;
                    try {
                        balance = balanceStr.isEmpty() ? 0 : Double.parseDouble(balanceStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Số dư không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (wallet == null) {
                        String tempId = "temp_" + System.currentTimeMillis();
                        Wallet newWallet = new Wallet(tempId, name, balance, "");
                        addWallet(newWallet);
                    } else {
                        wallet.setName(name);
                        wallet.setBalance(balance);
                        updateWallet(wallet);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addWallet(Wallet wallet) {
        walletRepository.addWallet(wallet, new WalletRepository.DataCallback<Wallet>() {
            @Override
            public void onSuccess(Wallet data) {
                Toast.makeText(WalletActivity.this, "Đã thêm ví thành công!", Toast.LENGTH_SHORT).show();
                loadWallets();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(WalletActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWallet(Wallet wallet) {
        walletRepository.updateWallet(wallet, new WalletRepository.DataCallback<Wallet>() {
            @Override
            public void onSuccess(Wallet data) {
                Toast.makeText(WalletActivity.this, "Đã cập nhật ví!", Toast.LENGTH_SHORT).show();
                loadWallets();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(WalletActivity.this, "Lỗi khi cập nhật: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteWallet(String id) {
        walletRepository.deleteWallet(id, new WalletRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(WalletActivity.this, "Đã xóa ví!", Toast.LENGTH_SHORT).show();
                loadWallets();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(WalletActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
