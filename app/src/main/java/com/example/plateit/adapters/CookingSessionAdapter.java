package com.example.plateit.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plateit.R;
import com.example.plateit.responses.CookbookEntry;
import com.example.plateit.responses.CookingSession;

import java.util.List;

public class CookingSessionAdapter extends RecyclerView.Adapter<CookingSessionAdapter.ViewHolder> {

    public interface OnResumeClickListener {
        void onResume(CookingSession session, CookbookEntry matchedEntry);
    }

    private List<CookingSession> sessions;
    private List<CookbookEntry> cookbook;
    private OnResumeClickListener resumeListener;

    public CookingSessionAdapter(List<CookingSession> sessions, List<CookbookEntry> cookbook,
            OnResumeClickListener listener) {
        this.sessions = sessions;
        this.cookbook = cookbook;
        this.resumeListener = listener;
    }

    public void updateData(List<CookingSession> sessions, List<CookbookEntry> cookbook) {
        this.sessions = sessions;
        this.cookbook = cookbook;
        notifyDataSetChanged();
    }

    public void updateCookbook(List<CookbookEntry> cookbook) {
        this.cookbook = cookbook;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CookingSession session = sessions.get(position);

        // Find matching cookbook entry
        CookbookEntry match = null;
        if (session.getCookbookId() != null) {
            for (CookbookEntry entry : cookbook) {
                if (entry.getId() == session.getCookbookId()) {
                    match = entry;
                    break;
                }
            }
        }

        // Set title
        if (match != null) {
            holder.tvTitle.setText(match.getTitle());
        } else {
            holder.tvTitle.setText("Session #" + session.getId());
        }

        // Set progress and status
        if (session.isFinished()) {
            holder.tvProgress.setText("Completed ✓");
            holder.tvProgress.setTextColor(0xFF4CAF50); // Green
            holder.btnResume.setVisibility(View.GONE);
            holder.ivStatus.setAlpha(0.5f);
            holder.itemView.setAlpha(0.7f);
            holder.itemView.setOnClickListener(null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holder.cardRoot.setOutlineSpotShadowColor(0xFF4CAF50);
                holder.cardRoot.setOutlineAmbientShadowColor(0xFF4CAF50);
            }
        } else {
            holder.tvProgress.setText("Step " + (session.getCurrentStepIndex() + 1) + " • In Progress");
            holder.tvProgress.setTextColor(0xFFFF9800); // Orange
            holder.btnResume.setVisibility(View.VISIBLE);
            holder.ivStatus.setAlpha(1.0f);
            holder.itemView.setAlpha(1.0f);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                holder.cardRoot.setOutlineSpotShadowColor(0xFFFF9800);
                holder.cardRoot.setOutlineAmbientShadowColor(0xFFFF9800);
            }

            final CookbookEntry finalMatch = match;
            View.OnClickListener resumeClick = v -> {
                if (resumeListener != null && finalMatch != null) {
                    resumeListener.onResume(session, finalMatch);
                }
            };

            holder.btnResume.setOnClickListener(resumeClick);
            holder.itemView.setOnClickListener(resumeClick); // Make entire card clickable
        }
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardRoot;
        ImageView ivStatus;
        TextView tvTitle, tvProgress;
        Button btnResume;

        ViewHolder(View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            ivStatus = itemView.findViewById(R.id.ivSessionStatus);
            tvTitle = itemView.findViewById(R.id.tvSessionTitle);
            tvProgress = itemView.findViewById(R.id.tvSessionProgress);
            btnResume = itemView.findViewById(R.id.btnSessionResume);
        }
    }
}
