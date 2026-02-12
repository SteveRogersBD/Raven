package com.example.plateit.requests;

import com.example.plateit.models.Recipe;

public class ChatRequest {
    private String message;
    private String thread_id;
    private String user_id; // Added user_id for persistence
    private Recipe recipe;
    private int current_step;
    private String image_data;

    public ChatRequest(String message, String thread_id, String user_id, Recipe recipe, int current_step,
            String image_data) {
        this.message = message;
        this.thread_id = thread_id;
        this.user_id = user_id;
        this.recipe = recipe;
        this.current_step = current_step;
        this.image_data = image_data;
    }
}
