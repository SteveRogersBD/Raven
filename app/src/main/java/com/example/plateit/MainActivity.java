package com.example.plateit;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        com.example.plateit.utils.SessionManager sessionManager = new com.example.plateit.utils.SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            android.content.Intent intent = new android.content.Intent(this, SignInActivity.class);
            intent.setFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize AppBar
        com.example.plateit.utils.AppBarHelper.setup(this, "Home", false);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
                title = "Home";
            } else if (itemId == R.id.navigation_dashboard) {
                selectedFragment = new DashboardFragment();
                title = "Dashboard";
            } else if (itemId == R.id.navigation_pantry) {
                selectedFragment = new PantryFragment();
                title = "Pantry";
            }

            if (selectedFragment != null) {
                com.example.plateit.utils.AppBarHelper.setup(this, title, false);
                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
        // Set default selection
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }
}
