package com.example.plateit;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plateit.adapters.ChatAdapter;
import com.example.plateit.models.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChatMessages;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private EditText etChatMessage;
    private ImageButton btnAttachImage;
    private ImageButton btnSendMessage;

    private Uri pendingImageUri = null;
    private String currentThreadId = java.util.UUID.randomUUID().toString();
    private boolean isPro = false;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pendingImageUri = uri;
                    // Provide feedback that image is selected
                    btnAttachImage.setColorFilter(getColor(R.color.app_primary));
                    Toast.makeText(this, "Image attached!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Standardized AppBar Setup
        com.example.plateit.utils.AppBarHelper.setup(this, "Chef Intelligence", true, R.drawable.ic_history,
                this::showHistoryDialog);

        // Views
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnAttachImage = findViewById(R.id.btnAttachImage);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        // Check Pro Status
        refreshProStatus();

        // Setup RecyclerView
        messageList = new ArrayList<>();
        // Add a welcome message if list is empty
        if (messageList.isEmpty()) {
            messageList.add(new ChatMessage(
                    "Hello! I'm your AI Chef. Ask me anything about cooking or show me your ingredients!", false));
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        rvChatMessages.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(messageList);
        rvChatMessages.setAdapter(chatAdapter);

        // Actions
        btnAttachImage.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProStatus();
    }

    private void refreshProStatus() {
        com.example.plateit.utils.TokenManager.getInstance(this).isPro(pro -> {
            runOnUiThread(() -> isPro = pro);
        });
    }

    private void sendMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty() && pendingImageUri == null) {
            return;
        }

        // Check Monetization
        com.example.plateit.utils.SessionManager sessionManager = new com.example.plateit.utils.SessionManager(this);

        // Synchronous check: Trust TokenManager's instant state (Cached + Token
        // Fallback)
        if (!isPro && com.example.plateit.utils.TokenManager.getInstance(this).isPro()) {
            isPro = true;
        }

        if (!isPro) {
            if (sessionManager.hasUsedFreeChat()) {
                android.content.Intent intent = new android.content.Intent(this, PaywallActivity.class);
                startActivity(intent);

                // Show teaser message
                messageList.add(new ChatMessage("Upgrade to Pro to act like a Master Chef!", false));
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChatMessages.scrollToPosition(messageList.size() - 1);
                return;
            }
            sessionManager.setHasUsedFreeChat(true);
        }

        // 1. Create User Message UI
        ChatMessage userMsg = new ChatMessage(text, true, pendingImageUri);
        messageList.add(userMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.scrollToPosition(messageList.size() - 1);

        // 2. Add "..." placeholder for AI
        final ChatMessage typingMsg = new ChatMessage("...", false);
        messageList.add(typingMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChatMessages.scrollToPosition(messageList.size() - 1);

        // 3. Prepare Data
        String imageBase64 = null;
        if (pendingImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(pendingImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                // Compress and Encode
                java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                imageBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }

        // 4. Reset Inputs
        etChatMessage.setText("");
        pendingImageUri = null;
        btnAttachImage.setColorFilter(getColor(R.color.gray_600));

        // 5. API Call
        // SessionManager already instantiated above
        String userId = sessionManager.getUserId();

        com.example.plateit.requests.ChatRequest req = new com.example.plateit.requests.ChatRequest(
                text,
                currentThreadId,
                userId,
                null,
                0,
                imageBase64);

        com.example.plateit.api.RetrofitClient.getAgentService().chat(req)
                .enqueue(new retrofit2.Callback<com.example.plateit.responses.ChatResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.plateit.responses.ChatResponse> call,
                            retrofit2.Response<com.example.plateit.responses.ChatResponse> response) {

                        // Remove "..." placeholder safely
                        int currentPos = messageList.indexOf(typingMsg);
                        if (currentPos != -1) {
                            messageList.remove(currentPos);
                            chatAdapter.notifyItemRemoved(currentPos);
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            com.example.plateit.responses.ChatResponse resp = response.body();

                            ChatMessage aiMsg = new ChatMessage(
                                    resp.getChatBubble(),
                                    resp.getUiType() != null ? resp.getUiType() : "none",
                                    resp.getRecipeData(),
                                    resp.getIngredientData(),
                                    resp.getVideoData());

                            messageList.add(aiMsg);
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
                        } else {
                            messageList.add(new ChatMessage("Sorry, I'm having trouble connecting.", false));
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.plateit.responses.ChatResponse> call,
                            Throwable t) {
                        int currentPos = messageList.indexOf(typingMsg);
                        if (currentPos != -1) {
                            messageList.remove(currentPos);
                            chatAdapter.notifyItemRemoved(currentPos);
                        }
                        messageList.add(new ChatMessage("Network error: " + t.getMessage(), false));
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        rvChatMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_history) {
            showHistoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHistoryDialog() {
        com.example.plateit.utils.SessionManager sm = new com.example.plateit.utils.SessionManager(this);
        com.example.plateit.api.RetrofitClient.getAgentService().getChatSessions(sm.getUserId())
                .enqueue(new retrofit2.Callback<List<com.example.plateit.models.ChatSession>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<com.example.plateit.models.ChatSession>> call,
                            retrofit2.Response<List<com.example.plateit.models.ChatSession>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<com.example.plateit.models.ChatSession> sessions = response.body();
                            if (sessions.isEmpty()) {
                                Toast.makeText(ChatActivity.this, "No history found", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String[] titles = new String[sessions.size()];
                            for (int i = 0; i < sessions.size(); i++) {
                                titles[i] = sessions.get(i).getTitle();
                            }

                            new androidx.appcompat.app.AlertDialog.Builder(ChatActivity.this)
                                    .setTitle("Chat History")
                                    .setItems(titles, (dialog, which) -> {
                                        currentThreadId = sessions.get(which).getId();
                                        loadChatHistory(currentThreadId);
                                    })
                                    .show();
                        } else {
                            Toast.makeText(ChatActivity.this, "No history found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<List<com.example.plateit.models.ChatSession>> call,
                            Throwable t) {
                        Toast.makeText(ChatActivity.this, "Error fetching history", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadChatHistory(String threadId) {
        com.example.plateit.api.RetrofitClient.getAgentService().getChatHistory(threadId)
                .enqueue(new retrofit2.Callback<List<com.example.plateit.responses.ChatHistoryResponse>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<List<com.example.plateit.responses.ChatHistoryResponse>> call,
                            retrofit2.Response<List<com.example.plateit.responses.ChatHistoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            messageList.clear();
                            for (com.example.plateit.responses.ChatHistoryResponse item : response.body()) {
                                ChatMessage msg = new ChatMessage(
                                        item.getContent(),
                                        item.getUiType() != null ? item.getUiType() : "none",
                                        item.getRecipeData(),
                                        item.getIngredientData(),
                                        item.getVideoData());
                                if ("user".equals(item.getSender())) {
                                    msg.setUser(true);
                                }
                                messageList.add(msg);
                            }
                            chatAdapter.notifyDataSetChanged();
                            if (!messageList.isEmpty()) {
                                rvChatMessages.scrollToPosition(messageList.size() - 1);
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<List<com.example.plateit.responses.ChatHistoryResponse>> call,
                            Throwable t) {
                        Toast.makeText(ChatActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
