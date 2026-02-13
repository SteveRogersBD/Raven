package com.example.plateit.requests;

import com.example.plateit.responses.ShoppingListItem;
import java.util.List;

public class ShoppingListCreateRequest {
    private String user_id;
    private String title;
    private List<ShoppingListItem> items;

    public ShoppingListCreateRequest(String user_id, String title, List<ShoppingListItem> items) {
        this.user_id = user_id;
        this.title = title;
        this.items = items;
    }
}
