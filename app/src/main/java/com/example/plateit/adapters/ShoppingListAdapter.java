package com.example.plateit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.plateit.R;
import com.example.plateit.responses.ShoppingList;
import com.example.plateit.responses.ShoppingListItem;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {

    private List<ShoppingList> shoppingLists;
    private OnListClickListener listener;

    public interface OnListClickListener {
        void onListClick(ShoppingList list);

        void onListDelete(ShoppingList list);
    }

    public ShoppingListAdapter(List<ShoppingList> shoppingLists, OnListClickListener listener) {
        this.shoppingLists = shoppingLists;
        this.listener = listener;
    }

    public void updateData(List<ShoppingList> newList) {
        this.shoppingLists = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingList list = shoppingLists.get(position);
        holder.tvTitle.setText(list.getTitle());

        int total = list.getItems().size();
        int bought = 0;
        for (ShoppingListItem item : list.getItems()) {
            if (item.isBought())
                bought++;
        }

        holder.tvStats.setText(bought + "/" + total + " items bought");

        holder.itemView.setOnClickListener(v -> listener.onListClick(list));
        holder.btnDelete.setOnClickListener(v -> listener.onListDelete(list));
    }

    @Override
    public int getItemCount() {
        return shoppingLists != null ? shoppingLists.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvStats;
        View btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvListTitle);
            tvStats = itemView.findViewById(R.id.tvListStats);
            btnDelete = itemView.findViewById(R.id.btnDeleteList);
        }
    }
}
