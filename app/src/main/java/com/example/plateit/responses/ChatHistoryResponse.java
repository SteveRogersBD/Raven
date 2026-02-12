package com.example.plateit.responses;

public class ChatHistoryResponse {
    private String sender;
    private String content;
    private String ui_type;
    private com.example.plateit.responses.ChatResponse.RecipeListPayload recipe_data;
    private com.example.plateit.responses.ChatResponse.IngredientListPayload ingredient_data;
    private com.example.plateit.responses.ChatResponse.VideoListPayload video_data;
    private String created_at;

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getUiType() {
        return ui_type;
    }

    public com.example.plateit.responses.ChatResponse.RecipeListPayload getRecipeData() {
        return recipe_data;
    }

    public com.example.plateit.responses.ChatResponse.IngredientListPayload getIngredientData() {
        return ingredient_data;
    }

    public com.example.plateit.responses.ChatResponse.VideoListPayload getVideoData() {
        return video_data;
    }

    public String getCreated_at() {
        return created_at;
    }
}
