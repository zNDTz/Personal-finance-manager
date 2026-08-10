package com.example.personal_finance_manager.api;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.personal_finance_manager.models.Category;
import com.example.personal_finance_manager.models.SavingGoal;
import com.example.personal_finance_manager.models.Transaction;
import com.example.personal_finance_manager.models.User;
import com.example.personal_finance_manager.models.Wallet;

@Database(entities = {Transaction.class, Category.class, User.class, Wallet.class, SavingGoal.class}, version = 10)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();
    public abstract CategoryDao categoryDao();
    public abstract UserDao userDao();
    public abstract WalletDao walletDao();
    public abstract SavingGoalDao savingGoalDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "personal_finance_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
