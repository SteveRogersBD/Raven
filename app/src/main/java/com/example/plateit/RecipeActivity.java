package com.example.plateit;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RecipeActivity extends AppCompatActivity {

    private TextView ingredientsTextView;
    private TextView stepsTextView;
    private com.example.plateit.responses.ShoppingList currentShoppingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        // Standardized AppBar Setup
        com.example.plateit.utils.AppBarHelper.setup(this, "Recipe Details", true);

        android.widget.TextView tvTitle = findViewById(R.id.tvRecipeTitle);
        android.widget.TextView tvTime = findViewById(R.id.tvRecipeTime);
        androidx.cardview.widget.CardView cvSourceCard = findViewById(R.id.cvSourceCard);
        android.widget.ImageView imgSourceThumbnail = findViewById(R.id.imgSourceThumbnail);
        android.view.View btnWatchSource = findViewById(R.id.btnWatchSource);
        androidx.recyclerview.widget.RecyclerView rvIngredients = findViewById(R.id.rvIngredients);
        androidx.recyclerview.widget.RecyclerView rvSteps = findViewById(R.id.rvSteps);

        com.example.plateit.responses.RecipeResponse recipe = null;
        try {
            String json = getIntent().getStringExtra("recipe_json");
            if (json != null) {
                recipe = new com.google.gson.Gson().fromJson(json, com.example.plateit.responses.RecipeResponse.class);
            }
        } catch (Exception e) {
            android.util.Log.e("PlateIt", "RecipeActivity: JSON Parse Error", e);
        }

        // Initialize SessionManager
        com.example.plateit.utils.SessionManager sessionManager = new com.example.plateit.utils.SessionManager(this);

        // Retrieve existing cookbook_id from intent if available
        int intentCookbookId = getIntent().getIntExtra("cookbook_id", -1);
        final Integer[] cookbookId = { intentCookbookId != -1 ? intentCookbookId : null };

        // Save Button Logic
        android.widget.Button btnSave = findViewById(R.id.btnSaveCookbook);
        if (cookbookId[0] != null) {
            btnSave.setText("Saved to Cookbook");
            btnSave.setEnabled(false);
        }

        final com.example.plateit.responses.RecipeResponse finalRecipe = recipe;

        btnSave.setOnClickListener(v -> {
            if (finalRecipe == null)
                return;
            String userId = sessionManager.getUserId();
            if (userId == null) {
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            com.example.plateit.requests.CookbookEntryCreate request = new com.example.plateit.requests.CookbookEntryCreate(
                    userId,
                    finalRecipe.getName(),
                    finalRecipe, // Pass the whole object as recipe_data
                    finalRecipe.getSourceUrl(),
                    finalRecipe.getSourceImage());

            com.example.plateit.api.RetrofitClient.getAgentService().addToCookbook(request)
                    .enqueue(new retrofit2.Callback<com.example.plateit.responses.CookbookEntry>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.example.plateit.responses.CookbookEntry> call,
                                retrofit2.Response<com.example.plateit.responses.CookbookEntry> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                cookbookId[0] = response.body().getId();
                                btnSave.setText("Saved!");
                                android.util.Log.d("PlateIt", "Recipe saved successfully. ID: " + cookbookId[0]);
                                android.util.Log.e("PlateIt",
                                        "Save failed: " + response.code() + " " + response.message());
                                btnSave.setEnabled(true);
                                btnSave.setText("Save to My Cookbook");
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.example.plateit.responses.CookbookEntry> call,
                                Throwable t) {
                            android.util.Log.e("PlateIt", "Save Error: " + t.getMessage(), t);
                            btnSave.setEnabled(true);
                            btnSave.setText("Save to My Cookbook");
                        }
                    });
        });

        findViewById(R.id.btnStartCooking).setOnClickListener(v -> {
            if (finalRecipe != null && finalRecipe.getSteps() != null) {
                android.content.Intent intent = new android.content.Intent(this, CookingModeActivity.class);

                // Convert Response to Model for Intent passing
                com.example.plateit.models.Recipe recipeModel = new com.example.plateit.models.Recipe(
                        finalRecipe.getName(),
                        finalRecipe.getSteps(),
                        finalRecipe.getIngredients(),
                        finalRecipe.getSourceUrl(),
                        finalRecipe.getSourceImage(),
                        finalRecipe.getTotalTime());

                // Pass as JSON
                String jsonModel = new com.google.gson.Gson().toJson(recipeModel);
                intent.putExtra("recipe_json", jsonModel);

                // Pass cookbook ID for session tracking
                if (cookbookId[0] != null) {
                    intent.putExtra("cookbook_id", cookbookId[0]);
                }

                startActivity(intent);
            } else {
                // No steps available
            }
        });

        findViewById(R.id.btnChooseAnother).setOnClickListener(v -> finish());

        // Check for null recipe and log
        if (recipe != null) {
            // Header

            // Header
            if (getSupportActionBar() != null)
                getSupportActionBar().hide();
            com.example.plateit.utils.AppBarHelper.setup(this, recipe.getName(), true);
            tvTitle.setText(recipe.getName());

            tvTime.setText(recipe.getTotalTime() != null ? recipe.getTotalTime() : "N/A");

            // Populate Source Card
            String sourceUrl = recipe.getSourceUrl();
            String sourceImage = recipe.getSourceImage();

            if (sourceUrl != null && !sourceUrl.isEmpty() && sourceUrl.startsWith("http")) {
                cvSourceCard.setVisibility(android.view.View.VISIBLE);

                if (sourceImage != null && !sourceImage.isEmpty()) {
                    com.squareup.picasso.Picasso.get()
                            .load(sourceImage)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .fit()
                            .into(imgSourceThumbnail);
                } else {
                    imgSourceThumbnail.setImageResource(R.drawable.ic_launcher_background);
                }

                // Click listener
                android.view.View.OnClickListener openUrl = v -> {
                    android.content.Intent browserIntent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW, android.net.Uri.parse(sourceUrl));
                    startActivity(browserIntent);
                };

                cvSourceCard.setOnClickListener(openUrl);
                if (btnWatchSource != null) {
                    btnWatchSource.setOnClickListener(openUrl);
                }
            } else {
                cvSourceCard.setVisibility(android.view.View.GONE);
            }

            // Ingredients (Horizontal)
            rvIngredients.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this,
                    androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            com.example.plateit.adapters.IngredientsAdapter ingredientsAdapter = new com.example.plateit.adapters.IngredientsAdapter(
                    recipe.getIngredients());
            rvIngredients.setAdapter(ingredientsAdapter);

            // Steps (Vertical)
            rvSteps.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            com.example.plateit.adapters.StepsAdapter stepsAdapter = new com.example.plateit.adapters.StepsAdapter(
                    recipe.getSteps());
            rvSteps.setAdapter(stepsAdapter);

            // Added: check pantry and highlight
            checkPantryAndHighlightIngredients(recipe, ingredientsAdapter);

            // Added: Shopping List Button Logic
            android.widget.Button btnShopping = findViewById(R.id.btnCreateShoppingList);
            btnShopping.setOnClickListener(v -> {
                if (currentShoppingList == null) {
                    createShoppingList(finalRecipe);
                } else {
                    viewAndEditShoppingList();
                }
            });

        } else {
            android.util.Log.e("PlateIt", "RecipeActivity: Recipe is NULL!");
            finish();
        }
    }

    private void checkPantryAndHighlightIngredients(com.example.plateit.responses.RecipeResponse recipe,
            com.example.plateit.adapters.IngredientsAdapter adapter) {
        String userId = new com.example.plateit.utils.SessionManager(this).getUserId();
        if (userId == null)
            return;

        com.example.plateit.api.RetrofitClient.getAgentService().getPantryItems(userId)
                .enqueue(new retrofit2.Callback<java.util.List<com.example.plateit.db.PantryItem>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.List<com.example.plateit.db.PantryItem>> call,
                            retrofit2.Response<java.util.List<com.example.plateit.db.PantryItem>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            java.util.Set<String> pantryNames = new java.util.HashSet<>();
                            for (com.example.plateit.db.PantryItem item : response.body()) {
                                pantryNames.add(item.name.toLowerCase().trim());
                            }

                            for (com.example.plateit.models.Ingredient ing : recipe.getIngredients()) {
                                if (!pantryNames.contains(ing.getName().toLowerCase().trim())) {
                                    ing.setMissing(true);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<java.util.List<com.example.plateit.db.PantryItem>> call,
                            Throwable t) {
                        android.util.Log.e("PlateIt", "Pantry check failed", t);
                    }
                });
    }

    private void createShoppingList(com.example.plateit.responses.RecipeResponse recipe) {
        if (recipe == null)
            return;
        String userId = new com.example.plateit.utils.SessionManager(this).getUserId();
        if (userId == null) {
            return;
        }

        android.widget.Button btn = findViewById(R.id.btnCreateShoppingList);
        btn.setEnabled(false);
        btn.setText("Creating...");

        java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("recipe_name", recipe.getName());
        requestBody.put("ingredients", recipe.getIngredients());

        com.example.plateit.api.RetrofitClient.getAgentService().createShoppingListFromRecipe(requestBody)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.ShoppingListFromRecipeResponse>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<com.example.plateit.responses.ShoppingListFromRecipeResponse> call,
                            retrofit2.Response<com.example.plateit.responses.ShoppingListFromRecipeResponse> response) {
                        btn.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            currentShoppingList = response.body().getList();
                            btn.setText("View Shopping List");
                        } else {
                            btn.setText("Create Shopping List");
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<com.example.plateit.responses.ShoppingListFromRecipeResponse> call,
                            Throwable t) {
                        btn.setEnabled(true);
                        btn.setText("Create Shopping List");
                    }
                });
    }

    private void viewAndEditShoppingList() {
        if (currentShoppingList == null)
            return;

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_shopping_list, null);
        androidx.recyclerview.widget.RecyclerView rvItems = dialogView.findViewById(R.id.rvEditShoppingItems);
        rvItems.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        com.example.plateit.adapters.ShoppingItemsEditAdapter editAdapter = new com.example.plateit.adapters.ShoppingItemsEditAdapter(
                currentShoppingList.getItems());
        rvItems.setAdapter(editAdapter);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Edit Shopping List")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    updateShoppingListOnServer();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void updateShoppingListOnServer() {
        if (currentShoppingList == null)
            return;

        com.example.plateit.requests.ShoppingListUpdate update = new com.example.plateit.requests.ShoppingListUpdate(
                currentShoppingList.getTitle(),
                currentShoppingList.getItems());

        com.example.plateit.api.RetrofitClient.getAgentService().updateShoppingList(currentShoppingList.getId(), update)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.ShoppingList>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.plateit.responses.ShoppingList> call,
                            retrofit2.Response<com.example.plateit.responses.ShoppingList> response) {
                        if (response.isSuccessful()) {
                            // Shopping list updated successfully
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.plateit.responses.ShoppingList> call,
                            Throwable t) {
                    }
                });
    }
}
