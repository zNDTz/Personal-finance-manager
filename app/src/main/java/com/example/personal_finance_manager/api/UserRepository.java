package com.example.personal_finance_manager.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.personal_finance_manager.models.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;
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
        executorService.execute(() -> {
            User localUser = userDao.getUser();
            if (localUser != null) {
                mainHandler.post(() -> callback.onSuccess(localUser));
            }

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
                    mainHandler.post(() -> callback.onError("Lỗi " + response.code() + ": Cập nhật thất bại"));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void changePassword(String currentPassword, String newPassword, DataCallback<String> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("currentPassword", currentPassword);
        body.put("oldPassword", currentPassword); 
        body.put("newPassword", newPassword);
        body.put("password", newPassword);

        apiService.changePassword(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().get("message");
                    mainHandler.post(() -> callback.onSuccess(message != null ? message : "Đổi mật khẩu thành công"));
                } else {
                    String errorMsg = "Lỗi " + response.code() + ": Xác thực thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            Log.e("API_DEBUG", "Code: " + response.code() + " Body: " + errorStr);
                            
                            if (response.code() == 404) {
                                errorMsg = "Lỗi 404: Không tìm thấy đường dẫn trên Server. Hãy Rebuild app.";
                            } else {
                                Map<String, String> errorData = new Gson().fromJson(errorStr, new TypeToken<Map<String, String>>() {}.getType());
                                if (errorData != null && errorData.containsKey("message")) {
                                    errorMsg = errorData.get("message");
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("API_DEBUG", "Parse error", e);
                    }
                    
                    if (response.code() == 401 || response.code() == 403) {
                        errorMsg = "Lỗi " + response.code() + ": Mật khẩu cũ sai hoặc bạn cần ĐĂNG XUẤT và ĐĂNG NHẬP lại.";
                    }
                    
                    final String finalMsg = errorMsg;
                    mainHandler.post(() -> callback.onError(finalMsg));
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối: " + t.getMessage()));
            }
        });
    }
}
