package com.example.plateit;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<RecipeVideo> videoList;
    private OnVideoClickListener listener;
    private boolean isChatMode;

    public interface OnVideoClickListener {
        void onVideoClick(RecipeVideo video);
    }

    public VideoAdapter(List<RecipeVideo> videoList, boolean isChatMode, OnVideoClickListener listener) {
        this.videoList = videoList;
        this.isChatMode = isChatMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isChatMode ? R.layout.item_chat_rich_card : R.layout.item_video_card;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new VideoViewHolder(view, isChatMode);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        RecipeVideo video = videoList.get(position);
        holder.title.setText(video.getTitle());

        if (isChatMode) {
            holder.badge.setText("VIDEO");
            holder.badge.setVisibility(View.VISIBLE);
            holder.footer.setText(video.getLength() != null ? video.getLength() : "Watch");
        } else {
            if (holder.time != null) {
                holder.time.setText(video.getLength() != null ? video.getLength() : "");
            }
        }

        // Load thumbnail using Picasso
        if (video.getThumbnail() != null && !video.getThumbnail().isEmpty()) {
            Picasso.get()
                    .load(video.getThumbnail())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.thumbnail);
        }

        // Handle click to open dialog
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVideoClick(video);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public void updateData(List<RecipeVideo> newVideos) {
        this.videoList = newVideos;
        notifyDataSetChanged();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView title, time, badge, footer;
        ImageView thumbnail;

        public VideoViewHolder(@NonNull View itemView, boolean isChatMode) {
            super(itemView);
            if (isChatMode) {
                title = itemView.findViewById(R.id.tvCardTitle);
                badge = itemView.findViewById(R.id.tvCardBadge);
                footer = itemView.findViewById(R.id.tvCardFooter);
                thumbnail = itemView.findViewById(R.id.imgCardThumbnail);
            } else {
                title = itemView.findViewById(R.id.videoTitle);
                time = itemView.findViewById(R.id.videoTime);
                thumbnail = itemView.findViewById(R.id.videoThumbnail);
            }
        }
    }
}
