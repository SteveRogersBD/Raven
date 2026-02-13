package com.example.plateit;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plateit.utils.TokenManager;
import com.example.plateit.utils.LoadingDialog;
import com.google.android.material.button.MaterialButton;
import com.revenuecat.purchases.CustomerInfo;
import com.revenuecat.purchases.Offerings;
import com.revenuecat.purchases.Package;
import com.revenuecat.purchases.Purchases;
import com.revenuecat.purchases.PurchasesError;
import com.revenuecat.purchases.interfaces.PurchaseCallback;
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback;
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback;
import com.revenuecat.purchases.models.StoreProduct;

public class PaywallActivity extends AppCompatActivity {

    private RadioGroup rgSubscription;
    private RadioButton rbMonthly;
    private MaterialButton btnContinue;
    private TextView tvRestore;
    private ImageButton btnClose;
    private LoadingDialog loadingDialog;

    private Package annualPackage;
    private Package monthlyPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paywall);

        // Views
        rgSubscription = findViewById(R.id.rgSubscription);
        rbMonthly = findViewById(R.id.rbMonthly);
        btnContinue = findViewById(R.id.btnContinue);
        tvRestore = findViewById(R.id.tvRestore);
        btnClose = findViewById(R.id.btnClose);

        loadingDialog = new LoadingDialog(this);

        // Clear placeholders so we know if data is loading correctly
        rbMonthly.setText("Loading Monthly price...");

        // Listeners
        btnClose.setOnClickListener(v -> finish());

        tvRestore.setOnClickListener(v -> restorePurchases());

        btnContinue.setOnClickListener(v -> {
            if (monthlyPackage != null) {
                purchasePackage(monthlyPackage);
            } else {
                // Fetching plan details...
            }
        });

        // Fetch Offerings
        fetchOfferings();
    }

    private void fetchOfferings() {
        loadingDialog.startLoadingDialog("Loading prices...");
        Purchases.getSharedInstance().getOfferings(new ReceiveOfferingsCallback() {
            @Override
            public void onReceived(@NonNull Offerings offerings) {
                loadingDialog.dismissDialog();
                com.revenuecat.purchases.Offering offering = offerings.getCurrent();

                // Fallback: If no 'Current' offering, try to grab any offering
                if (offering == null && !offerings.getAll().isEmpty()) {
                    offering = offerings.getAll().values().iterator().next();
                }

                if (offering != null) {
                    monthlyPackage = offering.getMonthly();

                    if (monthlyPackage != null) {
                        StoreProduct product = monthlyPackage.getProduct();
                        rbMonthly.setText("Monthly\nFull access for just " + product.getPrice().getFormatted() + "/mo");
                        rbMonthly.setVisibility(View.VISIBLE);
                    } else if (!offering.getAvailablePackages().isEmpty()) {
                        // Fallback: Use the first available package
                        monthlyPackage = offering.getAvailablePackages().get(0);
                        StoreProduct product = monthlyPackage.getProduct();
                        rbMonthly.setText(product.getTitle() + "\n" + product.getPrice().getFormatted());
                        rbMonthly.setVisibility(View.VISIBLE);
                    } else {
                        rbMonthly.setVisibility(View.GONE);
                    }
                } else {
                    android.util.Log.e("Paywall", "No offerings found at all in RevenueCat.");
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                loadingDialog.dismissDialog();
            }
        });
    }

    private void purchasePackage(Package packageToPurchase) {
        loadingDialog.startLoadingDialog("Processing...");
        Purchases.getSharedInstance().purchasePackage(this, packageToPurchase, new PurchaseCallback() {
            @Override
            public void onCompleted(@NonNull com.revenuecat.purchases.models.StoreTransaction storeTransaction,
                    @NonNull CustomerInfo customerInfo) {
                loadingDialog.dismissDialog();

                if (TokenManager.getInstance(PaywallActivity.this).checkIfPro(customerInfo)) {
                    // Sync immediately with the info we have
                    TokenManager.getInstance(PaywallActivity.this).syncWithCustomerInfo(customerInfo);

                    // Unlock Pro Features
                    TokenManager.getInstance(PaywallActivity.this).forceRefreshProStatus(() -> {
                        runOnUiThread(() -> {
                            finish();
                        });
                    });
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error, boolean userCancelled) {
                loadingDialog.dismissDialog();
                if (!userCancelled) {
                    // Purchase failed
                }
            }
        });
    }

    private void restorePurchases() {
        loadingDialog.startLoadingDialog("Restoring...");
        Purchases.getSharedInstance().restorePurchases(new ReceiveCustomerInfoCallback() {
            @Override
            public void onReceived(@NonNull CustomerInfo customerInfo) {
                loadingDialog.dismissDialog();
                if (TokenManager.getInstance(PaywallActivity.this).checkIfPro(customerInfo)) {
                    // Sync immediately
                    TokenManager.getInstance(PaywallActivity.this).syncWithCustomerInfo(customerInfo);

                    TokenManager.getInstance(PaywallActivity.this).forceRefreshProStatus(() -> {
                        runOnUiThread(() -> {
                            finish();
                        });
                    });
                } else {
                    // No active subscriptions found
                }
            }

            @Override
            public void onError(@NonNull PurchasesError error) {
                loadingDialog.dismissDialog();
            }
        });
    }
}
