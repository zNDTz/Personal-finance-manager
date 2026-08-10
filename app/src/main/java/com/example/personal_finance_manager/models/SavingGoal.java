package com.example.personal_finance_manager.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "saving_goals")
public class SavingGoal {
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("targetAmount")
    private double targetAmount;

    @SerializedName("currentAmount")
    private double currentAmount;

    @SerializedName("deadline")
    private String deadline; // yyyy-MM-dd

    @SerializedName("userId")
    private String userId;

    public SavingGoal(@NonNull String id, String name, double targetAmount, double currentAmount, String deadline, String userId) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.userId = userId;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
