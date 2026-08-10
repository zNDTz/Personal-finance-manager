package com.example.personal_finance_manager.api;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.example.personal_finance_manager.models.SavingGoal;
import java.util.List;

@Dao
public interface SavingGoalDao {
    @Query("SELECT * FROM saving_goals WHERE userId = :userId")
    List<SavingGoal> getGoalsByUserId(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SavingGoal goal);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SavingGoal> goals);

    @Update
    void update(SavingGoal goal);

    @Delete
    void delete(SavingGoal goal);

    @Query("DELETE FROM saving_goals")
    void deleteAll();
}
