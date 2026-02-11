package com.example.plateit.requests;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class CookbookEntryCreate {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("title")
    private String title;

    @SerializedName("recipe_data")
    private Object recipeData; // Store as generic object or Map

    @SerializedName("source_url")
    private String sourceUrl;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    public CookbookEntryCreate(String userId, String title, Object recipeData, String sourceUrl, String thumbnailUrl) {
        this.userId = userId;
        this.title = title;
        this.recipeData = recipeData;
        this.sourceUrl = sourceUrl;
        this.thumbnailUrl = thumbnailUrl;
    }
}
