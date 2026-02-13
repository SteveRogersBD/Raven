package com.example.plateit;

import android.app.Application;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesConfiguration;

public class PlateItApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize RevenueCat
        // TODO: Replace with actual API Key from RevenueCat Dashboard
        Purchases.setLogLevel(com.revenuecat.purchases.LogLevel.DEBUG);
        Purchases.configure(new PurchasesConfiguration.Builder(this, "goog_tAzkQqZivDMnTsyVCqqoRIeaYPI").build());
    }
}
