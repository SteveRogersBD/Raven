package com.example.plateit.models;

import java.io.Serializable;

public class ChatSession implements Serializable {
    private String id;
    private String user_id;
    private String title;
    private String created_at;
    private String updated_at;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUpdatedAt() {
        return updated_at;
    }
}
