package com.example.plateit;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.plateit.requests.VideoRequest;
import com.example.plateit.responses.RecipeResponse;
import com.example.plateit.api.RetrofitClient;
import com.example.plateit.db.DatabaseClient;
import com.example.plateit.db.VideoDao;
import com.example.plateit.db.VideoEntity;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import com.example.plateit.utils.TokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    private android.widget.TextView tvTokenBalance;
    private android.widget.TextView tvProBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvRecipes = view.findViewById(R.id.rvRecipes);
        rvRecipes.setLayoutManager(new LinearLayoutManager(getContext()));

        EditText etPasteUrl = view.findViewById(R.id.etPasteUrl);
        ImageView ivPlatform = view.findViewById(R.id.ivPlatform);
        ImageView btnPaste = view.findViewById(R.id.btnPaste);
        ImageView btnScan = view.findViewById(R.id.btnScan);

        // Update Token Balance
        tvTokenBalance = view.findViewById(R.id.tvTokenBalance);
        tvProBadge = view.findViewById(R.id.tvProBadge);
        updateTokenDisplay();

        // --- 1. Smart Link Detection Logic (Left Icon) ---
        etPasteUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().toLowerCase();
                ivPlatform.setImageTintList(null); // Reset color filter if needed

                if (text.contains("youtube.com") || text.contains("youtu.be")) {
                    ivPlatform.setImageResource(R.drawable.youtube);
                } else if (text.contains("instagram.com")) {
                    ivPlatform.setImageResource(R.drawable.instagram);
                } else if (text.contains("twitter.com") || text.contains("x.com")) {
                    ivPlatform.setImageResource(R.drawable.twitter);
                } else if (text.contains("facebook.com")) {
                    ivPlatform.setImageResource(R.drawable.facebook);
                } else if (text.contains("tiktok.com")) {
                    ivPlatform.setImageResource(R.drawable.tiktok);
                } else if (text.contains("http") || text.contains("www.")) {
                    ivPlatform.setImageResource(R.drawable.www);
                } else {
                    // Default generic icon
                    ivPlatform.setImageResource(R.drawable.www);
                    ivPlatform.setImageTintList(
                            android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray_600)));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // --- 2. Action Buttons ---

        btnScan.setOnClickListener(v -> showVisionBottomSheet());

        // btnUpload.setOnClickListener(v -> { // Removed
        // Toast.makeText(getContext(), "Opening Gallery for Video...",
        // Toast.LENGTH_SHORT).show();
        // // TODO: Implement Video Picker Logic here
        // });

        btnPaste.setOnClickListener(v -> {

            String url = etPasteUrl.getText().toString().trim();
            if (url.isEmpty()) {
                return;
            }

            extractRecipe(url);
        });

        // --- 3. RecyclerView Setup ---

        // Videos (Vertical)
        LinearLayoutManager videoLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL,
                false);
        rvRecipes.setLayoutManager(videoLayoutManager);

        // Initialize with empty list
        VideoAdapter videoAdapter = new VideoAdapter(new ArrayList<>(), false, this::showVideoOptionsDialog);
        rvRecipes.setAdapter(videoAdapter);

        // Fetch Real Data
        com.example.plateit.utils.SessionManager sessionManager = new com.example.plateit.utils.SessionManager(
                getContext());
        String userId = sessionManager.getUserId();

        if (userId != null) {
            fetchVideoRecommendations(userId, videoAdapter);
        } else {
            // Recommendation prompt
        }

        // Chat FAB
        View fabChat = view.findViewById(R.id.fabChat);
        fabChat.setOnClickListener(v -> showChatBottomSheet());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTokenDisplay();
    }

    private void updateTokenDisplay() {
        if (tvTokenBalance != null && getContext() != null) {
            tvTokenBalance.setText(String.valueOf(TokenManager.getInstance(getContext()).getTokens()));

            // Also check Pro status for the badge
            TokenManager.getInstance(getContext()).isPro(isPro -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (tvProBadge != null) {
                            tvProBadge.setVisibility(isPro ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            });
        }
    }

    private void fetchVideoRecommendations(String userId, VideoAdapter adapter) {
        android.util.Log.d("HomeFragment", "Fetching videos for user: " + userId);
        com.example.plateit.utils.SessionManager sessionManager = new com.example.plateit.utils.SessionManager(
                getContext());
        long lastFetch = sessionManager.getLastVideoFetchTime();
        long currentTime = System.currentTimeMillis();
        long thirtyMinutes = 30 * 60 * 1000;

        // 1. Load from Local Cache (Room)
        new AsyncTask<Void, Void, List<VideoEntity>>() {
            @Override
            protected List<VideoEntity> doInBackground(Void... voids) {
                return DatabaseClient.getInstance(getContext()).getAppDatabase().videoDao().getAll();
            }

            @Override
            protected void onPostExecute(List<VideoEntity> cachedVideos) {
                boolean hasCache = cachedVideos != null && !cachedVideos.isEmpty();
                if (hasCache) {
                    List<RecipeVideo> displayVideos = new ArrayList<>();
                    for (VideoEntity ve : cachedVideos) {
                        displayVideos
                                .add(new RecipeVideo(ve.title, ve.link, ve.thumbnail, ve.channel, ve.views, ve.length));
                    }
                    adapter.updateData(displayVideos);
                    android.util.Log.d("HomeFragment", "Loaded " + cachedVideos.size() + " videos from local cache");
                }

                // 2. Refresh from Network only if cache is old or missing
                if (!hasCache || (currentTime - lastFetch > thirtyMinutes)) {
                    android.util.Log.d("HomeFragment",
                            "Refreshing from network (Cache age: " + (currentTime - lastFetch) / 1000 + "s)");
                    refreshVideosFromNetwork(userId, adapter);
                } else {
                    android.util.Log.d("HomeFragment", "Throttling: Using cache, skipping network hit.");
                }
            }
        }.execute();
    }

    private void refreshVideosFromNetwork(String userId, VideoAdapter adapter) {
        RetrofitClient.getAgentService().getRecommendations(userId)
                .enqueue(new Callback<com.example.plateit.responses.VideoRecommendationResponse>() {
                    @Override
                    public void onResponse(Call<com.example.plateit.responses.VideoRecommendationResponse> call,
                            Response<com.example.plateit.responses.VideoRecommendationResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<RecipeVideo> videos = response.body().getVideos();
                            if (videos != null && !videos.isEmpty()) {
                                adapter.updateData(videos);
                                // 3. Sync to Cache
                                syncVideoCache(videos);
                                // 4. Update last fetch time for throttling
                                new com.example.plateit.utils.SessionManager(getContext())
                                        .setLastVideoFetchTime(System.currentTimeMillis());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.plateit.responses.VideoRecommendationResponse> call,
                            Throwable t) {
                        android.util.Log.e("HomeFragment", "Video Network Error: " + t.getMessage());
                        // If no cache and network fails, show error
                    }
                });
    }

    @SuppressLint("StaticFieldLeak")
    private void syncVideoCache(List<RecipeVideo> videos) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                VideoDao dao = DatabaseClient.getInstance(getContext()).getAppDatabase().videoDao();
                dao.deleteAll();
                List<VideoEntity> entities = new ArrayList<>();
                for (RecipeVideo rv : videos) {
                    entities.add(new VideoEntity(rv.getTitle(), rv.getLink(), rv.getThumbnail(), rv.getChannel(),
                            rv.getViews(), rv.getLength()));
                }
                dao.insertAll(entities);
                return null;
            }
        }.execute();
    }

    private void showChatBottomSheet() {
        android.content.Intent intent = new android.content.Intent(getContext(), ChatActivity.class);
        startActivity(intent);
    }

    // --- Vision Launchers ---

    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    // Camera permission required
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> cameraLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    android.graphics.Bitmap photo = (android.graphics.Bitmap) result.getData().getExtras().get("data");
                    processBitmapForRecipe(photo);
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processUriForRecipe(uri);
                }
            });

    private void launchCamera() {
        android.content.Intent takePictureIntent = new android.content.Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            cameraLauncher.launch(takePictureIntent);
        } catch (android.content.ActivityNotFoundException e) {
            // Camera not found
        }
    }

    private void showVisionBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_vision, null);
        bottomSheetDialog.setContentView(sheetView);

        View btnCamera = sheetView.findViewById(R.id.btnOptionCamera);
        View btnGallery = sheetView.findViewById(R.id.btnOptionGallery);

        btnCamera.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        });

        btnGallery.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            galleryLauncher.launch("image/*");
        });

        bottomSheetDialog.show();
    }

    // --- Image Processing Helpers ---

    private void processBitmapForRecipe(android.graphics.Bitmap bitmap) {
        try {
            java.io.File file = new java.io.File(getContext().getCacheDir(),
                    "recipe_scan_" + System.currentTimeMillis() + ".jpg");
            file.createNewFile();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bitmapdata = bos.toByteArray();

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();

            uploadImageForRecipe(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processUriForRecipe(android.net.Uri uri) {
        try {
            java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            java.io.File file = new java.io.File(getContext().getCacheDir(),
                    "recipe_gallery_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file);

            byte[] buffer = new byte[4 * 1024]; // 4kb buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            uploadImageForRecipe(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private com.example.plateit.utils.LoadingDialog loadingDialog;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadingDialog = new com.example.plateit.utils.LoadingDialog(getActivity());
    }

    private void uploadImageForRecipe(java.io.File file) {
        loadingDialog.startLoadingDialog("Scanning Dish...");

        okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), file);
        okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("file", file.getName(), reqFile);

        RetrofitClient.getAgentService().identifyDishFromImage(body).enqueue(new Callback<RecipeResponse>() {
            @Override
            public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                loadingDialog.dismissDialog();

                if (response.isSuccessful() && response.body() != null) {
                    showRecipePreviewDialog(response.body());
                } else {
                    // Identification failed
                }
                file.delete();
            }

            @Override
            public void onFailure(Call<RecipeResponse> call, Throwable t) {
                loadingDialog.dismissDialog();
                file.delete();
            }
        });
    }

    private void showVideoOptionsDialog(RecipeVideo video) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_video_options, null);
        bottomSheetDialog.setContentView(sheetView);

        android.widget.ImageView imgThumbnail = sheetView.findViewById(R.id.imgVideoThumbnail);
        android.widget.TextView tvTitle = sheetView.findViewById(R.id.tvVideoTitle);
        android.widget.TextView tvChannel = sheetView.findViewById(R.id.tvVideoChannel);
        View btnExtract = sheetView.findViewById(R.id.btnExtractRecipe);
        View btnWatch = sheetView.findViewById(R.id.btnWatchVideo);

        tvTitle.setText(video.getTitle());
        tvChannel.setText(video.getChannel() + " • " + (video.getViews() != null ? video.getViews() + " views" : ""));

        if (video.getThumbnail() != null && !video.getThumbnail().isEmpty()) {
            com.squareup.picasso.Picasso.get()
                    .load(video.getThumbnail())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(imgThumbnail);
        } else {
            imgThumbnail.setImageResource(R.drawable.ic_launcher_background);
        }

        btnExtract.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            extractRecipe(video.getLink(), video.getThumbnail());
        });

        // Update button text with cost
        ((android.widget.Button) btnExtract).setText("Extract Recipe (2 🪙)");

        btnWatch.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (video.getLink() != null) {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(video.getLink()));
                startActivity(intent);
            }
        });

        bottomSheetDialog.show();
    }

    private void extractRecipe(String url) {
        extractRecipe(url, "");
    }

    private void extractRecipe(String url, String thumbnailUrl) {
        int cost = 1; // Default Web Cost
        if (url.toLowerCase().contains("youtube") || url.toLowerCase().contains("youtu.be")) {
            cost = 2; // Video Cost
        }

        TokenManager tokenManager = TokenManager.getInstance(getContext());
        if (!tokenManager.canAfford(cost)) {
            android.content.Intent intent = new android.content.Intent(getContext(), PaywallActivity.class);
            startActivity(intent);
            return;
        }

        // Deduct tokens immediately (optimistic UI)
        tokenManager.useTokens(cost);
        updateTokenDisplay();

        loadingDialog.startLoadingDialog("Extracting Recipe...");

        VideoRequest request = new VideoRequest(url);
        RetrofitClient.getService().extractRecipe(request)
                .enqueue(new Callback<RecipeResponse>() {
                    @Override
                    public void onResponse(Call<RecipeResponse> call, Response<RecipeResponse> response) {
                        loadingDialog.dismissDialog();
                        if (response.isSuccessful() && response.body() != null) {
                            showRecipePreviewDialog(response.body());
                        } else {
                            // Extraction failed
                        }
                    }

                    @Override
                    public void onFailure(Call<RecipeResponse> call, Throwable t) {
                        loadingDialog.dismissDialog();
                    }
                });
    }

    // Removing old showExtractionProgress and extractionDialog field

    private void showRecipePreviewDialog(RecipeResponse recipe) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_recipe_preview, null);
        bottomSheetDialog.setContentView(sheetView);

        android.widget.TextView tvTitle = sheetView.findViewById(R.id.tvPreviewTitle);
        android.widget.TextView tvTime = sheetView.findViewById(R.id.tvPreviewTime);
        android.widget.TextView tvIngredientsCount = sheetView.findViewById(R.id.tvPreviewIngredients);
        com.google.android.material.button.MaterialButton btnStartCooking = sheetView
                .findViewById(R.id.btnStartCooking);
        com.google.android.material.button.MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancel);

        tvTitle.setText(recipe.getName());
        tvTime.setText(recipe.getTotalTime() != null ? recipe.getTotalTime() : "N/A");
        tvIngredientsCount
                .setText((recipe.getIngredients() != null ? recipe.getIngredients().size() : 0) + " Ingredients");

        btnStartCooking.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            android.content.Intent intent = new android.content.Intent(getContext(), RecipeActivity.class);
            // Pass JSON to avoid Serializable issues
            String json = new com.google.gson.Gson().toJson(recipe);
            intent.putExtra("recipe_json", json);
            startActivity(intent);
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }
}
