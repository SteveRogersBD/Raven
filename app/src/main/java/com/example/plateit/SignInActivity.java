package com.example.plateit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plateit.utils.SessionManager;
import android.widget.Button;
import com.google.android.material.textfield.TextInputEditText;

public class SignInActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnSignIn;
    private TextView tvSignUp;
    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Standardized AppBar Setup
        com.example.plateit.utils.AppBarHelper.setup(this, "Sign In", false);

        // Check for existing session
        SessionManager sessionManager = new SessionManager(
                SignInActivity.this);
        if (sessionManager.isLoggedIn()) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        progressBar = findViewById(R.id.progressBar);

        btnSignIn.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                return;
            }

            btnSignIn.setEnabled(false);
            btnSignIn.setText("Signing In...");
            progressBar.setVisibility(android.view.View.VISIBLE);

            com.example.plateit.requests.SignInRequest request = new com.example.plateit.requests.SignInRequest(email,
                    password);

            com.example.plateit.api.RetrofitClient.getService().signin(request)
                    .enqueue(new retrofit2.Callback<com.example.plateit.responses.AuthResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.example.plateit.responses.AuthResponse> call,
                                retrofit2.Response<com.example.plateit.responses.AuthResponse> response) {
                            progressBar.setVisibility(android.view.View.GONE);
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Sign In");

                            if (response.isSuccessful() && response.body() != null) {
                                // Save session
                                sessionManager.createLoginSession(
                                        response.body().getUserId(),
                                        response.body().getEmail(),
                                        response.body().getUsername(),
                                        response.body().getFullName());

                                // Proceed to Preferences Activity
                                Intent intent = new Intent(SignInActivity.this, PreferencesActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                // Sign In Failed silently
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.example.plateit.responses.AuthResponse> call,
                                Throwable t) {
                            progressBar.setVisibility(android.view.View.GONE);
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Sign In");
                        }
                    });
        });

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
