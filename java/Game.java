package com.example.pegasusimagemanager;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

public class Game {
    private String name;
    private Uri imageUri;
    private DocumentFile gameDirectory;
    private boolean hasImage;

    public Game(String name) {
        this.name = name;
        this.imageUri = null;
        this.gameDirectory = null;
        this.hasImage = false;
    }

    public Game(String name, Uri imageUri) {
        this.name = name;
        this.imageUri = imageUri;
        this.hasImage = imageUri != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
        this.hasImage = imageUri != null;
    }

    public DocumentFile getGameDirectory() {
        return gameDirectory;
    }

    public void setGameDirectory(DocumentFile gameDirectory) {
        this.gameDirectory = gameDirectory;
        // Atualiza status da imagem se temos diretório
        if (gameDirectory != null) {
            updateImageStatus();
        }
    }

    public boolean hasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    /**
     * Atualiza o status da imagem verificando se existe no diretório
     */
    private void updateImageStatus() {
        if (gameDirectory != null) {
            DocumentFile[] files = gameDirectory.listFiles();
            boolean foundImage = false;
            
            for (DocumentFile file : files) {
                if (file != null && file.isFile() && "boxFront.png".equals(file.getName())) {
                    this.imageUri = file.getUri();
                    this.hasImage = true;
                    foundImage = true;
                    break;
                }
            }
            
            if (!foundImage) {
                this.imageUri = null;
                this.hasImage = false;
            }
        }
    }

    /**
     * Verifica se existe imagem para este jogo usando DocumentFile
     * @param context Contexto da aplicação
     * @return true se existe imagem
     */
    public boolean checkImageExists(Context context) {
        if (gameDirectory == null) {
            return false;
        }
        
        DocumentFile imageFile = FileHelper.getBoxFrontImage(context, gameDirectory);
        if (imageFile != null) {
            this.imageUri = imageFile.getUri();
            this.hasImage = true;
            return true;
        }
        
        this.imageUri = null;
        this.hasImage = false;
        return false;
    }
}