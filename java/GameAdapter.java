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
import com.bumptech.glide.signature.ObjectKey;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private List<Game> games;
    private Context context;
    private OnGameClickListener listener;

    public interface OnGameClickListener {
        void onAddImageClick(Game game, int position);
        void onSearchCoverClick(Game game, int position);
    }

    public GameAdapter(List<Game> games, Context context) {
        this.games = games;
        this.context = context;
    }

    public void setOnGameClickListener(OnGameClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_game, parent, false);
        return new GameViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        Game game = games.get(position);
        holder.bind(game, position);
    }

    @Override
    public int getItemCount() {
        return games.size();
    }

    public void updateGame(int position, Game game) {
        games.set(position, game);
        notifyItemChanged(position);
    }

    class GameViewHolder extends RecyclerView.ViewHolder {
        private TextView tvGameName;
        private TextView tvImageStatus;
        private ImageView imgGameCover;
        private ImageView imgPlaceholder;
        private MaterialButton btnAddImage;
        private MaterialButton btnSearchCover;

        public GameViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGameName = itemView.findViewById(R.id.tvGameName);
            tvImageStatus = itemView.findViewById(R.id.tvImageStatus);
            imgGameCover = itemView.findViewById(R.id.imgGameCover);
            imgPlaceholder = itemView.findViewById(R.id.imgPlaceholder);
            btnAddImage = itemView.findViewById(R.id.btnAddImage);
            btnSearchCover = itemView.findViewById(R.id.btnSearchCover);
        }

        public void bind(Game game, int position) {
            tvGameName.setText(game.getName());

            // Verifica se o jogo tem imagem
            game.checkImageExists(context);

            // Atualiza UI baseado na existência da imagem
            if (game.hasImage()) {
                tvImageStatus.setText("Imagem disponível");
                imgPlaceholder.setVisibility(View.GONE);
                imgGameCover.setVisibility(View.VISIBLE);
                btnAddImage.setText(context.getString(R.string.change_image));
                
                // Carrega imagem usando URI com assinatura para cache busting
                Glide.with(context)
                        .load(game.getImageUri())
                        .signature(new ObjectKey(String.valueOf(game.getImageLastModified())))
                        .centerCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(imgGameCover);
            } else {
                tvImageStatus.setText(context.getString(R.string.no_image));
                imgPlaceholder.setVisibility(View.VISIBLE);
                imgGameCover.setVisibility(View.GONE);
                btnAddImage.setText(context.getString(R.string.add_image));
            }

            btnAddImage.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddImageClick(game, position);
                }
            });

            btnSearchCover.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSearchCoverClick(game, position);
                }
            });
        }
    }
}