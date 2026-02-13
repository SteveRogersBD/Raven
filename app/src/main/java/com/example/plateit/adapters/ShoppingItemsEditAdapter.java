package com.example.plateit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plateit.R;
import com.example.plateit.responses.ShoppingListItem;

import java.util.List;

public class ShoppingItemsEditAdapter extends RecyclerView.Adapter<ShoppingItemsEditAdapter.ViewHolder> {

    private List<ShoppingListItem> items;

    public ShoppingItemsEditAdapter(List<ShoppingListItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping_list_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingListItem item = items.get(position);

        // Remove old listener before setting text to avoid triggering it
        if (holder.textWatcher != null) {
            holder.etName.removeTextChangedListener(holder.textWatcher);
        }

        holder.etName.setText(item.getName());

        holder.textWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                item.setName(s.toString());
            }
        };
        holder.etName.addTextChangedListener(holder.textWatcher);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<ShoppingListItem> getItems() {
        return items;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        android.widget.EditText etName;
        android.text.TextWatcher textWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            etName = itemView.findViewById(R.id.etItemName);
        }
    }
}
