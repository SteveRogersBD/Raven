package com.example.plateit.responses;

public class UserStatsResponse {
    private int total_recipes;
    private int total_sessions;
    private int finished_sessions;
    private int unfinished_sessions;
    private int active_days;

    public int getTotalRecipes() {
        return total_recipes;
    }

    public int getTotalSessions() {
        return total_sessions;
    }

    public int getFinishedSessions() {
        return finished_sessions;
    }

    public int getUnfinishedSessions() {
        return unfinished_sessions;
    }

    public int getActiveDays() {
        return active_days;
    }
}
