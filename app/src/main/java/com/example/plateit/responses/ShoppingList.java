package com.example.plateit.responses;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ShoppingList {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("title")
    private String title;

    @SerializedName("items")
    private List<ShoppingListItem> items;

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public List<ShoppingListItem> getItems() {
        return items;
    }
}
