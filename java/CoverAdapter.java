package com.example.pegasusimagemanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class CoverAdapter extends RecyclerView.Adapter<CoverAdapter.ViewHolder> {
    private List<SteamGridDbResponse.GridResult> covers;
    private OnCoverClickListener listener;
    private Context context;

    public interface OnCoverClickListener {
        void onCoverClick(SteamGridDbResponse.GridResult cover);
    }

    public CoverAdapter(Context context, List<SteamGridDbResponse.GridResult> covers, OnCoverClickListener listener) {
        this.context = context;
        this.covers = covers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cover, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SteamGridDbResponse.GridResult cover = covers.get(position);
        
        // Carregar thumbnail da capa
        Glide.with(context)
                .load(cover.getThumb())
                .into(holder.imgCover);
        
        // Configurar tags se existirem
        holder.chipGroupTags.removeAllViews();
        if (cover.getTags() != null && !cover.getTags().isEmpty()) {
            for (String tag : cover.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setTextSize(10);
                holder.chipGroupTags.addView(chip);
            }
        }
        
        // Configurar autor se existir
        if (cover.getAuthor() != null) {
            holder.tvAuthorName.setText(cover.getAuthor().getName());
            if (cover.getAuthor().getAvatar() != null) {
                Glide.with(context)
                        .load(cover.getAuthor().getAvatar())
                        .circleCrop()
                        .into(holder.imgAuthorAvatar);
            }
        } else {
            holder.tvAuthorName.setText("Autor desconhecido");
        }
        
        // Click listeners
        holder.cardCover.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCoverClick(cover);
            }
        });
        
        // Click listener para botão select também
        holder.btnSelect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCoverClick(cover);
            }
        });
    }

    @Override
    public int getItemCount() {
        return covers != null ? covers.size() : 0;
    }

    public void updateCovers(List<SteamGridDbResponse.GridResult> newCovers) {
        this.covers = newCovers;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCover;
        ImageView imgCover;
        ChipGroup chipGroupTags;
        ImageView imgAuthorAvatar;
        TextView tvAuthorName;
        MaterialButton btnSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCover = itemView.findViewById(R.id.cardCover);
            imgCover = itemView.findViewById(R.id.imgCover);
            chipGroupTags = itemView.findViewById(R.id.chipGroupTags);
            imgAuthorAvatar = itemView.findViewById(R.id.imgAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            btnSelect = itemView.findViewById(R.id.btnSelect);
        }
    }
}