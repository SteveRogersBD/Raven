package com.example.plateit.responses;

import com.google.gson.annotations.SerializedName;
import com.example.plateit.models.Recipe;

public class CookbookEntry {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("recipe_data")
    private Recipe recipeData;

    @SerializedName("source_url")
    private String sourceUrl;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Recipe getRecipeData() {
        return recipeData;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
