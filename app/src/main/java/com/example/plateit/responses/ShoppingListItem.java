package com.example.plateit.responses;

import com.google.gson.annotations.SerializedName;

public class ShoppingListItem {
    @SerializedName("name")
    private String name;

    @SerializedName("amount")
    private String amount;

    @SerializedName("bought")
    private boolean bought;

    public ShoppingListItem(String name, String amount, boolean bought) {
        this.name = name;
        this.amount = amount;
        this.bought = bought;
    }

    public String getName() {
        return name;
    }

    public String getAmount() {
        return amount;
    }

    public boolean isBought() {
        return bought;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBought(boolean bought) {
        this.bought = bought;
    }
}
