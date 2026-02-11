package com.example.plateit.requests;

import com.google.gson.annotations.SerializedName;

public class CookingProgressUpdate {
    @SerializedName("session_id")
    private int sessionId;

    @SerializedName("current_step_index")
    private int currentStepIndex;

    @SerializedName("is_finished")
    private boolean isFinished;

    public CookingProgressUpdate(int sessionId, int currentStepIndex, boolean isFinished) {
        this.sessionId = sessionId;
        this.currentStepIndex = currentStepIndex;
        this.isFinished = isFinished;
    }
}
