package com.example.plateit.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.plateit.R;
import com.example.plateit.responses.CookbookEntry;
import com.squareup.picasso.Picasso;
import java.util.List;

public class CookbookAdapter extends RecyclerView.Adapter<CookbookAdapter.ViewHolder> {

    private List<CookbookEntry> recipes;
    private OnRecipeClickListener listener;
    private OnRecipeDeleteListener deleteListener;

    public interface OnRecipeClickListener {
        void onRecipeClick(CookbookEntry recipe);
    }

    public interface OnRecipeDeleteListener {
        void onRecipeDelete(CookbookEntry recipe);
    }

    public CookbookAdapter(List<CookbookEntry> recipes, OnRecipeClickListener listener,
            OnRecipeDeleteListener deleteListener) {
        this.recipes = recipes;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cookbook_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CookbookEntry recipe = recipes.get(position);
        holder.title.setText(recipe.getTitle());
        holder.subtitle.setText("Saved Recipe"); // Or time if available

        if (recipe.getThumbnailUrl() != null && !recipe.getThumbnailUrl().isEmpty()) {
            Picasso.get().load(recipe.getThumbnailUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onRecipeDelete(recipe);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onRecipeDelete(recipe);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateData(List<CookbookEntry> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle, channel;
        ImageView thumbnail;
        android.widget.ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.videoTitle);
            subtitle = itemView.findViewById(R.id.videoTime);
            thumbnail = itemView.findViewById(R.id.videoThumbnail);
            btnDelete = itemView.findViewById(R.id.btnDeleteCookbook);
            channel = itemView.findViewById(R.id.tvVideoChannel);
            channel.setVisibility(View.GONE);
        }
    }
}
