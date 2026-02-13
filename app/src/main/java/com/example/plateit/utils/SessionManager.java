package com.example.plateit.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "PlateItPref";
    private static final String IS_LOGGED_IN = "IsLoggedIn";
    private static final String ONBOARDING_COMPLETED = "OnboardingCompleted";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_LAST_VIDEO_FETCH = "lastVideoFetchTime";
    private static final String KEY_LAST_PANTRY_FETCH = "lastPantryFetchTime";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context _context;

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String userId, String email, String username, String fullName) {
        editor.putBoolean(IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGGED_IN, false) && getUserId() != null;
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public void logout() {
        editor.clear();
        editor.commit();
    }

    public String getFullName() {
        return pref.getString(KEY_FULL_NAME, null);
    }

    public void setFullName(String fullName) {
        editor.putString(KEY_FULL_NAME, fullName);
        editor.commit();
    }

    public void setOnboardingCompleted() {
        editor.putBoolean(ONBOARDING_COMPLETED, true);
        editor.commit();
    }

    public boolean isOnboardingCompleted() {
        return pref.getBoolean(ONBOARDING_COMPLETED, false);
    }

    public void setLastVideoFetchTime(long timestamp) {
        editor.putLong(KEY_LAST_VIDEO_FETCH, timestamp);
        editor.commit();
    }

    public long getLastVideoFetchTime() {
        return pref != null ? pref.getLong(KEY_LAST_VIDEO_FETCH, 0) : 0;
    }

    public void setLastPantryFetchTime(long timestamp) {
        if (pref != null) {
            pref.edit().putLong(KEY_LAST_PANTRY_FETCH, timestamp).apply();
        }
    }

    public long getLastPantryFetchTime() {
        return pref != null ? pref.getLong(KEY_LAST_PANTRY_FETCH, 0) : 0;
    }

    // --- Token System ---
    private static final String KEY_TOKEN_BALANCE = "tokenBalance";
    private static final String KEY_LAST_TOKEN_RESET = "lastTokenResetTime";
    private static final String KEY_HAS_USED_FREE_CHAT = "hasUsedFreeChat";
    private static final String KEY_IS_PRO = "isProCached";

    public void setTokenBalance(int balance) {
        editor.putInt(KEY_TOKEN_BALANCE, balance);
        editor.commit();
    }

    public int getTokenBalance() {
        // Default to 10 tokens for new users
        return pref.getInt(KEY_TOKEN_BALANCE, 10);
    }

    public void setLastTokenResetTime(long timestamp) {
        editor.putLong(KEY_LAST_TOKEN_RESET, timestamp);
        editor.commit();
    }

    public long getLastTokenResetTime() {
        return pref.getLong(KEY_LAST_TOKEN_RESET, 0);
    }

    public void setHasUsedFreeChat(boolean used) {
        editor.putBoolean(KEY_HAS_USED_FREE_CHAT, used);
        editor.commit();
    }

    public boolean hasUsedFreeChat() {
        return pref.getBoolean(KEY_HAS_USED_FREE_CHAT, false);
    }

    public void setProCached(boolean isPro) {
        editor.putBoolean(KEY_IS_PRO, isPro);
        editor.commit();
    }

    public boolean isProCached() {
        return pref.getBoolean(KEY_IS_PRO, false);
    }
}
