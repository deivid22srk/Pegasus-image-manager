package com.example.pegasusimagemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SteamGridDbApi {
    private static final String TAG = "SteamGridDbApi";
    private static final String BASE_URL = "https://www.steamgriddb.com/api/v2";
    private static final String PREFS_NAME = "steamgriddb_prefs";
    private static final String API_KEY_PREF = "api_key";
    
    private final OkHttpClient client;
    private final Gson gson;
    private final SharedPreferences prefs;
    
    public interface SearchCallback {
        void onSuccess(List<SteamGridDbResponse.GameResult> games);
        void onError(String error);
    }
    
    public interface GridCallback {
        void onSuccess(List<SteamGridDbResponse.GridResult> grids);
        void onError(String error);
    }
    
    public SteamGridDbApi(Context context) {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        
        gson = new GsonBuilder().create();
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void setApiKey(String apiKey) {
        prefs.edit().putString(API_KEY_PREF, apiKey).apply();
    }
    
    public String getApiKey() {
        return prefs.getString(API_KEY_PREF, "");
    }
    
    public boolean hasApiKey() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
    
    public void searchGames(String gameName, SearchCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API Key não configurada");
            return;
        }
        
        String url = BASE_URL + "/search/autocomplete/" + gameName;
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getApiKey())
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Erro na busca: " + e.getMessage());
                callback.onError("Erro de conexão: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Resposta não bem-sucedida: " + response.code());
                    callback.onError("Erro do servidor: " + response.code());
                    return;
                }
                
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Resposta da busca: " + responseBody);
                    
                    SteamGridDbResponse searchResponse = gson.fromJson(responseBody, SteamGridDbResponse.class);
                    
                    if (searchResponse.isSuccess() && searchResponse.getData() != null) {
                        callback.onSuccess(searchResponse.getData());
                    } else {
                        callback.onError("Nenhum jogo encontrado");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar resposta: " + e.getMessage());
                    callback.onError("Erro ao processar resposta");
                }
            }
        });
    }
    
    public void getGameHeroes(int gameId, GridCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API Key não configurada");
            return;
        }

        String url = BASE_URL + "/heroes/game/" + gameId + "?limit=20";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getApiKey())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Erro ao buscar heroes: " + e.getMessage());
                callback.onError("Erro de conexão: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Resposta não bem-sucedida: " + response.code());
                    callback.onError("Erro do servidor: " + response.code());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Resposta dos heroes: " + responseBody);

                    SteamGridDbResponse.GridResponse gridResponse = gson.fromJson(responseBody, SteamGridDbResponse.GridResponse.class);

                    if (gridResponse.isSuccess() && gridResponse.getData() != null) {
                        callback.onSuccess(gridResponse.getData());
                    } else {
                        callback.onError("Nenhum banner encontrado");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar heroes: " + e.getMessage());
                    callback.onError("Erro ao processar resposta");
                }
            }
        });
    }

    public void getGameGrids(int gameId, GridCallback callback) {
        if (!hasApiKey()) {
            callback.onError("API Key não configurada");
            return;
        }
        
        String url = BASE_URL + "/grids/game/" + gameId + "?dimensions=600x900&types=static&limit=20";
        
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + getApiKey())
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Erro ao buscar grids: " + e.getMessage());
                callback.onError("Erro de conexão: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Resposta não bem-sucedida: " + response.code());
                    callback.onError("Erro do servidor: " + response.code());
                    return;
                }
                
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Resposta dos grids: " + responseBody);
                    
                    SteamGridDbResponse.GridResponse gridResponse = gson.fromJson(responseBody, SteamGridDbResponse.GridResponse.class);
                    
                    if (gridResponse.isSuccess() && gridResponse.getData() != null) {
                        callback.onSuccess(gridResponse.getData());
                    } else {
                        callback.onError("Nenhuma capa encontrada");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar grids: " + e.getMessage());
                    callback.onError("Erro ao processar resposta");
                }
            }
        });
    }
    
    public void downloadImage(String imageUrl, Callback callback) {
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();
        
        client.newCall(request).enqueue(callback);
    }
}