package com.example.personal_finance_manager;

import android.app.Application;
import com.example.personal_finance_manager.utils.ThemeManager;

public class PersonalFinanceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        new ThemeManager(this).applySavedTheme();
    }
}
