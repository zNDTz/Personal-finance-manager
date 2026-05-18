package com.example.personal_finance_manager.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    private String id;
    
    private String name;
    private String type; // "INCOME" or "EXPENSE"
    private String userId;

    @SerializedName("budget")
    private double budget; // Ngân sách hàng tháng cho danh mục này

    public Category(@NonNull String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.budget = 0;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    @Override
    public String toString() {
        return name;
    }
}
