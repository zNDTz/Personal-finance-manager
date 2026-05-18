package com.example.personal_finance_manager.api;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.personal_finance_manager.models.User;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    User getUser();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Update
    void update(User user);

    @Query("DELETE FROM users")
    void deleteUser();
}
