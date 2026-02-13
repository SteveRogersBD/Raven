package com.example.plateit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private Button btnSignUp;
    private TextView tvSignIn;
    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Standardized AppBar Setup
        com.example.plateit.utils.AppBarHelper.setup(this, "Sign Up", true);

        com.example.plateit.utils.SessionManager sessionManager = new com.example.plateit.utils.SessionManager(
                SignUpActivity.this);

        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);
        progressBar = findViewById(R.id.progressBar);

        btnSignUp.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString();
            String username = etUsername.getText().toString();
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                return;
            }

            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating Account...");
            progressBar.setVisibility(android.view.View.VISIBLE);

            com.example.plateit.requests.SignUpRequest request = new com.example.plateit.requests.SignUpRequest(
                    fullName, username, email, password);

            com.example.plateit.api.RetrofitClient.getService().signup(request)
                    .enqueue(new retrofit2.Callback<com.example.plateit.responses.AuthResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.example.plateit.responses.AuthResponse> call,
                                retrofit2.Response<com.example.plateit.responses.AuthResponse> response) {
                            progressBar.setVisibility(android.view.View.GONE);
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign Up");

                            if (response.isSuccessful() && response.body() != null) {
                                // Save session
                                sessionManager.createLoginSession(
                                        response.body().getUserId(),
                                        response.body().getEmail(),
                                        response.body().getUsername(),
                                        response.body().getFullName());

                                // Proceed to Preferences Activity
                                Intent intent = new Intent(SignUpActivity.this, PreferencesActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                // Sign Up Failed silently or handled by other UI elements if any
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.example.plateit.responses.AuthResponse> call,
                                Throwable t) {
                            progressBar.setVisibility(android.view.View.GONE);
                            btnSignUp.setEnabled(true);
                            btnSignUp.setText("Sign Up");
                        }
                    });
        });

        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
