package com.example.plateit.utils;

import android.content.Context;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.PurchasesError;
import androidx.annotation.NonNull;

public class TokenManager {
    private static TokenManager instance;
    private SessionManager sessionManager;

    // Entitlement ID from RevenueCat Dashboard
    private static final String ENTITLEMENT_ID = "PlateIt Pro";

    private TokenManager(Context context) {
        sessionManager = new SessionManager(context);
    }

    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }

    public int getTokens() {
        checkMonthlyReset();
        return sessionManager.getTokenBalance();
    }

    public boolean canAfford(int cost) {
        // Double check reset before verifying affordability
        checkMonthlyReset();
        return getTokens() >= cost;
    }

    public boolean useTokens(int cost) {
        if (canAfford(cost)) {
            int newBalance = getTokens() - cost;
            sessionManager.setTokenBalance(newBalance);
            return true;
        }
        return false;
    }

    public void addTokens(int amount) {
        sessionManager.setTokenBalance(getTokens() + amount);
    }

    private void checkMonthlyReset() {
        long lastReset = sessionManager.getLastTokenResetTime();
        long currentTime = System.currentTimeMillis();
        // 30 days in milliseconds
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;

        if (currentTime - lastReset > thirtyDays) {
            // It's time to reset!
            // We need to check if user is pro to determine amount
            // Since this is async, we do a "best effort" check or default to free amount
            // first
            // followed by an async update.
            // For now, let's reset to free amount (10) and then trigger a Re-Sync
            resetTokens(10);

            // Async check for Pro status to upgrade to 50
            Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
                @Override
                public void onReceived(@NonNull CustomerInfo customerInfo) {
                    if (customerInfo.getEntitlements().get(ENTITLEMENT_ID) != null &&
                            customerInfo.getEntitlements().get(ENTITLEMENT_ID).isActive()) {
                        // User is PRO, upgrade balance if they were just reset
                        sessionManager.setTokenBalance(50);
                        sessionManager.setProCached(true);
                        sessionManager.setHasUsedFreeChat(false);
                    } else {
                        sessionManager.setProCached(false);
                    }
                }

                @Override
                public void onError(@NonNull PurchasesError error) {
                    // Start with 10 (already set)
                }
            });
        }
    }

    private void resetTokens(int amount) {
        sessionManager.setTokenBalance(amount);
        sessionManager.setLastTokenResetTime(System.currentTimeMillis());
        // Also reset chat usage
        sessionManager.setHasUsedFreeChat(false);
    }

    // Helper to check pro status asynchronously with a callback
    public void isPro(final ProStatusCallback callback) {
        Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo info) {
                boolean isPro = (info.getEntitlements().get(ENTITLEMENT_ID) != null
                        && info.getEntitlements().get(ENTITLEMENT_ID).isActive()) ||
                        (info.getEntitlements().get("pro_access") != null
                                && info.getEntitlements().get("pro_access").isActive());

                // Fallback: If ANY entitlement is active, count it as Pro for the hackathon
                if (!isPro && !info.getEntitlements().getActive().isEmpty()) {
                    isPro = true;
                }

                sessionManager.setProCached(isPro);
                if (isPro) {
                    sessionManager.setHasUsedFreeChat(false);
                }
                callback.onResult(isPro);
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                callback.onResult(false);
            }
        });
    }

    // Force refresh status and set tokens to Pro amount if applicable
    public void forceRefreshProStatus(@androidx.annotation.Nullable final Runnable onComplete) {
        Purchases.getSharedInstance().getCustomerInfo(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo info) {
                boolean isPro = (info.getEntitlements().get(ENTITLEMENT_ID) != null
                        && info.getEntitlements().get(ENTITLEMENT_ID).isActive()) ||
                        (info.getEntitlements().get("pro_access") != null
                                && info.getEntitlements().get("pro_access").isActive());

                if (isPro) {
                    // Upgrade logic: If user is Pro but has default tokens, bump them to 50
                    // And reset the cycle so checkMonthlyReset doesn't overwrite it
                    sessionManager.setProCached(true);
                    sessionManager.setHasUsedFreeChat(false);
                    if (sessionManager.getTokenBalance() < 50) {
                        sessionManager.setTokenBalance(50);
                        sessionManager.setLastTokenResetTime(System.currentTimeMillis());
                    }
                } else {
                    sessionManager.setProCached(false);
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                // Ignore error
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    public boolean isPro() {
        return sessionManager.isProCached() || sessionManager.getTokenBalance() >= 50;
    }

    public interface ProStatusCallback {
        void onResult(boolean isPro);
    }
}
