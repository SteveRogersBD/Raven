package com.example.plateit.responses;

import com.google.gson.annotations.SerializedName;

public class CookingSession {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("cookbook_id")
    private Integer cookbookId;

    @SerializedName("current_step_index")
    private int currentStepIndex;

    @SerializedName("is_finished")
    private boolean isFinished;

    public int getId() {
        return id;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public Integer getCookbookId() {
        return cookbookId;
    }

    public boolean isFinished() {
        return isFinished;
    }
}
