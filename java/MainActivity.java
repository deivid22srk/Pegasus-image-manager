package com.example.pegasusimagemanager;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GameAdapter.OnGameClickListener {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int IMAGE_SEARCH_REQUEST_CODE = 1002;
    private static final String PREFS_NAME = "pegasus_prefs";
    private static final String PREF_SELECTED_FOLDER_URI = "selected_folder_uri";
    
    private MaterialButton btnSelectFolder;
    private MaterialCardView cardConsoleInfo;
    private TextView tvConsoleName;
    private TextView tvGamesCount;
    private TextView tvCurrentPath;
    private RecyclerView recyclerViewGames;
    
    private GameAdapter gameAdapter;
    private List<Game> gamesList;
    private Uri selectedFolderUri;
    private int currentGamePosition = -1;
    
    private ActivityResultLauncher<Intent> folderPickerLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> imageSearchLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Aplica Dynamic Color se disponível
        DynamicColorHelper.applyDynamicColors(this);
        
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupToolbar();
        initializeActivityLaunchers();
        setupRecyclerView();
        checkPermissions();
        
        // Tenta carregar pasta salva automaticamente
        loadSavedFolder();
    }

    private void initializeViews() {
        btnSelectFolder = findViewById(R.id.btnSelectFolder);
        cardConsoleInfo = findViewById(R.id.cardConsoleInfo);
        tvConsoleName = findViewById(R.id.tvConsoleName);
        tvGamesCount = findViewById(R.id.tvGamesCount);
        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        recyclerViewGames = findViewById(R.id.recyclerViewGames);
        
        btnSelectFolder.setOnClickListener(v -> openFolderPicker());
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_change_folder) {
            openFolderPicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeActivityLaunchers() {
        // Launcher para seleção da pasta Pegasus
        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // Persiste permissão para a URI
                            getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            
                            selectedFolderUri = uri;
                            saveFolderUri(uri);
                            processSelectedFolder();
                        }
                    }
                }
        );

        // Launcher para seleção de imagem da galeria
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null && currentGamePosition >= 0) {
                            copyImageToGameDirectory(imageUri, currentGamePosition);
                        }
                    }
                }
        );
        
        // Launcher para busca de imagens
        imageSearchLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && currentGamePosition >= 0) {
                        // Atualiza o jogo se imagem foi baixada
                        Game game = gamesList.get(currentGamePosition);
                        game.checkImageExists(this);
                        gameAdapter.updateGame(currentGamePosition, game);
                    }
                }
        );
    }

    private void setupRecyclerView() {
        gamesList = new ArrayList<>();
        gameAdapter = new GameAdapter(gamesList, this);
        gameAdapter.setOnGameClickListener(this);
        
        recyclerViewGames.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGames.setAdapter(gameAdapter);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Solicita MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            // Android 10 e anteriores
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        folderPickerLauncher.launch(intent);
    }

    private void processSelectedFolder() {
        if (selectedFolderUri == null) {
            return;
        }
        
        // Lê o nome do console do arquivo metadata.pegasus.txt
        String consoleName = FileHelper.readConsoleNameFromMetadata(this, selectedFolderUri);
        
        if (consoleName == null) {
            Toast.makeText(this, getString(R.string.no_metadata_found), Toast.LENGTH_LONG).show();
            return;
        }
        
        // Lê os nomes dos jogos dos arquivos .desktop
        List<String> gameNames = FileHelper.readGameNamesFromDesktopFiles(this, selectedFolderUri);
        
        if (gameNames.isEmpty()) {
            Toast.makeText(this, "Nenhum jogo encontrado nos arquivos .desktop", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Atualiza a UI
        tvConsoleName.setText(consoleName);
        tvGamesCount.setText(gameNames.size() + " jogos");
        updateCurrentPathDisplay();
        cardConsoleInfo.setVisibility(View.VISIBLE);
        btnSelectFolder.setText(getString(R.string.change_folder));
        
        // Cria lista de jogos
        gamesList.clear();
        for (String gameName : gameNames) {
            Game game = new Game(gameName);
            
            // Cria diretório do jogo automaticamente
            DocumentFile gameDir = FileHelper.createGameMediaDirectory(this, selectedFolderUri, gameName);
            if (gameDir != null) {
                game.setGameDirectory(gameDir);
            }
            
            gamesList.add(game);
        }
        
        gameAdapter.notifyDataSetChanged();
        Toast.makeText(this, getString(R.string.directories_created), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddImageClick(Game game, int position) {
        if (game.getGameDirectory() == null) {
            Toast.makeText(this, "Diretório do jogo não encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        
        currentGamePosition = position;
        
        // Mostra opções: galeria ou buscar online
        showImageSourceDialog(game);
    }
    
    @Override
    public void onSearchCoverClick(Game game, int position) {
        if (game.getGameDirectory() == null) {
            Toast.makeText(this, "Diretório do jogo não encontrado", Toast.LENGTH_LONG).show();
            return;
        }
        
        currentGamePosition = position;
        searchCoverOnline(game);
    }
    
    private void showImageSourceDialog(Game game) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Selecionar Imagem")
                .setMessage("Como você gostaria de adicionar uma imagem para " + game.getName() + "?")
                .setPositiveButton("Galeria", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setType("image/*");
                    imagePickerLauncher.launch(intent);
                })
                .setNegativeButton("Buscar Online", (dialog, which) -> {
                    searchCoverOnline(game);
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }
    
    private void searchCoverOnline(Game game) {
        // Verifica se SteamGridDB está configurado
        SteamGridDbApi api = new SteamGridDbApi(this);
        if (!api.hasApiKey()) {
            Toast.makeText(this, getString(R.string.steamgriddb_not_configured), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        
        Intent intent = new Intent(this, ImageSearchActivity.class);
        intent.putExtra(ImageSearchActivity.EXTRA_GAME_NAME, game.getName());
        intent.putExtra(ImageSearchActivity.EXTRA_PEGASUS_FOLDER_URI, 
            game.getGameDirectory().getUri().toString());
        imageSearchLauncher.launch(intent);
    }

    private void copyImageToGameDirectory(Uri imageUri, int gamePosition) {
        if (gamePosition < 0 || gamePosition >= gamesList.size()) {
            return;
        }
        
        Game game = gamesList.get(gamePosition);
        DocumentFile gameDirectory = game.getGameDirectory();
        
        if (gameDirectory == null) {
            Toast.makeText(this, "Diretório do jogo não encontrado", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Cria o arquivo boxFront.png no diretório do jogo
            DocumentFile imageFile = FileHelper.createBoxFrontImage(this, gameDirectory);
            
            if (imageFile == null) {
                Toast.makeText(this, "Não foi possível criar o arquivo de imagem", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Copia o conteúdo da imagem selecionada
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
                 OutputStream outputStream = getContentResolver().openOutputStream(imageFile.getUri())) {
                
                if (inputStream == null || outputStream == null) {
                    throw new Exception("Não foi possível abrir streams");
                }
                
                byte[] buffer = new byte[1024];
                int length;
                
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                
                outputStream.flush();
            }
            
            // Atualiza o jogo na lista
            game.setImageUri(imageFile.getUri());
            game.setHasImage(true);
            gameAdapter.updateGame(gamePosition, game);
            
            Toast.makeText(this, getString(R.string.image_updated), Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            String errorMessage = "Erro ao copiar imagem";
            if (e.getMessage() != null) {
                errorMessage += ": " + e.getMessage();
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            
            if (!allPermissionsGranted) {
                Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void loadSavedFolder() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedUriString = prefs.getString(PREF_SELECTED_FOLDER_URI, null);
        
        if (savedUriString != null) {
            try {
                Uri savedUri = Uri.parse(savedUriString);
                
                // Verifica se ainda temos permissão para esta URI
                List<UriPermission> persistentUris = getContentResolver().getPersistedUriPermissions();
                boolean hasPermission = false;
                
                for (UriPermission permission : persistentUris) {
                    if (permission.getUri().equals(savedUri) && 
                        permission.isReadPermission() && permission.isWritePermission()) {
                        hasPermission = true;
                        break;
                    }
                }
                
                if (hasPermission) {
                    selectedFolderUri = savedUri;
                    processSelectedFolder();
                    return;
                }
            } catch (Exception e) {
                // URI inválida, remove das preferências
                prefs.edit().remove(PREF_SELECTED_FOLDER_URI).apply();
            }
        }
        
        // Se chegou aqui, não há pasta salva válida
        showSelectFolderState();
    }
    
    private void saveFolderUri(Uri uri) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_SELECTED_FOLDER_URI, uri.toString()).apply();
    }
    
    private void showSelectFolderState() {
        cardConsoleInfo.setVisibility(View.GONE);
        btnSelectFolder.setText(getString(R.string.select_folder));
        tvCurrentPath.setVisibility(View.GONE);
        gamesList.clear();
        gameAdapter.notifyDataSetChanged();
    }
    
    private void updateCurrentPathDisplay() {
        if (selectedFolderUri != null) {
            DocumentFile folder = DocumentFile.fromTreeUri(this, selectedFolderUri);
            if (folder != null && folder.exists()) {
                tvCurrentPath.setText("Pasta: " + folder.getName());
                tvCurrentPath.setVisibility(View.VISIBLE);
            }
        } else {
            tvCurrentPath.setVisibility(View.GONE);
        }
    }
}