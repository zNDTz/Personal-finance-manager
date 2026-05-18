package com.example.personal_finance_manager.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.personal_finance_manager.models.Wallet;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletRepository {
    private final ApiService apiService;
    private final WalletDao walletDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public WalletRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.walletDao = AppDatabase.getInstance(context).walletDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void getWallets(DataCallback<List<Wallet>> callback) {
        executorService.execute(() -> {
            List<Wallet> localData = walletDao.getAllWallets();
            mainHandler.post(() -> callback.onSuccess(localData));

            apiService.getWallets().enqueue(new Callback<List<Wallet>>() {
                @Override
                public void onResponse(Call<List<Wallet>> call, Response<List<Wallet>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Wallet> remoteData = response.body();
                        executorService.execute(() -> {
                            walletDao.deleteAll();
                            walletDao.insertAll(remoteData);
                            mainHandler.post(() -> callback.onSuccess(remoteData));
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Không thể lấy dữ liệu từ server"));
                    }
                }

                @Override
                public void onFailure(Call<List<Wallet>> call, Throwable t) {
                    mainHandler.post(() -> callback.onError("Lỗi kết nối: " + t.getMessage()));
                }
            });
        });
    }

    public void addWallet(Wallet wallet, DataCallback<Wallet> callback) {
        apiService.addWallet(wallet).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Wallet saved = response.body();
                    executorService.execute(() -> {
                        walletDao.insert(saved);
                        mainHandler.post(() -> callback.onSuccess(saved));
                    });
                } else {
                    String errorMsg = "Lỗi khi thêm ví";
                    try {
                        if (response.errorBody() != null) {
                            JSONObject jObjError = new JSONObject(response.errorBody().string());
                            errorMsg = jObjError.getString("message");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String finalErrorMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalErrorMsg));
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }

    public void updateWallet(Wallet wallet, DataCallback<Wallet> callback) {
        apiService.updateWallet(wallet.getId(), wallet).enqueue(new Callback<Wallet>() {
            @Override
            public void onResponse(Call<Wallet> call, Response<Wallet> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Wallet updated = response.body();
                    executorService.execute(() -> {
                        walletDao.update(updated);
                        mainHandler.post(() -> callback.onSuccess(updated));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi cập nhật ví"));
                }
            }

            @Override
            public void onFailure(Call<Wallet> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void deleteWallet(String id, DataCallback<Void> callback) {
        apiService.deleteWallet(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executorService.execute(() -> {
                        List<Wallet> all = walletDao.getAllWallets();
                        for (Wallet w : all) {
                            if (w.getId().equals(id)) {
                                walletDao.delete(w);
                                break;
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    });
                } else {
                    String errorMsg = "Không thể xóa ví";
                    try {
                        if (response.errorBody() != null) {
                            JSONObject jObjError = new JSONObject(response.errorBody().string());
                            errorMsg = jObjError.getString("message");
                        }
                    } catch (Exception e) {}
                    String finalErrorMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalErrorMsg));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void transferMoney(String fromWalletId, String toWalletId, double amount, String note, DataCallback<String> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("fromWalletId", fromWalletId);
        body.put("toWalletId", toWalletId);
        body.put("amount", amount);
        body.put("note", note);

        apiService.transferMoney(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess("Chuyển tiền thành công"));
                } else {
                    String errorMsg = "Lỗi khi chuyển tiền";
                    try {
                        if (response.errorBody() != null) {
                            JSONObject jObjError = new JSONObject(response.errorBody().string());
                            errorMsg = jObjError.getString("message");
                        }
                    } catch (Exception e) {}
                    String finalErrorMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalErrorMsg));
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }
}
