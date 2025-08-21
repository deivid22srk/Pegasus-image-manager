package com.example.pegasusimagemanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

public class SettingsActivity extends AppCompatActivity {
    
    private TextInputLayout tilApiKey;
    private TextInputEditText etApiKey;
    private MaterialButton btnSave;
    private MaterialButton btnGetApiKey;
    private MaterialButton btnTestConnection;
    private MaterialTextView tvStatus;
    
    private SteamGridDbApi steamGridDbApi;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Aplica Dynamic Color
        DynamicColorHelper.applyDynamicColors(this);
        
        setContentView(R.layout.activity_settings);
        
        initializeViews();
        setupToolbar();
        loadCurrentSettings();
        
        steamGridDbApi = new SteamGridDbApi(this);
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    private void initializeViews() {
        tilApiKey = findViewById(R.id.tilApiKey);
        etApiKey = findViewById(R.id.etApiKey);
        btnSave = findViewById(R.id.btnSave);
        btnGetApiKey = findViewById(R.id.btnGetApiKey);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        tvStatus = findViewById(R.id.tvStatus);
        
        btnSave.setOnClickListener(v -> saveApiKey());
        btnGetApiKey.setOnClickListener(v -> openSteamGridDbWebsite());
        btnTestConnection.setOnClickListener(v -> testConnection());
    }
    
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.settings));
        }
        
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void loadCurrentSettings() {
        String currentApiKey = steamGridDbApi.getApiKey();
        if (currentApiKey != null && !currentApiKey.isEmpty()) {
            etApiKey.setText(currentApiKey);
            updateStatus(true, getString(R.string.api_key_configured));
        } else {
            updateStatus(false, getString(R.string.api_key_not_configured));
        }
    }
    
    private void saveApiKey() {
        String apiKey = etApiKey.getText().toString().trim();
        
        if (apiKey.isEmpty()) {
            tilApiKey.setError("Por favor, insira uma API Key válida");
            return;
        }
        
        tilApiKey.setError(null);
        steamGridDbApi.setApiKey(apiKey);
        
        Toast.makeText(this, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show();
        updateStatus(true, getString(R.string.api_key_configured));
    }
    
    private void testConnection() {
        if (!steamGridDbApi.hasApiKey()) {
            Toast.makeText(this, "Primeiro configure uma API Key", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnTestConnection.setEnabled(false);
        btnTestConnection.setText("Testando...");
        
        // Faz uma busca simples para testar a conexão
        steamGridDbApi.searchGames("test", new SteamGridDbApi.SearchCallback() {
            @Override
            public void onSuccess(java.util.List<SteamGridDbResponse.GameResult> games) {
                mainHandler.post(() -> {
                    btnTestConnection.setEnabled(true);
                    btnTestConnection.setText(getString(R.string.test_connection));
                    Toast.makeText(SettingsActivity.this, 
                        getString(R.string.connection_test_success), Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    btnTestConnection.setEnabled(true);
                    btnTestConnection.setText(getString(R.string.test_connection));
                    Toast.makeText(SettingsActivity.this, 
                        getString(R.string.connection_test_failed, error), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void openSteamGridDbWebsite() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.steamgriddb.com/profile/preferences/api"));
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível abrir o navegador", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateStatus(boolean hasApiKey, String message) {
        tvStatus.setText(message);
        
        if (hasApiKey) {
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            tvStatus.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.presence_online, 0, 0, 0);
        } else {
            tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            tvStatus.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.presence_offline, 0, 0, 0);
        }
        
        btnTestConnection.setVisibility(hasApiKey ? View.VISIBLE : View.GONE);
    }
}