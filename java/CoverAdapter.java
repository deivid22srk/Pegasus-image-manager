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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

public class CoverAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_COVER = 1;
    private static final int VIEW_TYPE_BANNER = 2;

    private List<SteamGridDbResponse.GridResult> images;
    private OnCoverClickListener listener;
    private Context context;
    private String searchType;

    public interface OnCoverClickListener {
        void onCoverClick(SteamGridDbResponse.GridResult cover);
    }

    public CoverAdapter(Context context, List<SteamGridDbResponse.GridResult> images, String searchType, OnCoverClickListener listener) {
        this.context = context;
        this.images = images;
        this.searchType = searchType;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if ("banner".equals(searchType)) {
            return VIEW_TYPE_BANNER;
        }
        return VIEW_TYPE_COVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_BANNER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cover, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        ViewHolder holder = (ViewHolder) viewHolder;
        SteamGridDbResponse.GridResult image = images.get(position);

        // Carregar thumbnail da capa
        Glide.with(context)
                .load(image.getThumb())
                .into(holder.imgCover);

        // Configurar tags se existirem
        holder.chipGroupTags.removeAllViews();
        if (image.getTags() != null && !image.getTags().isEmpty()) {
            for (String tag : image.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setTextSize(10);
                holder.chipGroupTags.addView(chip);
            }
        }

        // Configurar autor se existir
        if (image.getAuthor() != null) {
            holder.tvAuthorName.setText(image.getAuthor().getName());
            if (image.getAuthor().getAvatar() != null) {
                Glide.with(context)
                        .load(image.getAuthor().getAvatar())
                        .circleCrop()
                        .into(holder.imgAuthorAvatar);
            }
        } else {
            holder.tvAuthorName.setText("Autor desconhecido");
        }

        // Click listeners
        holder.cardCover.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCoverClick(image);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }

    public void updateCovers(List<SteamGridDbResponse.GridResult> newImages) {
        this.images = newImages;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCover;
        ImageView imgCover;
        ChipGroup chipGroupTags;
        ImageView imgAuthorAvatar;
        TextView tvAuthorName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCover = itemView.findViewById(R.id.cardCover);
            imgCover = itemView.findViewById(R.id.imgCover);
            chipGroupTags = itemView.findViewById(R.id.chipGroupTags);
            imgAuthorAvatar = itemView.findViewById(R.id.imgAuthorAvatar);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
        }
    }
}