package com.zendo.apps.utils;

import com.zendo.apps.data.models.User;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SharedPrefManager {
    private static final String PREF_NAME = "ZendoSecurePrefs";
    private static SharedPrefManager instance;
    private SharedPreferences sharedPreferences;

    private SharedPrefManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            // Fallback to regular SharedPreferences if encryption fails (not ideal but avoids crash)
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public static synchronized SharedPrefManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context.getApplicationContext());
        }
        return instance;
    }

    public void saveUserRole(String role) {
        sharedPreferences.edit().putString("user_role", role).apply();
    }

    public String getUserRole() {
        return sharedPreferences.getString("user_role", "user");
    }

    public void saveUserEmail(String email) {
        sharedPreferences.edit().putString("user_email", email).apply();
    }

    public String getUserEmail() {
        return sharedPreferences.getString("user_email", "");
    }

    public void saveUserName(String name) {
        sharedPreferences.edit().putString("user_name", name).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString("user_name", "Người dùng Zendo");
    }

    public void saveSearchHistory(String history) {
        sharedPreferences.edit().putString("search_history", history).apply();
    }

    public String getSearchHistory() {
        return sharedPreferences.getString("search_history", "");
    }

    public void saveUserId(String id) {
        sharedPreferences.edit().putString("user_id", id).apply();
    }

    public String getUserId() {
        return sharedPreferences.getString("user_id", "");
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}


