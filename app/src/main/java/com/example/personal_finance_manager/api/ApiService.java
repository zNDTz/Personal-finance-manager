package com.example.personal_finance_manager.api;

import com.example.personal_finance_manager.models.Category;
import com.example.personal_finance_manager.models.Transaction;
import com.example.personal_finance_manager.models.User;
import com.example.personal_finance_manager.models.Wallet;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @POST("api/auth/register")
    Call<Map<String, String>> register(@Body Map<String, String> body);

    @POST("api/auth/login")
    Call<Map<String, String>> login(@Body Map<String, String> body);

    @GET("api/auth/profile")
    Call<User> getProfile();

    @PUT("api/auth/profile")
    Call<User> updateProfile(@Body User user);

    @POST("api/auth/change-password")
    Call<Map<String, String>> changePassword(@Body Map<String, String> body);

    @GET("api/transactions")
    Call<List<Transaction>> getTransactions();

    @POST("api/transactions")
    Call<Transaction> addTransaction(@Body Transaction transaction);

    @PUT("api/transactions/{id}")
    Call<Transaction> updateTransaction(@Path("id") String id, @Body Transaction transaction);

    @DELETE("api/transactions/{id}")
    Call<Void> deleteTransaction(@Path("id") String id);

    @GET("api/categories")
    Call<List<Category>> getCategories();

    @POST("api/categories")
    Call<Category> addCategory(@Body Category category);

    @PUT("api/categories/{id}")
    Call<Category> updateCategory(@Path("id") String id, @Body Category category);

    @DELETE("api/categories/{id}")
    Call<Void> deleteCategory(@Path("id") String id);

    @GET("api/wallets")
    Call<List<Wallet>> getWallets();

    @POST("api/wallets")
    Call<Wallet> addWallet(@Body Wallet wallet);

    @PUT("api/wallets/{id}")
    Call<Wallet> updateWallet(@Path("id") String id, @Body Wallet wallet);

    @DELETE("api/wallets/{id}")
    Call<Void> deleteWallet(@Path("id") String id);

    @POST("api/wallets/transfer")
    Call<Map<String, String>> transferMoney(@Body Map<String, Object> body);
}
