package com.example.personal_finance_manager.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.personal_finance_manager.models.Category;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryRepository {
    private final ApiService apiService;
    private final CategoryDao categoryDao;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public CategoryRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.categoryDao = AppDatabase.getInstance(context).categoryDao();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    public void getCategories(DataCallback<List<Category>> callback) {
        executorService.execute(() -> {
            List<Category> localData = categoryDao.getAllCategories();
            mainHandler.post(() -> callback.onSuccess(localData));

            apiService.getCategories().enqueue(new Callback<List<Category>>() {
                @Override
                public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Category> remoteData = response.body();
                        executorService.execute(() -> {
                            categoryDao.deleteAll();
                            categoryDao.insertAll(remoteData);
                            mainHandler.post(() -> callback.onSuccess(remoteData));
                        });
                    }
                }

                @Override
                public void onFailure(Call<List<Category>> call, Throwable t) {
                    mainHandler.post(() -> callback.onError("Sử dụng danh mục offline."));
                }
            });
        });
    }

    public void addCategory(Category category, DataCallback<Category> callback) {
        apiService.addCategory(category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Category saved = response.body();
                    executorService.execute(() -> {
                        categoryDao.insert(saved);
                        mainHandler.post(() -> callback.onSuccess(saved));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi thêm danh mục"));
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void updateCategory(Category category, DataCallback<Category> callback) {
        apiService.updateCategory(category.getId(), category).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Category updated = response.body();
                    executorService.execute(() -> {
                        categoryDao.update(updated);
                        mainHandler.post(() -> callback.onSuccess(updated));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi cập nhật danh mục"));
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }

    public void deleteCategory(String id, DataCallback<Void> callback) {
        apiService.deleteCategory(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    executorService.execute(() -> {
                        List<Category> all = categoryDao.getAllCategories();
                        for (Category c : all) {
                            if (c.getId().equals(id)) {
                                categoryDao.delete(c);
                                break;
                            }
                        }
                        mainHandler.post(() -> callback.onSuccess(null));
                    });
                } else {
                    mainHandler.post(() -> callback.onError("Lỗi khi xóa danh mục"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                mainHandler.post(() -> callback.onError("Lỗi kết nối"));
            }
        });
    }
}
