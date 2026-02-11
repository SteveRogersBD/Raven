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
        TextView tvChefName = view.findViewById(R.id.tvChefName);
        // Load name if saved (SessionManager could store this, for now just UI)

        btnEdit.setOnClickListener(v -> showEditProfileDialog(tvChefName));

        // Cookbook Section
        rvCookbook = view.findViewById(R.id.rvCookbook);
        tvEmpty = view.findViewById(R.id.tvEmptyCookbook);

        rvCookbook.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CookbookAdapter(new ArrayList<>(), this::openRecipe, this::deleteRecipe);
        rvCookbook.setAdapter(adapter);

        // Fetch Data
        fetchCookbook();

        return view;
    }

    private void fetchCookbook() {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        RetrofitClient.getAgentService().getCookbook(userId).enqueue(new Callback<List<CookbookEntry>>() {
            @Override
            public void onResponse(Call<List<CookbookEntry>> call, Response<List<CookbookEntry>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CookbookEntry> recipes = response.body();
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
                    android.util.Log.e("PlateIt", "Fetch failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<CookbookEntry>> call, Throwable t) {
                android.util.Log.e("PlateIt", "Fetch Error: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Failed to load cookbook", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openRecipe(CookbookEntry entry) {
        if (entry.getRecipeData() != null) {
            Intent intent = new Intent(getContext(), RecipeActivity.class);
            String json = new Gson().toJson(entry.getRecipeData());
            intent.putExtra("recipe_json", json);
            // Also pass cookbook ID if we want to track session against it
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
                                fetchCookbook(); // Refresh
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
