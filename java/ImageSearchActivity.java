package com.example.pegasusimagemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    public static final String EXTRA_SEARCH_TYPE = "search_type";
    
    private RecyclerView recyclerViewCovers;
    private ProgressBar progressBar;
    private TextView tvSearchStatus;
    private TextView tvErrorMessage;
    private LinearLayout layoutLoading;
    private LinearLayout layoutContent;
    private LinearLayout layoutError;
    private CoverAdapter adapter;
    private EditText etGameName;
    private Button btnSearch;
    
    private String initialGameName;
    private Uri pegasusFolderUri;
    private String searchType;
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
        }
        
        initViews();
        getIntentData();
        
        if ("banner".equals(searchType)) {
            getSupportActionBar().setTitle("Buscar Banners");
        } else {
            getSupportActionBar().setTitle("Buscar Capas");
        }

        mainHandler = new Handler(Looper.getMainLooper());
        steamGridDbApi = new SteamGridDbApi(this);
        
        etGameName.setText(initialGameName);
        btnSearch.setOnClickListener(v -> startImageSearch());

        // Oculta o layout de loading inicialmente
        showLoading(false);
        layoutContent.setVisibility(View.GONE);
    }
    
    private void initViews() {
        recyclerViewCovers = findViewById(R.id.recyclerViewCovers);
        progressBar = findViewById(R.id.progressBar);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutContent = findViewById(R.id.layoutContent);
        layoutError = findViewById(R.id.layoutError);
        etGameName = findViewById(R.id.etGameName);
        btnSearch = findViewById(R.id.btnSearch);
        
        recyclerViewCovers.setLayoutManager(new GridLayoutManager(this, 2));
    }
    
    private void getIntentData() {
        Intent intent = getIntent();
        initialGameName = intent.getStringExtra(EXTRA_GAME_NAME);
        pegasusFolderUri = intent.getParcelableExtra(EXTRA_PEGASUS_FOLDER_URI);
        searchType = intent.getStringExtra(EXTRA_SEARCH_TYPE);

        if (initialGameName == null || pegasusFolderUri == null || searchType == null) {
            Toast.makeText(this, "Dados inválidos", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startImageSearch() {
        showLoading(true);
        tvSearchStatus.setText("Buscando jogos...");

        if (!steamGridDbApi.hasApiKey()) {
            showError("API Key do SteamGridDB não configurada");
            return;
        }

        String currentGameName = etGameName.getText().toString();

        steamGridDbApi.searchGames(currentGameName, new SteamGridDbApi.SearchCallback() {
            @Override
            public void onSuccess(List<SteamGridDbResponse.GameResult> games) {
                if (games.isEmpty()) {
                    mainHandler.post(() -> showError("Nenhum jogo encontrado"));
                    return;
                }

                SteamGridDbResponse.GameResult selectedGame = games.get(0);
                
                String statusText = "banner".equals(searchType) ? "Buscando banners..." : "Buscando capas...";
                mainHandler.post(() -> tvSearchStatus.setText(statusText));

                SteamGridDbApi.GridCallback gridCallback = new SteamGridDbApi.GridCallback() {
                    @Override
                    public void onSuccess(List<SteamGridDbResponse.GridResult> grids) {
                        mainHandler.post(() -> {
                            showLoading(false);
                            if (grids.isEmpty()) {
                                String errorMsg = "banner".equals(searchType) ? "Nenhum banner encontrado" : "Nenhuma capa encontrada";
                                showError(errorMsg);
                            } else {
                                showImageResults(grids);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        mainHandler.post(() -> showError("Erro ao buscar imagens: " + error));
                    }
                };

                if ("banner".equals(searchType)) {
                    steamGridDbApi.getGameHeroes(selectedGame.getId(), gridCallback);
                } else {
                    steamGridDbApi.getGameGrids(selectedGame.getId(), gridCallback);
                }
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
    
    private void showImageResults(List<SteamGridDbResponse.GridResult> images) {
        layoutLoading.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        adapter = new CoverAdapter(this, images, this);
        recyclerViewCovers.setAdapter(adapter);
    }

    @Override
    public void onCoverClick(SteamGridDbResponse.GridResult image) {
        showLoading(true);
        tvSearchStatus.setText("Baixando imagem...");

        steamGridDbApi.downloadImage(image.getUrl(), new Callback() {
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
                    DocumentFile pegasusFolder = DocumentFile.fromTreeUri(ImageSearchActivity.this, pegasusFolderUri);
                    if (pegasusFolder == null) {
                        showErrorAndFinish("Erro ao acessar pasta");
                        return;
                    }

                    DocumentFile mediaFolder = pegasusFolder.findFile("media");
                    if (mediaFolder == null || !mediaFolder.isDirectory()) {
                        mediaFolder = pegasusFolder.createDirectory("media");
                    }
                    if (mediaFolder == null) {
                        showErrorAndFinish("Erro ao criar pasta media");
                        return;
                    }

                    DocumentFile gameFolder = mediaFolder.findFile(initialGameName);
                    if (gameFolder == null || !gameFolder.isDirectory()) {
                        gameFolder = mediaFolder.createDirectory(initialGameName);
                    }
                    if (gameFolder == null) {
                        showErrorAndFinish("Erro ao criar pasta do jogo");
                        return;
                    }

                    String fileName = "banner".equals(searchType) ? "screenshot.png" : "boxFront.png";
                    String mimeType = image.getMime().equals("image/jpeg") ? "image/jpeg" : "image/png";

                    DocumentFile existingFile = gameFolder.findFile(fileName);
                    if (existingFile != null && existingFile.isFile()) {
                        existingFile.delete();
                    }

                    DocumentFile imageFile = gameFolder.createFile(mimeType, fileName);
                    if (imageFile == null) {
                        showErrorAndFinish("Erro ao criar arquivo de imagem");
                        return;
                    }

                    try (InputStream inputStream = response.body().byteStream();
                         OutputStream outputStream = getContentResolver().openOutputStream(imageFile.getUri())) {

                        if (outputStream == null) {
                            showErrorAndFinish("Erro ao abrir arquivo para escrita");
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
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                } catch (Exception e) {
                    showErrorAndFinish("Erro ao salvar imagem: " + e.getMessage());
                }
            }
        });
    }

    private void showErrorAndFinish(String message) {
        mainHandler.post(() -> {
            showLoading(false);
            Toast.makeText(ImageSearchActivity.this, message, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static Intent createIntent(android.content.Context context, String gameName, Uri pegasusFolderUri, String searchType) {
        Intent intent = new Intent(context, ImageSearchActivity.class);
        intent.putExtra(EXTRA_GAME_NAME, gameName);
        intent.putExtra(EXTRA_PEGASUS_FOLDER_URI, pegasusFolderUri);
        intent.putExtra(EXTRA_SEARCH_TYPE, searchType);
        return intent;
    }
}