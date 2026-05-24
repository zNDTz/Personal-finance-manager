package com.example.personal_finance_manager.api;

import android.content.Context;
import com.example.personal_finance_manager.utils.TokenManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // SỬA LẠI: Kết nối tới server local trên máy tính của bạn để test tính năng mới
    // Dùng 10.0.2.2 nếu chạy giả lập Android Studio, hoặc dùng IP máy tính nếu chạy máy thật
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static Retrofit retrofit = null;
    private static Retrofit retrofitWithAuth = null;

    public static ApiService getApiService(Context context) {
        if (retrofitWithAuth == null) {
            TokenManager tokenManager = new TokenManager(context);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder();
                        
                        String token = tokenManager.getToken();
                        if (token != null) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }
                        
                        return chain.proceed(requestBuilder.build());
                    })
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .build();

            retrofitWithAuth = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitWithAuth.create(ApiService.class);
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(90, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
    
    public static void resetClient() {
        retrofit = null;
        retrofitWithAuth = null;
    }
}
