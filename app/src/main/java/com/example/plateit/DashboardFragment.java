package com.example.plateit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plateit.adapters.CookbookAdapter;
import com.example.plateit.api.RetrofitClient;
import com.example.plateit.responses.CookbookEntry;
import com.example.plateit.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private RecyclerView rvCookbook;
    private CookbookAdapter adapter;
    private TextView tvEmpty;
    private SessionManager sessionManager;

    // Sessions UI
    private RecyclerView rvSessions;
    private TextView tvSessionsHeader;
    private com.example.plateit.adapters.CookingSessionAdapter sessionAdapter;
    private List<CookbookEntry> myCookbook = new ArrayList<>();

    // Active Card UI
    private androidx.cardview.widget.CardView cvActiveSession;
    private TextView tvActiveTitle, tvActiveStep;
    private Button btnResume;

    // Stats UI
    private com.github.mikephil.charting.charts.PieChart chartSessions;
    private TextView tvStatRecipes, tvStatDays, tvStatSessions;
    private TextView tvChefName;

    public DashboardFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        sessionManager = new SessionManager(getContext());

        // Profile Section
        ImageView btnEdit = view.findViewById(R.id.btnEditProfile);
        tvChefName = view.findViewById(R.id.tvChefName);
        setupChefName();

        btnEdit.setOnClickListener(v -> showEditProfileDialog(tvChefName));

        // Stats UI
        chartSessions = view.findViewById(R.id.chartSessions);
        tvStatRecipes = view.findViewById(R.id.tvStatRecipes);
        tvStatDays = view.findViewById(R.id.tvStatDays);
        tvStatSessions = view.findViewById(R.id.tvStatSessions);
        setupPieChart();

        // Cookbook Section
        rvCookbook = view.findViewById(R.id.rvCookbook);
        tvEmpty = view.findViewById(R.id.tvEmptyCookbook);

        rvCookbook.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        adapter = new CookbookAdapter(new ArrayList<>(), this::openRecipe, this::deleteRecipe);
        rvCookbook.setAdapter(adapter);

        // Sessions Section (History)
        rvSessions = view.findViewById(R.id.rvSessions);
        tvSessionsHeader = view.findViewById(R.id.tvSessionsHeader);
        rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSessions.setNestedScrollingEnabled(false);
        sessionAdapter = new com.example.plateit.adapters.CookingSessionAdapter(
                new ArrayList<>(), new ArrayList<>(), this::onResumeSession);
        rvSessions.setAdapter(sessionAdapter);

        // Active Card Section
        cvActiveSession = view.findViewById(R.id.cvActiveSession);
        tvActiveTitle = view.findViewById(R.id.tvActiveRecipeTitle);
        tvActiveStep = view.findViewById(R.id.tvActiveStep);
        btnResume = view.findViewById(R.id.btnResume);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchCookbook();
        fetchActiveSession();
        fetchHistory();
        fetchUserStats();
        setupChefName(); // Refresh in case it changed
    }

    private void setupChefName() {
        String fullName = sessionManager.getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            String firstName = fullName.split(" ")[0];
            tvChefName.setText("Chef " + firstName);
        } else {
            // Fallback: fetch from API
            tvChefName.setText("Chef");
            fetchUserProfile();
        }
    }

    private void fetchUserProfile() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        RetrofitClient.getAgentService().getUserProfile(userId)
                .enqueue(new Callback<com.example.plateit.responses.AuthResponse>() {
                    @Override
                    public void onResponse(Call<com.example.plateit.responses.AuthResponse> call,
                            Response<com.example.plateit.responses.AuthResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String fullName = response.body().getFullName();
                            if (fullName != null && !fullName.isEmpty()) {
                                sessionManager.setFullName(fullName);
                                String firstName = fullName.split(" ")[0];
                                if (tvChefName != null) {
                                    tvChefName.setText("Chef " + firstName);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.plateit.responses.AuthResponse> call, Throwable t) {
                        // Fail silently
                    }
                });
    }

    private void setupPieChart() {
        chartSessions.setUsePercentValues(true);
        chartSessions.getDescription().setEnabled(false);
        chartSessions.setExtraOffsets(5, 10, 5, 5);
        chartSessions.setDragDecelerationFrictionCoef(0.95f);
        chartSessions.setDrawHoleEnabled(true);
        chartSessions.setHoleColor(android.graphics.Color.WHITE);
        chartSessions.setTransparentCircleRadius(61f);
        chartSessions.setEntryLabelColor(android.graphics.Color.BLACK);
        chartSessions.setEntryLabelTextSize(12f);
        chartSessions.getLegend().setEnabled(false);
    }

    private void fetchUserStats() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        RetrofitClient.getAgentService().getUserStats(userId)
                .enqueue(new Callback<com.example.plateit.responses.UserStatsResponse>() {
                    @Override
                    public void onResponse(Call<com.example.plateit.responses.UserStatsResponse> call,
                            Response<com.example.plateit.responses.UserStatsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.plateit.responses.UserStatsResponse stats = response.body();
                            updateStatsUI(stats);
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.plateit.responses.UserStatsResponse> call, Throwable t) {
                        // Fail silently
                    }
                });
    }

    private void updateStatsUI(com.example.plateit.responses.UserStatsResponse stats) {
        tvStatRecipes.setText(stats.getTotalRecipes() + " Recipes Saved");
        tvStatDays.setText(stats.getActiveDays() + " Active Days");
        tvStatSessions.setText(stats.getTotalSessions() + " Total Sessions");

        if (stats.getTotalSessions() > 0) {
            List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();
            if (stats.getFinishedSessions() > 0) {
                entries.add(new com.github.mikephil.charting.data.PieEntry(stats.getFinishedSessions(), "Done"));
            }
            if (stats.getUnfinishedSessions() > 0) {
                entries.add(new com.github.mikephil.charting.data.PieEntry(stats.getUnfinishedSessions(), "Progress"));
            }

            com.github.mikephil.charting.data.PieDataSet dataSet = new com.github.mikephil.charting.data.PieDataSet(
                    entries, "");
            dataSet.setColors(new int[] {
                    android.graphics.Color.parseColor("#4CAF50"), // Green
                    android.graphics.Color.parseColor("#FFC107") // Amber
            });
            dataSet.setSliceSpace(3f);
            dataSet.setSelectionShift(5f);

            com.github.mikephil.charting.data.PieData data = new com.github.mikephil.charting.data.PieData(dataSet);
            data.setValueTextSize(10f);
            data.setValueTextColor(android.graphics.Color.BLACK);

            chartSessions.setData(data);
            chartSessions.invalidate();
        }
    }

    private void onResumeSession(com.example.plateit.responses.CookingSession session, CookbookEntry entry) {
        launchCookingMode(session, entry);
    }

    private void launchCookingMode(com.example.plateit.responses.CookingSession session, CookbookEntry entry) {
        if (entry.getRecipeData() != null) {
            Intent intent = new Intent(getContext(), CookingModeActivity.class);
            String json = new Gson().toJson(entry.getRecipeData());
            intent.putExtra("recipe_json", json);
            intent.putExtra("session_id", session.getId());
            intent.putExtra("initial_step", session.getCurrentStepIndex());
            intent.putExtra("cookbook_id", entry.getId());
            startActivity(intent);
        }
    }

    private void fetchCookbook() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        android.util.Log.d("PlateIt", "DEBUG: Calling getCookbook for " + userId);
        RetrofitClient.getAgentService().getCookbook(userId).enqueue(new Callback<List<CookbookEntry>>() {
            @Override
            public void onResponse(Call<List<CookbookEntry>> call, Response<List<CookbookEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CookbookEntry> recipes = response.body();
                    myCookbook = recipes;
                    android.util.Log.d("PlateIt", "Cookbook fetched: " + recipes.size() + " items");

                    if (recipes.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvCookbook.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvCookbook.setVisibility(View.VISIBLE);
                        adapter.updateData(recipes);
                    }
                } else {
                    android.util.Log.e("PlateIt", "Cookbook fetch failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<CookbookEntry>> call, Throwable t) {
                android.util.Log.e("PlateIt", "Cookbook Fetch Error: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show();

                // Still try to find sessions
            }
        });
    }

    private void fetchActiveSession() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        RetrofitClient.getAgentService().getActiveCookingSession(userId)
                .enqueue(new Callback<com.example.plateit.responses.CookingSession>() {
                    @Override
                    public void onResponse(Call<com.example.plateit.responses.CookingSession> call,
                            Response<com.example.plateit.responses.CookingSession> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.plateit.responses.CookingSession session = response.body();

                            // Match recipe
                            CookbookEntry match = null;
                            if (myCookbook != null) {
                                for (CookbookEntry entry : myCookbook) {
                                    if (entry.getId() == session.getCookbookId()) {
                                        match = entry;
                                        break;
                                    }
                                }
                            }

                            if (match != null) {
                                cvActiveSession.setVisibility(View.VISIBLE);
                                tvActiveTitle.setText(match.getTitle());
                                tvActiveStep.setText("Currently on Step " + (session.getCurrentStepIndex() + 1));

                                final CookbookEntry finalMatch = match;
                                btnResume.setOnClickListener(v -> launchCookingMode(session, finalMatch));
                            } else {
                                cvActiveSession.setVisibility(View.GONE);
                            }
                        } else {
                            cvActiveSession.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.plateit.responses.CookingSession> call, Throwable t) {
                        cvActiveSession.setVisibility(View.GONE);
                    }
                });
    }

    private void fetchHistory() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        RetrofitClient.getAgentService().getAllCookingSessions(userId)
                .enqueue(new Callback<List<com.example.plateit.responses.CookingSession>>() {
                    @Override
                    public void onResponse(Call<List<com.example.plateit.responses.CookingSession>> call,
                            Response<List<com.example.plateit.responses.CookingSession>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<com.example.plateit.responses.CookingSession> sessions = response.body();
                            if (!sessions.isEmpty()) {
                                tvSessionsHeader.setVisibility(View.VISIBLE);
                                tvSessionsHeader.setText("Recent Activity");
                                rvSessions.setVisibility(View.VISIBLE);
                                sessionAdapter.updateData(sessions, myCookbook);
                            } else {
                                tvSessionsHeader.setVisibility(View.GONE);
                                rvSessions.setVisibility(View.GONE);
                            }
                        } else {
                            tvSessionsHeader.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<com.example.plateit.responses.CookingSession>> call, Throwable t) {
                        tvSessionsHeader.setVisibility(View.GONE);
                    }
                });
    }

    private void openRecipe(CookbookEntry entry) {
        if (entry.getRecipeData() != null) {
            Intent intent = new Intent(getContext(), RecipeActivity.class);
            String json = new Gson().toJson(entry.getRecipeData());
            intent.putExtra("recipe_json", json);
            intent.putExtra("cookbook_id", entry.getId());
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Recipe data corrupted", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecipe(CookbookEntry entry) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Delete Recipe")
                .setMessage("Remove '" + entry.getTitle() + "' from your cookbook?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    RetrofitClient.getAgentService().deleteFromCookbook(entry.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                fetchCookbook();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditProfileDialog(TextView tvNameTarget) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        TextInputEditText etName = view.findViewById(R.id.etChefName);
        Button btnSave = view.findViewById(R.id.btnSaveProfile);

        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString();
            if (!newName.isEmpty()) {
                tvNameTarget.setText(newName);
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
