package com.example.plateit.requests;

import com.google.gson.annotations.SerializedName;

public class CookingSessionCreate {
    @SerializedName("user_id")
    private String userId;

    @SerializedName("cookbook_id")
    private Integer cookbookId;

    public CookingSessionCreate(String userId, Integer cookbookId) {
        this.userId = userId;
        this.cookbookId = cookbookId;
    }
}
