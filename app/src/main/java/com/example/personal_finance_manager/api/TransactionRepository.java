package com.example.personal_finance_manager.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.personal_finance_manager.models.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionRepository {
    private final ApiService apiService;
    private final TransactionDao transactionDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public TransactionRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.transactionDao = AppDatabase.getInstance(context).transactionDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void getTransactions(DataCallback<List<Transaction>> callback) {
        executorService.execute(() -> {
            List<Transaction> localData = transactionDao.getAllTransactions();
            mainHandler.post(() -> callback.onSuccess(localData));

            apiService.getTransactions().enqueue(new Callback<List<Transaction>>() {
                @Override
                public void onResponse(Call<List<Transaction>> call, Response<List<Transaction>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Transaction> remoteData = response.body();
                        executorService.execute(() -> {
                            try {
                                transactionDao.deleteAll();
                                transactionDao.insertAll(remoteData);
                                mainHandler.post(() -> callback.onSuccess(remoteData));
                            } catch (Exception e) {
                                Log.e("TransactionRepo", "Database error: " + e.getMessage());
                            }
                        });
                    } else {
                        mainHandler.post(() -> callback.onError("Lỗi phản hồi từ Server: " + response.code()));
                    }
                }

                @Override
                public void onFailure(Call<List<Transaction>> call, Throwable t) {
                    Log.e("TransactionRepo", "Network/Parsing error: " + t.getMessage());
                    mainHandler.post(() -> callback.onError("Lỗi dữ liệu hoặc kết nối Server."));
                }
            });
        });
    }

    public void addTransaction(Transaction transaction, DataCallback<Transaction> callback) {
        apiService.addTransaction(transaction).enqueue(new Callback<Transaction>() {
            @Override
            public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Transaction saved = response.body();
                    executorService.execute(() -> {
                        transactionDao.insert(saved);
                        mainHandler.post(() -> callback.onSuccess(saved));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi thêm giao dịch"));
                }
            }

            @Override
            public void onFailure(Call<Transaction> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void updateTransaction(Transaction transaction, DataCallback<Transaction> callback) {
        apiService.updateTransaction(transaction.getId(), transaction).enqueue(new Callback<Transaction>() {
            @Override
            public void onResponse(Call<Transaction> call, Response<Transaction> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Transaction updated = response.body();
                    executorService.execute(() -> {
                        transactionDao.update(updated);
                        mainHandler.post(() -> callback.onSuccess(updated));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi cập nhật giao dịch"));
                }
            }

            @Override
            public void onFailure(Call<Transaction> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void deleteTransaction(String id, DataCallback<Void> callback) {
        apiService.deleteTransaction(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executorService.execute(() -> {
                        List<Transaction> all = transactionDao.getAllTransactions();
                        for (Transaction t : all) {
                            if (t.getId().equals(id)) {
                                transactionDao.delete(t);
                                break;
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi xóa"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }
}
