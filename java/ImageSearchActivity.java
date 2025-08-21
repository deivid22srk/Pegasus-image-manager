package com.example.pegasusimagemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ImageSearchActivity extends AppCompatActivity implements CoverAdapter.OnCoverClickListener {
    
    public static final String EXTRA_GAME_NAME = "game_name";
    public static final String EXTRA_PEGASUS_FOLDER_URI = "pegasus_folder_uri";
    
    private RecyclerView recyclerViewCovers;
    private ProgressBar progressBar;
    private TextView tvSearchStatus;
    private TextView tvErrorMessage;
    private LinearLayout layoutLoading;
    private LinearLayout layoutContent;
    private LinearLayout layoutError;
    private CoverAdapter adapter;
    
    private String gameName;
    private Uri pegasusFolderUri;
    private SteamGridDbApi steamGridDbApi;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_search);
        
        DynamicColorHelper.applyDynamicColors(this);
        
        setSupportActionBar(findViewById(R.id.topAppBar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Buscar Capas");
        }
        
        initViews();
        getIntentData();
        
        mainHandler = new Handler(Looper.getMainLooper());
        steamGridDbApi = new SteamGridDbApi(this);
        
        searchCovers();
    }
    
    private void initViews() {
        recyclerViewCovers = findViewById(R.id.recyclerViewCovers);
        progressBar = findViewById(R.id.progressBar);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutContent = findViewById(R.id.layoutContent);
        layoutError = findViewById(R.id.layoutError);
        
        recyclerViewCovers.setLayoutManager(new GridLayoutManager(this, 2));
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        gameName = intent.getStringExtra(EXTRA_GAME_NAME);
        pegasusFolderUri = intent.getParcelableExtra(EXTRA_PEGASUS_FOLDER_URI);
        
        if (gameName == null || pegasusFolderUri == null) {
            Toast.makeText(this, "Dados inválidos", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void searchCovers() {
        showLoading(true);
        tvSearchStatus.setText("Buscando jogos...");
        
        if (!steamGridDbApi.hasApiKey()) {
            showError("API Key do SteamGridDB não configurada");
            return;
        }
        
        steamGridDbApi.searchGames(gameName, new SteamGridDbApi.SearchCallback() {
            @Override
            public void onSuccess(List<SteamGridDbResponse.GameResult> games) {
                if (games.isEmpty()) {
                    mainHandler.post(() -> showError("Nenhum jogo encontrado"));
                    return;
                }
                
                // Pegar o primeiro resultado
                SteamGridDbResponse.GameResult selectedGame = games.get(0);
                
                mainHandler.post(() -> {
                    tvSearchStatus.setText("Buscando capas...");
                });
                
                // Buscar capas para o jogo
                steamGridDbApi.getGameGrids(selectedGame.getId(), new SteamGridDbApi.GridCallback() {
                    @Override
                    public void onSuccess(List<SteamGridDbResponse.GridResult> grids) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            if (grids.isEmpty()) {
                                showError("Nenhuma capa encontrada");
                            } else {
                                showCovers(grids);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> showError("Erro ao buscar capas: " + error));
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> showError("Erro na busca: " + error));
            }
        });
    }
    
    private void showLoading(boolean show) {
        layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        layoutContent.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutError.setVisibility(View.GONE);
    }
    
    private void showError(String message) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(message);
    }
    
    private void showCovers(List<SteamGridDbResponse.GridResult> covers) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        adapter = new CoverAdapter(this, covers, this);
        recyclerViewCovers.setAdapter(adapter);
    }
    
    @Override
    public void onCoverClick(SteamGridDbResponse.GridResult cover) {
        showLoading(true);
        tvSearchStatus.setText("Baixando imagem...");
        
        steamGridDbApi.downloadImage(cover.getUrl(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(ImageSearchActivity.this, 
                        "Erro ao baixar imagem: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        Toast.makeText(ImageSearchActivity.this, 
                            "Erro no download: " + response.code(), 
                            Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                try {
                    // Criar diretório /media/nomedojogo/
                    DocumentFile pegasusFolder = DocumentFile.fromTreeUri(ImageSearchActivity.this, pegasusFolderUri);
                    if (pegasusFolder == null) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(ImageSearchActivity.this, 
                                "Erro ao acessar pasta", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    
                    DocumentFile mediaFolder = pegasusFolder.findFile("media");
                    if (mediaFolder == null || !mediaFolder.isDirectory()) {
                        mediaFolder = pegasusFolder.createDirectory("media");
                    }
                    
                    if (mediaFolder == null) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(ImageSearchActivity.this, 
                                "Erro ao criar pasta media", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    
                    DocumentFile gameFolder = mediaFolder.findFile(gameName);
                    if (gameFolder == null || !gameFolder.isDirectory()) {
                        gameFolder = mediaFolder.createDirectory(gameName);
                    }
                    
                    if (gameFolder == null) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(ImageSearchActivity.this, 
                                "Erro ao criar pasta do jogo", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    
                    // Criar arquivo boxFront.png
                    DocumentFile existingFile = gameFolder.findFile("boxFront.png");
                    if (existingFile != null && existingFile.isFile()) {
                        existingFile.delete();
                    }
                    
                    DocumentFile imageFile = gameFolder.createFile("image/png", "boxFront.png");
                    if (imageFile == null) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(ImageSearchActivity.this, 
                                "Erro ao criar arquivo", Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    
                    // Copiar dados da imagem
                    try (InputStream inputStream = response.body().byteStream();
                         OutputStream outputStream = getContentResolver().openOutputStream(imageFile.getUri())) {
                        
                        if (outputStream == null) {
                            mainHandler.post(() -> {
                                showLoading(false);
                                Toast.makeText(ImageSearchActivity.this, 
                                    "Erro ao abrir arquivo para escrita", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        
                        mainHandler.post(() -> {
                            showLoading(false);
                            Toast.makeText(ImageSearchActivity.this, 
                                "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        showLoading(false);
                        Toast.makeText(ImageSearchActivity.this, 
                            "Erro ao salvar imagem: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    public static Intent createIntent(android.content.Context context, String gameName, Uri pegasusFolderUri) {
        Intent intent = new Intent(context, ImageSearchActivity.class);
        intent.putExtra(EXTRA_GAME_NAME, gameName);
        intent.putExtra(EXTRA_PEGASUS_FOLDER_URI, pegasusFolderUri);
        return intent;
    }
}