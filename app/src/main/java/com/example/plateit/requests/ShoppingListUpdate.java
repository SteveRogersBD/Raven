package com.example.plateit.requests;

import com.example.plateit.responses.ShoppingListItem;
import java.util.List;

public class ShoppingListUpdate {
    private String title;
    private List<ShoppingListItem> items;

    public ShoppingListUpdate(String title, List<ShoppingListItem> items) {
        this.title = title;
        this.items = items;
    }
}
