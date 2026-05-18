package com.example.personal_finance_manager.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.personal_finance_manager.models.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;
    private final UserDao userDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public UserRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.userDao = AppDatabase.getInstance(context).userDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void getUserProfile(DataCallback<User> callback) {
        // 1. Lấy từ Local Database ngay lập tức
        executorService.execute(() -> {
            User localUser = userDao.getUser();
            if (localUser != null) {
                mainHandler.post(() -> callback.onSuccess(localUser));
            }

            // 2. Đồng bộ từ Server
            apiService.getProfile().enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        User remoteUser = response.body();
                        executorService.execute(() -> {
                            userDao.deleteUser();
                            userDao.insert(remoteUser);
                            mainHandler.post(() -> callback.onSuccess(remoteUser));
                        });
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    mainHandler.post(() -> callback.onError("Không thể kết nối Server"));
                }
            });
        });
    }

    public void updateProfile(User user, DataCallback<User> callback) {
        apiService.updateProfile(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User updatedUser = response.body();
                    executorService.execute(() -> {
                        userDao.insert(updatedUser);
                        mainHandler.post(() -> callback.onSuccess(updatedUser));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi cập nhật hồ sơ"));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }
}
