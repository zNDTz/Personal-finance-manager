package com.example.personal_finance_manager.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    private String id;
    
    @SerializedName("amount")
    private double amount;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("date")
    private String date;
    
    @SerializedName("categoryId")
    private String categoryId;
    
    @SerializedName("categoryName")
    private String categoryName;
    
    @SerializedName("type")
    private String type;
    
    @SerializedName("userId")
    private String userId;

    @SerializedName("walletId")
    private String walletId;

    public Transaction() {}

    public Transaction(String id, double amount, String description, String date, String categoryId, String categoryName, String type, String walletId) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.date = date;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
        this.walletId = walletId;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getWalletId() { return walletId; }
    public void setWalletId(String walletId) { this.walletId = walletId; }

    public String getTitle() { return description; }
    public void setTitle(String title) { this.description = title; }
    public String getCategory() { return categoryName; }
    public void setCategory(String category) { this.categoryName = category; }
}
