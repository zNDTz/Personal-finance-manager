package com.example.personal_finance_manager.api;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.personal_finance_manager.models.Wallet;

import java.util.List;

@Dao
public interface WalletDao {
    @Query("SELECT * FROM wallets")
    List<Wallet> getAllWallets();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Wallet> wallets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Wallet wallet);

    @Update
    void update(Wallet wallet);

    @Delete
    void delete(Wallet wallet);

    @Query("DELETE FROM wallets")
    void deleteAll();
}
