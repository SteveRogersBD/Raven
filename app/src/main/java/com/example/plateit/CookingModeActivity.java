package com.example.plateit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.plateit.adapters.CookingStepsAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CookingModeActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private com.airbnb.lottie.LottieAnimationView apiLoadingIndicator;
    private TextView tvStepProgress;
    private TextView tvRecipeName;
    private CardView cvSourceCard;
    private ImageView imgSourceThumbnail;
    private TextView tvSourceText;
    private List<com.example.plateit.models.RecipeStep> steps;
    // Data
    private com.example.plateit.models.Recipe currentRecipe;

    // Assistant UI
    private FloatingActionButton btnMic;
    private ImageButton btnCamera, btnKeyboard;
    private CardView cvAssistantResponse;
    private TextView tvAssistantText;
    private android.view.View userQueryContainer;
    private ImageView imgUserQuery;
    private TextView tvUserQuery;

    // Voice & TTS
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isListening = false;

    // Dynamic UI
    private androidx.recyclerview.widget.RecyclerView rvRecipeList, rvIngredientList, rvVideoList;

    // Session
    private int sessionId = -1;
    private com.example.plateit.utils.SessionManager sessionManager;

    private void startCookingSession(int cookbookId) {
        String userId = sessionManager.getUserId();
        if (userId == null)
            return;

        com.example.plateit.requests.CookingSessionCreate req = new com.example.plateit.requests.CookingSessionCreate(
                userId, cookbookId);
        com.example.plateit.api.RetrofitClient.getAgentService().startCookingSession(req)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.CookingSession>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.plateit.responses.CookingSession> call,
                            retrofit2.Response<com.example.plateit.responses.CookingSession> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            sessionId = response.body().getId();
                            android.util.Log.d("PlateIt", "Cooking Session Started: " + sessionId);
                        } else {
                            android.util.Log.e("PlateIt", "Session Start Failed: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.plateit.responses.CookingSession> call,
                            Throwable t) {
                        // Fail silently
                    }
                });
    }

    private void updateCookingSession(int stepIndex, boolean isFinished) {
        com.example.plateit.requests.CookingProgressUpdate req = new com.example.plateit.requests.CookingProgressUpdate(
                sessionId, stepIndex, isFinished);
        com.example.plateit.api.RetrofitClient.getAgentService().updateCookingProgress(req)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.CookingSession>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.plateit.responses.CookingSession> call,
                            retrofit2.Response<com.example.plateit.responses.CookingSession> response) {
                        // Success
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.plateit.responses.CookingSession> call,
                            Throwable t) {
                        // Fail silently
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // androidx.activity.EdgeToEdge.enable(this); // Disabled to restore standard
        // bars
        setContentView(R.layout.activity_cooking_mode);

        // setContentView(R.layout.activity_cooking_mode); already called above

        sessionManager = new com.example.plateit.utils.SessionManager(this);
        int existingSessionId = getIntent().getIntExtra("session_id", -1);
        int initialStep = getIntent().getIntExtra("initial_step", 0);

        if (existingSessionId != -1) {
            this.sessionId = existingSessionId;
        } else {
            int cookbookId = getIntent().getIntExtra("cookbook_id", -1);
            if (cookbookId != -1) {
                startCookingSession(cookbookId);
            }
        }

        // Get Recipe from Intent (JSON Mode)
        try {
            String json = getIntent().getStringExtra("recipe_json");
            if (json != null) {
                currentRecipe = new com.google.gson.Gson().fromJson(json, com.example.plateit.models.Recipe.class);
            }
            // Check for legacy object passing just in case (optional, can remove)
            if (currentRecipe == null) {
                currentRecipe = (com.example.plateit.models.Recipe) getIntent().getSerializableExtra("recipe_object");
            }

        } catch (Exception e) {
            android.util.Log.e("CookingMode", "Error parsing recipe JSON", e);
        }

        if (currentRecipe != null) {
            steps = currentRecipe.getSteps();
        } else {
            // Fallback for old intents or legacy usage
            // (Keeping this block but it likely won't be hit if JSON works)
            android.util.Log.d("CookingMode", "Checking for legacy string list");
            ArrayList<String> stringSteps = getIntent().getStringArrayListExtra("steps_list");
            steps = new ArrayList<>();
            if (stringSteps != null) {
                for (String s : stringSteps) {
                    steps.add(new com.example.plateit.models.RecipeStep(s, null, null));
                }
                android.util.Log.d("CookingMode", "Legacy steps found: " + steps.size());
            }
        }

        if (steps == null || steps.isEmpty()) {
            android.util.Log.e("CookingMode", "No steps found!");
            // finish();
            // Create a dummy step to prevent crash if that's what keeps it open
            steps = new ArrayList<>();
            steps.add(new com.example.plateit.models.RecipeStep("No steps available.", null, null));
        }

        viewPager = findViewById(R.id.viewPagerSteps);
        progressBar = findViewById(R.id.progressBar);
        apiLoadingIndicator = findViewById(R.id.apiLoadingIndicator);
        tvStepProgress = findViewById(R.id.tvStepProgress);
        tvRecipeName = findViewById(R.id.tvRecipeName);
        if (currentRecipe != null) {
            tvRecipeName.setText(currentRecipe.getName());
        }

        cvSourceCard = findViewById(R.id.cvSourceCard);
        imgSourceThumbnail = findViewById(R.id.imgSourceThumbnail);
        tvSourceText = findViewById(R.id.tvSourceText);
        View btnPrevious = findViewById(R.id.btnPrevious);
        View btnNext = findViewById(R.id.btnNext);

        // Assistant UI
        btnMic = findViewById(R.id.btnMic);
        btnCamera = findViewById(R.id.btnCamera);
        btnKeyboard = findViewById(R.id.btnKeyboard);
        cvAssistantResponse = findViewById(R.id.cvAssistantResponse);
        tvAssistantText = findViewById(R.id.tvAssistantText);
        userQueryContainer = findViewById(R.id.userQueryContainer);
        imgUserQuery = findViewById(R.id.imgUserQuery);
        tvUserQuery = findViewById(R.id.tvUserQuery);

        // Dynamic Lists
        rvRecipeList = findViewById(R.id.rvRecipeList);
        rvIngredientList = findViewById(R.id.rvIngredientList);
        rvVideoList = findViewById(R.id.rvVideoList);

        // Setup LayoutManagers
        rvRecipeList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        // Use Grid for Ingredients to save space if needed, or vertical linear
        rvIngredientList.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        rvVideoList.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));

        // Setup ViewPager
        try {
            CookingStepsAdapter adapter = new CookingStepsAdapter(steps);
            viewPager.setAdapter(adapter);
            // More dramatic zoom/fade transformer
            viewPager.setPageTransformer((page, position) -> {
                float absPos = Math.abs(position);
                page.setAlpha(1 - absPos * 0.5f);
                page.setScaleY(0.85f + (1 - absPos) * 0.15f);
            });
        } catch (Exception e) {
        }

        // Set initial step if resuming
        if (initialStep > 0 && initialStep < steps.size()) {
            viewPager.setCurrentItem(initialStep, false);
            updateProgress(initialStep); // Ensure UI reflects step
        }

        // Show ingredients immediately - COMMENTED OUT to preventing blocking steps
        /*
         * if (currentRecipe != null && currentRecipe.getIngredients() != null) {
         * com.example.plateit.adapters.IngredientsAdapter ingAdapter = new
         * com.example.plateit.adapters.IngredientsAdapter(
         * currentRecipe.getIngredients());
         * rvIngredientList.setAdapter(ingAdapter);
         * rvIngredientList.setVisibility(View.VISIBLE);
         * 
         * }
         */

        if (currentRecipe != null && cvSourceCard != null) {
            String sourceUrl = currentRecipe.getSource();
            String sourceImage = currentRecipe.getSourceImage();

            // Check if source is valid and http based (not local file path)
            if (sourceUrl != null && !sourceUrl.isEmpty() && sourceUrl.startsWith("http")) {
                cvSourceCard.setVisibility(View.VISIBLE);

                // Set Image
                if (sourceImage != null && !sourceImage.isEmpty()) {
                    com.squareup.picasso.Picasso.get()
                            .load(sourceImage)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .fit()
                            .into(imgSourceThumbnail);
                } else {
                    // Placeholder if no image
                    imgSourceThumbnail.setImageResource(R.drawable.ic_launcher_background);
                }

                // Set Text based on Source Type
                if (sourceUrl.contains("youtube") || sourceUrl.contains("youtu.be")) {
                    tvSourceText.setText("Watch Video on YouTube");
                } else {
                    tvSourceText.setText("View Original Recipe");
                }

                cvSourceCard.setOnClickListener(v -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(sourceUrl));
                    startActivity(browserIntent);
                });

            } else {
                cvSourceCard.setVisibility(View.GONE);
            }
        }

        updateProgress(initialStep);

        // ViewPager Listeners
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateProgress(position);
                if (sessionId != -1) {
                    boolean isFinished = (position == steps.size() - 1);
                    updateCookingSession(position, isFinished);
                }
            }
        });

        // Navigation Listeners
        btnPrevious.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0)
                viewPager.setCurrentItem(current - 1);
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < steps.size() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finish();
            }
        });

        // Removed the old btnClose.setOnClickListener here as it's moved above.

        // --- Assistant Logic ---
        try {
            initializeTextToSpeech();
            initializeSpeechRecognizer();
        } catch (Exception e) {
            android.util.Log.e("CookingMode", "Voice Init Failed", e);
        }

        btnMic.setOnClickListener(v -> {
            if (checkPermission()) {
                if (!isListening) {
                    startListening();
                } else {
                    stopListening();
                }
            } else {
                requestPermission();
            }
        });

        btnKeyboard.setOnClickListener(v -> showKeyboardInput());
        btnCamera.setOnClickListener(v -> showCameraInput());
    }

    // --- Inputs: Keyboard & Camera ---

    private void showKeyboardInput() {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);

        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_chat_input, null);
        sheet.setContentView(view);

        com.google.android.material.textfield.TextInputEditText etQuery = view.findViewById(R.id.etQuery);
        com.google.android.material.button.MaterialButton btnSend = view.findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> {
            String q = etQuery.getText().toString().trim();
            if (!q.isEmpty()) {
                handleUserQuery(q);
                sheet.dismiss();
            }
        });

        sheet.show();
    }

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int GALLERY_REQUEST_CODE = 201;

    private void showCameraInput() {
        showVisionOptions();
    }

    private void showVisionOptions() {
        String[] options = { "Take Photo", "Choose from Gallery" };
        new android.app.AlertDialog.Builder(this)
                .setTitle("Assistant Vision")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .show();
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
        } catch (android.content.ActivityNotFoundException e) {
            // Camera not found
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST_CODE);
    }

    private void promptForTextWithImage(android.graphics.Bitmap bitmap) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("What should I look for?");

        final EditText input = new EditText(this);
        input.setHint("e.g. Does the crust look done?");
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("Ask Assistant", (dialog, which) -> {
            String q = input.getText().toString().trim();
            if (q.isEmpty())
                q = "Analyze this photo of my cooking.";
            handleUserQuery(q, bitmap);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // --- Logic Stub ---

    private void handleUserQuery(String query) {
        handleUserQuery(query, null);
    }

    private void handleUserQuery(String query, android.graphics.Bitmap image) {
        if (currentRecipe == null) {
            return;
        }

        setApiLoading(true);
        // Toast.makeText(this, "Thinking...", Toast.LENGTH_SHORT).show();

        int currentStepIndex = viewPager.getCurrentItem();
        String imageBase64 = null;

        // Display user query in UI
        cvAssistantResponse.setVisibility(View.VISIBLE);
        userQueryContainer.setVisibility(View.VISIBLE);
        if (image != null) {
            imgUserQuery.setVisibility(View.VISIBLE);
            imgUserQuery.setImageBitmap(image);
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            imageBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
        } else {
            imgUserQuery.setVisibility(View.GONE);
        }
        tvUserQuery.setText(query);
        tvAssistantText.setText("Thinking...");
        // The original code had an extra closing brace here, which was syntactically
        // incorrect.
        // It has been removed to ensure correctness.
        // The imageBase64 conversion was also inside an incorrect block. Moved it
        // inside the `if (image != null)` block.

        String userId = sessionManager.getUserId();
        String threadId = (sessionId != -1) ? "cooking_" + sessionId : "chat_" + userId;

        com.example.plateit.requests.ChatRequest req = new com.example.plateit.requests.ChatRequest(
                query,
                threadId,
                userId,
                currentRecipe,
                currentStepIndex,
                imageBase64);

        // Use AgentApiService
        com.example.plateit.api.RetrofitClient.getAgentService().chat(req)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.ChatResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.plateit.responses.ChatResponse> call,
                            retrofit2.Response<com.example.plateit.responses.ChatResponse> response) {
                        setApiLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.plateit.responses.ChatResponse resp = response.body();

                            // 1. Show Text
                            String reply = resp.getChatBubble();
                            cvAssistantResponse.setVisibility(View.VISIBLE);
                            tvAssistantText.setText(reply);

                            // 2. Handle Dynamic UI
                            String type = resp.getUiType();
                            rvRecipeList.setVisibility(View.GONE);
                            rvIngredientList.setVisibility(View.GONE);
                            rvVideoList.setVisibility(View.GONE);

                            if ("recipe_list".equals(type) && resp.getRecipeData() != null) {
                                com.example.plateit.adapters.RecipeCardAdapter adapter = new com.example.plateit.adapters.RecipeCardAdapter(
                                        resp.getRecipeData().getItems(),
                                        recipe -> {
                                            showRecipePreviewSheet(recipe);
                                        });
                                rvRecipeList.setAdapter(adapter);
                                rvRecipeList.setVisibility(View.VISIBLE);

                            } else if ("ingredient_list".equals(type) && resp.getIngredientData() != null) {
                                // Convert items
                                List<com.example.plateit.models.Ingredient> converted = new ArrayList<>();
                                for (com.example.plateit.responses.ChatResponse.IngredientItem item : resp
                                        .getIngredientData().getItems()) {
                                    converted.add(new com.example.plateit.models.Ingredient(
                                            item.getName(), item.getAmount(), item.getImage()));
                                }
                                com.example.plateit.adapters.IngredientsAdapter adapter = new com.example.plateit.adapters.IngredientsAdapter(
                                        converted);
                                rvIngredientList.setAdapter(adapter);
                                rvIngredientList.setVisibility(View.VISIBLE);

                            } else if ("video_list".equals(type) && resp.getVideoData() != null) {
                                // Convert items
                                List<RecipeVideo> converted = new ArrayList<>();
                                for (com.example.plateit.responses.ChatResponse.VideoItem item : resp.getVideoData()
                                        .getItems()) {
                                    converted.add(new RecipeVideo(
                                            item.getTitle(), item.getUrl(), item.getThumbnail(), "", "", ""));
                                }
                                VideoAdapter adapter = new VideoAdapter(converted, true, video -> {
                                    // Handle video click (e.g. open Intent)
                                    Intent intent = new Intent(Intent.ACTION_VIEW,
                                            android.net.Uri.parse(video.getLink()));
                                    startActivity(intent);
                                });
                                rvVideoList.setAdapter(adapter);
                                rvVideoList.setVisibility(View.VISIBLE);
                            }

                            speak(reply);
                        } else {
                            // Server rejected request
                        }
                    }

                    public void onFailure(retrofit2.Call<com.example.plateit.responses.ChatResponse> call,
                            Throwable t) {
                        setApiLoading(false);
                    }
                });
    }

    private void showRecipePreviewSheet(com.example.plateit.responses.ChatResponse.RecipeCard recipe) {
        com.google.android.material.bottomsheet.BottomSheetDialog sheet = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_recipe_preview, null);
        sheet.setContentView(view);

        // Populate views
        ImageView imgPreview = view.findViewById(R.id.imgRecipePreview);
        TextView tvTitle = view.findViewById(R.id.tvRecipeTitle);
        TextView tvTime = view.findViewById(R.id.tvReadyTime);
        com.google.android.material.button.MaterialButton btnViewFull = view.findViewById(R.id.btnViewFullRecipe);
        com.google.android.material.button.MaterialButton btnStartCooking = view.findViewById(R.id.btnStartCooking);

        tvTitle.setText(recipe.getTitle());
        if (recipe.getReadyInMinutes() != null) {
            tvTime.setText("Ready in " + recipe.getReadyInMinutes() + " mins");
        } else {
            tvTime.setVisibility(View.GONE);
        }

        if (recipe.getImageUrl() != null) {
            com.squareup.picasso.Picasso.get().load(recipe.getImageUrl()).into(imgPreview);
        }

        // View Full Recipe button
        btnViewFull.setOnClickListener(v -> {
            if (recipe.getSourceUrl() != null && !recipe.getSourceUrl().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(recipe.getSourceUrl()));
                startActivity(browserIntent);
                sheet.dismiss();
            } else {
                // Source not available
            }
        });

        // Start Cooking button
        btnStartCooking.setOnClickListener(v -> {
            sheet.dismiss();
            fetchAndStartRecipe(recipe.getId());
        });

        sheet.show();
    }

    private void fetchAndStartRecipe(int recipeId) {
        // Toast.makeText(this, "Loading recipe...", Toast.LENGTH_SHORT).show();
        setApiLoading(true);

        com.example.plateit.api.RetrofitClient.getAgentService().getRecipeDetails(recipeId)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.RecipeResponse>() {
                    public void onResponse(retrofit2.Call<com.example.plateit.responses.RecipeResponse> call,
                            retrofit2.Response<com.example.plateit.responses.RecipeResponse> response) {
                        setApiLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            com.example.plateit.responses.RecipeResponse recipeResp = response.body();

                            // Convert to Recipe model
                            com.example.plateit.models.Recipe recipe = new com.example.plateit.models.Recipe(
                                    recipeResp.getName(),
                                    recipeResp.getSteps(),
                                    recipeResp.getIngredients(),
                                    recipeResp.getSourceUrl(),
                                    recipeResp.getSourceImage());

                            // Restart CookingModeActivity with new recipe
                            Intent intent = new Intent(CookingModeActivity.this, CookingModeActivity.class);
                            intent.putExtra("recipe_object", recipe);
                            finish(); // Close current activity
                            startActivity(intent);
                        } else {
                            // Failed to load
                        }
                    }

                    public void onFailure(retrofit2.Call<com.example.plateit.responses.RecipeResponse> call,
                            Throwable t) {
                        setApiLoading(false);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                android.os.Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey("data")) {
                    android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        promptForTextWithImage(imageBitmap);
                    }
                } else {
                    // Failed to capture
                }
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                android.net.Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media
                                .getBitmap(this.getContentResolver(), selectedImage);
                        promptForTextWithImage(bitmap);
                    } catch (java.io.IOException e) {
                        // Error loading
                    }
                }
            }
        }
    }

    // --- Voice Recognition ---

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                btnMic.setImageTintList(getColorStateList(android.R.color.holo_red_light));
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                stopListening();
            }

            @Override
            public void onError(int error) {
                stopListening();
                // Toast.makeText(CookingModeActivity.this, "Error: " + error,
                // Toast.LENGTH_SHORT).show();
                btnMic.setImageTintList(getColorStateList(R.color.black));
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (data != null && !data.isEmpty()) {
                    String spokenText = data.get(0);
                    handleUserQuery(spokenText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    private void startListening() {
        isListening = true;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
    }

    private void stopListening() {
        isListening = false;
        speechRecognizer.stopListening();
        // Reset color logic if needed, simplifed here
        btnMic.setImageTintList(getColorStateList(R.color.black));
    }

    // --- TTS ---

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
            }
        });
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // --- Permissions ---

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.RECORD_AUDIO }, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
            } else {
                // Permission Denied
            }
        }
    }

    private void applyWindowInsets() {
        View CompatView = findViewById(R.id.topBar);
        if (CompatView != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(CompatView, (v, insets) -> {
                androidx.core.graphics.Insets statusBars = insets
                        .getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
                // Only pad the top, keep the rest edge-to-edge
                v.setPadding(v.getPaddingLeft(), statusBars.top + v.getPaddingTop(), v.getPaddingRight(),
                        v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void setApiLoading(boolean isLoading) {
        if (apiLoadingIndicator != null) {
            if (isLoading) {
                apiLoadingIndicator.setVisibility(View.VISIBLE);
                apiLoadingIndicator.playAnimation();
            } else {
                apiLoadingIndicator.cancelAnimation();
                apiLoadingIndicator.setVisibility(View.GONE);
            }
        }
    }

    // --- Helpers ---

    private void updateProgress(int position) {
        int total = steps.size();
        int current = position + 1;
        tvStepProgress.setText("Step " + current + " of " + total);
        if (total > 0) {
            int progress = (int) ((current / (float) total) * 100);
            progressBar.setProgress(progress);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    // Animation Class
    public class ZoomOutPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) {
                view.setAlpha(0f);
            } else if (position <= 1) {
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            } else {
                view.setAlpha(0f);
            }
        }
    }
}
