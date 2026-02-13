package com.example.plateit;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plateit.adapters.PantryAdapter;
import com.example.plateit.api.RetrofitClient;
import com.example.plateit.db.DatabaseClient;
import com.example.plateit.db.PantryDao;
import com.example.plateit.db.PantryItem;
import com.example.plateit.requests.PantryScanRequest;
import com.example.plateit.responses.PantryScanResponse;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.example.plateit.R;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.plateit.utils.TokenManager;

public class PantryFragment extends Fragment {

    private RecyclerView rvPantryItems;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private PantryAdapter adapter;
    private List<PantryItem> pantryTypeList = new ArrayList<>();
    private com.example.plateit.utils.SessionManager sessionManager;
    private com.example.plateit.utils.LoadingDialog loadingDialog;

    private static final int CAMERA_REQUEST_CODE = 202;

    public PantryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pantry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadingDialog = new com.example.plateit.utils.LoadingDialog(getActivity());
        rvPantryItems = view.findViewById(R.id.rvPantryItems);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        View fabAdd = view.findViewById(R.id.fabAdd);

        // Standardized AppBar Setup for Pantry - Using Text Action for clarity
        com.example.plateit.utils.AppBarHelper.setup(getActivity(), "Pantry", false, 0,
                "Fetch Recipes", this::cookWithPantry);

        rvPantryItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PantryAdapter(pantryTypeList, item -> deleteItem(item));
        rvPantryItems.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddOptions());

        sessionManager = new com.example.plateit.utils.SessionManager(requireContext());
        loadPantryItems(false);
    }

    private void cookWithPantry() {
        if (pantryTypeList.isEmpty()) {
            Toast.makeText(getContext(), "Pantry is empty! Add items first.", Toast.LENGTH_SHORT).show();
            return;
        }
        showIngredientSelectionDialog();
    }

    private void showIngredientSelectionDialog() {
        // Prepare list of ingredient names
        String[] ingredients = new String[pantryTypeList.size()];
        boolean[] checkedItems = new boolean[pantryTypeList.size()];
        for (int i = 0; i < pantryTypeList.size(); i++) {
            ingredients[i] = pantryTypeList.get(i).name;
            checkedItems[i] = true; // Default all selected
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Ingredients")
                .setMultiChoiceItems(ingredients, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Find Recipes", (dialog, which) -> {
                    List<String> selectedIngredients = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedIngredients.add(ingredients[i]);
                        }
                    }
                    if (!selectedIngredients.isEmpty()) {
                        findRecipesByIngredients(selectedIngredients);
                    } else {
                        Toast.makeText(getContext(), "Select at least one ingredient", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void findRecipesByIngredients(List<String> ingredients) {
        loadingDialog.startLoadingDialog("Finding Recipes...");
        com.example.plateit.requests.IngredientSearchRequest request = new com.example.plateit.requests.IngredientSearchRequest(
                ingredients, 10);

        RetrofitClient.getAgentService().findRecipesByIngredients(request)
                .enqueue(new Callback<List<com.example.plateit.responses.RecipeSummary>>() {
                    @Override
                    public void onResponse(Call<List<com.example.plateit.responses.RecipeSummary>> call,
                            Response<List<com.example.plateit.responses.RecipeSummary>> response) {
                        loadingDialog.dismissDialog();
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            showRecipeResultsDialog(response.body());
                        } else {
                            Toast.makeText(getContext(), "No matching recipes found.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<com.example.plateit.responses.RecipeSummary>> call, Throwable t) {
                        loadingDialog.dismissDialog();
                        Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRecipeResultsDialog(List<com.example.plateit.responses.RecipeSummary> recipes) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_pantry_recipes, null);
        bottomSheetDialog.setContentView(sheetView);

        RecyclerView rvResults = sheetView.findViewById(R.id.rvRecipeResults);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));

        // Creating a simple inner adapter or using a new separate adapter class?
        // For brevity, I'll assume we need to create 'PantryRecipeAdapter'.
        // I will use a simple inline binding logic or placeholder for now,
        // but user requested to "show returned recipes".
        // Let's create a quick adapter here or assume one exists.
        // I'll create a simple adapter inside this method or class.

        PantryRecipeAdapter recipeAdapter = new PantryRecipeAdapter(recipes, recipe -> {
            bottomSheetDialog.dismiss();
            fetchFullRecipeDetails(recipe.getId());
        });
        rvResults.setAdapter(recipeAdapter);

        bottomSheetDialog.show();
    }

    private void fetchFullRecipeDetails(int recipeId) {
        loadingDialog.startLoadingDialog("Loading Details...");

        RetrofitClient.getAgentService().getRecipeDetails(recipeId)
                .enqueue(new Callback<com.example.plateit.responses.RecipeResponse>() {
                    @Override
                    public void onResponse(Call<com.example.plateit.responses.RecipeResponse> call,
                            Response<com.example.plateit.responses.RecipeResponse> response) {
                        loadingDialog.dismissDialog();
                        if (response.isSuccessful() && response.body() != null) {
                            // Navigate to RecipeActivity
                            Intent intent = new Intent(getContext(), RecipeActivity.class);
                            // Pass JSON
                            String json = new com.google.gson.Gson().toJson(response.body());
                            intent.putExtra("recipe_json", json);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(), "Failed to load details.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.plateit.responses.RecipeResponse> call, Throwable t) {
                        loadingDialog.dismissDialog();
                        Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Internal Adapter Class for Recipe Results
    private static class PantryRecipeAdapter extends RecyclerView.Adapter<PantryRecipeAdapter.ViewHolder> {
        private final List<com.example.plateit.responses.RecipeSummary> recipes;
        private final OnRecipeClickListener listener;

        interface OnRecipeClickListener {
            void onRecipeClick(com.example.plateit.responses.RecipeSummary recipe);
        }

        public PantryRecipeAdapter(List<com.example.plateit.responses.RecipeSummary> recipes,
                OnRecipeClickListener listener) {
            this.recipes = recipes;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry_recipe_result, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            com.example.plateit.responses.RecipeSummary recipe = recipes.get(position);
            holder.tvTitle.setText(recipe.getTitle());
            holder.tvInfo.setText(
                    "Used: " + recipe.getUsedIngredientCount() + " | Missing: " + recipe.getMissedIngredientCount());

            if (recipe.getImage() != null && !recipe.getImage().isEmpty()) {
                com.squareup.picasso.Picasso.get().load(recipe.getImage()).into(holder.imgThumbnail);
            }

            holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
        }

        @Override
        public int getItemCount() {
            return recipes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvInfo;
            android.widget.ImageView imgThumbnail;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvRecipeTitle);
                tvInfo = itemView.findViewById(R.id.tvRecipeInfo);
                imgThumbnail = itemView.findViewById(R.id.imgRecipeThumbnail);
            }
        }
    }

    private void showAddOptions() {
        String[] options = { "Type Manually", "Scan with Camera (1 🪙)", "Choose from Gallery (1 🪙)" };
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Item")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showManualAddDialog();
                    } else {
                        // Check Tokens for Vision features
                        TokenManager tokenManager = TokenManager.getInstance(getContext());
                        if (!tokenManager.canAfford(1)) {
                            android.content.Intent intent = new android.content.Intent(getContext(),
                                    PaywallActivity.class);
                            startActivity(intent);
                            return;
                        }

                        // Deduct and Proceed
                        tokenManager.useTokens(1);

                        if (which == 1) {
                            openCamera();
                        } else {
                            galleryLauncher.launch("image/*");
                        }
                    }
                })
                .show();
    }

    private void showManualAddDialog() {
        android.view.View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_pantry_item, null);
        android.widget.EditText etName = dialogView.findViewById(R.id.etName);
        android.widget.EditText etAmount = dialogView.findViewById(R.id.etAmount);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add Pantry Item")
                .setView(dialogView)
                .setPositiveButton("Add", null) // Set null here to override later
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String amount = etAmount.getText().toString().trim();
                if (!name.isEmpty()) {
                    dialog.dismiss();
                    fetchImageAndSave(name, amount);
                }
            });
        });

        dialog.show();
    }

    private void fetchImageAndSave(String name, String amount) {
        loadingDialog.startLoadingDialog("Fetching Image...");

        RetrofitClient.getAgentService().getIngredientImage(name)
                .enqueue(new Callback<com.example.plateit.responses.IngredientImageResponse>() {
                    @Override
                    public void onResponse(Call<com.example.plateit.responses.IngredientImageResponse> call,
                            Response<com.example.plateit.responses.IngredientImageResponse> response) {
                        loadingDialog.dismissDialog();
                        String imageUrl = null;
                        if (response.isSuccessful() && response.body() != null) {
                            imageUrl = response.body().getImageUrl();
                        }
                        saveItem(name, amount, imageUrl);
                    }

                    @Override
                    public void onFailure(Call<com.example.plateit.responses.IngredientImageResponse> call,
                            Throwable t) {
                        loadingDialog.dismissDialog();
                        saveItem(name, amount, null); // Save without image on failure
                    }
                });
    }

    // --- Camera & Gallery Launchers ---
    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCameraIntent();
                } else {
                    Toast.makeText(getContext(), "Camera permission required.", Toast.LENGTH_SHORT).show();
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            processImage(imageBitmap);
                        }
                    }
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processUri(uri);
                }
            });

    private void openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchCameraIntent();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void launchCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Camera not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void processUri(android.net.Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            processImage(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private void processImage(Bitmap bitmap) {
        loadingDialog.startLoadingDialog("Analyzing Pantry...");

        try {
            // 1. Save Bitmap to File
            java.io.File file = new java.io.File(getContext().getCacheDir(),
                    "pantry_scan_" + System.currentTimeMillis() + ".jpg");
            file.createNewFile();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bitmapdata = bos.toByteArray();

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();

            // 2. Create Multipart Body
            okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), file);
            okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("file", file.getName(),
                    reqFile);

            // 3. Upload
            RetrofitClient.getAgentService().scanPantryImage(body).enqueue(new Callback<PantryScanResponse>() {
                @Override
                public void onResponse(Call<PantryScanResponse> call, Response<PantryScanResponse> response) {
                    loadingDialog.dismissDialog();
                    if (response.isSuccessful() && response.body() != null) {
                        List<PantryScanResponse.PantryItem> scannedItems = response.body().getItems();
                        if (scannedItems != null && !scannedItems.isEmpty()) {
                            saveBatchItems(scannedItems);
                            Toast.makeText(getContext(), "Identified " + scannedItems.size() + " items!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "No items found. Try a clearer photo.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                    // Optimize: Delete temp file
                    file.delete();
                }

                @Override
                public void onFailure(Call<PantryScanResponse> call, Throwable t) {
                    loadingDialog.dismissDialog();
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    file.delete();
                }
            });

        } catch (Exception e) {
            loadingDialog.dismissDialog();
            e.printStackTrace();
            Toast.makeText(getContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveItem(String name, String amount, String imageUrl) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.plateit.requests.PantryItemCreateRequest request = new com.example.plateit.requests.PantryItemCreateRequest(
                userId, name, amount, imageUrl);

        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getAgentService().addPantryItem(request).enqueue(new Callback<PantryItem>() {
            @Override
            public void onResponse(Call<PantryItem> call, Response<PantryItem> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    // Reset fetch timer to force a fresh pull
                    sessionManager.setLastPantryFetchTime(0);
                    loadPantryItems(true);
                } else {
                    Toast.makeText(getContext(), "Failed to add item", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PantryItem> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBatchItems(List<PantryScanResponse.PantryItem> scannedItems) {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        Toast.makeText(getContext(), "Saving " + scannedItems.size() + " items...", Toast.LENGTH_SHORT).show();

        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(
                scannedItems.size());

        for (PantryScanResponse.PantryItem sItem : scannedItems) {
            com.example.plateit.requests.PantryItemCreateRequest request = new com.example.plateit.requests.PantryItemCreateRequest(
                    userId, sItem.getName(), sItem.getAmount(), sItem.getImageUrl());
            RetrofitClient.getAgentService().addPantryItem(request).enqueue(new Callback<PantryItem>() {
                @Override
                public void onResponse(Call<PantryItem> call, Response<PantryItem> response) {
                    if (counter.decrementAndGet() == 0) {
                        sessionManager.setLastPantryFetchTime(0); // Force refresh
                        loadPantryItems(true);
                    }
                }

                @Override
                public void onFailure(Call<PantryItem> call, Throwable t) {
                    if (counter.decrementAndGet() == 0) {
                        loadPantryItems(true);
                    }
                }
            });
        }
    }

    private void deleteItem(PantryItem item) {
        RetrofitClient.getAgentService().deletePantryItem(item.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    sessionManager.setLastPantryFetchTime(0); // Force refresh
                    loadPantryItems(true);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPantryItems(boolean forceRefresh) {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        android.util.Log.d("PantryFragment",
                "loadPantryItems: Initiated for user " + userId + " (force=" + forceRefresh + ")");

        // 1. Load from Local Cache First (Instant UI)
        new AsyncTask<Void, Void, List<PantryItem>>() {
            @Override
            protected List<PantryItem> doInBackground(Void... voids) {
                return DatabaseClient.getInstance(getContext()).getAppDatabase().pantryDao().getAll();
            }

            @Override
            protected void onPostExecute(List<PantryItem> cachedItems) {
                if (getContext() == null)
                    return;

                long lastFetch = sessionManager.getLastPantryFetchTime();
                long currentTime = System.currentTimeMillis();
                long fiveMinutes = 5 * 60 * 1000L;

                boolean hasCache = cachedItems != null && !cachedItems.isEmpty();
                // Update UI from cache if we don't have items showing yet
                if (hasCache && pantryTypeList.isEmpty()) {
                    pantryTypeList = cachedItems;
                    adapter.updateList(pantryTypeList);
                    tvEmpty.setVisibility(View.GONE);
                    android.util.Log.d("PantryFragment",
                            "Cache HIT: Loaded " + cachedItems.size() + " items from local DB");
                }

                // 2. Fetch Fresh Data only if forced, or cache is old/missing
                if (forceRefresh || !hasCache || (currentTime - lastFetch > fiveMinutes)) {
                    android.util.Log.d("PantryFragment", "Triggering network fetch. Force=" + forceRefresh
                            + ", cacheAge=" + (currentTime - lastFetch) / 1000 + "s");
                    fetchPantryFromNetwork(userId);
                } else {
                    android.util.Log.d("PantryFragment", "THROTTLED: Using local data. Skipping network hit.");
                }
            }
        }.execute();
    }

    private void fetchPantryFromNetwork(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getAgentService().getPantryItems(userId).enqueue(new Callback<List<PantryItem>>() {
            @Override
            public void onResponse(Call<List<PantryItem>> call, Response<List<PantryItem>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<PantryItem> freshItems = response.body();

                    // Parse dates and update local list
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                            java.util.Locale.getDefault());
                    for (PantryItem item : freshItems) {
                        try {
                            if (item.start_date != null) {
                                item.dateAdded = sdf.parse(item.start_date).getTime();
                            }
                        } catch (Exception e) {
                            item.dateAdded = System.currentTimeMillis();
                        }
                    }

                    // 3. Update UI
                    pantryTypeList = freshItems;
                    adapter.updateList(pantryTypeList);
                    tvEmpty.setVisibility(pantryTypeList.isEmpty() ? View.VISIBLE : View.GONE);

                    // 4. Update Local Database (Sync)
                    syncLocalDatabase(freshItems);

                    // 5. Update last fetch time
                    sessionManager.setLastPantryFetchTime(System.currentTimeMillis());
                }
            }

            @Override
            public void onFailure(Call<List<PantryItem>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                // If network fails, we already show the cached items (if any)
                android.util.Log.e("PantryFragment", "Network fetch failed, showing cached data: " + t.getMessage());
            }
        });
    }

    private void syncLocalDatabase(List<PantryItem> freshItems) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                PantryDao dao = DatabaseClient.getInstance(getContext()).getAppDatabase().pantryDao();
                dao.deleteAll();
                dao.insertAll(freshItems.toArray(new PantryItem[0]));
                return null;
            }
        }.execute();
    }
}
