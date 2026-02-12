package com.example.plateit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.plateit.R;
import com.example.plateit.responses.ChatResponse;
import com.squareup.picasso.Picasso;
import java.util.List;

public class RecipeCardAdapter extends RecyclerView.Adapter<RecipeCardAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ChatResponse.RecipeCard recipe);
    }

    private List<ChatResponse.RecipeCard> recipes;
    private OnItemClickListener listener;

    public RecipeCardAdapter(List<ChatResponse.RecipeCard> recipes, OnItemClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_rich_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatResponse.RecipeCard recipe = recipes.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onItemClick(recipe);
        });

        holder.tvTitle.setText(recipe.getTitle());
        holder.tvBadge.setText("RECIPE");
        holder.tvBadge.setVisibility(View.VISIBLE);

        if (recipe.getReadyInMinutes() != null) {
            holder.tvFooter.setText(recipe.getReadyInMinutes() + " mins");
        } else {
            holder.tvFooter.setText("View Details");
        }

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(recipe.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgThumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        TextView tvBadge;
        TextView tvTitle;
        TextView tvFooter;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.imgCardThumbnail);
            tvBadge = itemView.findViewById(R.id.tvCardBadge);
            tvTitle = itemView.findViewById(R.id.tvCardTitle);
            tvFooter = itemView.findViewById(R.id.tvCardFooter);
        }
    }
}
