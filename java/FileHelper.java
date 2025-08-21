package com.example.pegasusimagemanager;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileHelper {

    /**
     * Lê o arquivo metadata.pegasus.txt e extrai o nome do console usando SAF
     * @param context Contexto da aplicação
     * @param directoryUri URI do diretório que contém o arquivo
     * @return Nome do console ou null se não encontrado
     */
    public static String readConsoleNameFromMetadata(Context context, Uri directoryUri) {
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return null;
        }

        // Procura pelo arquivo metadata.pegasus.txt
        DocumentFile metadataFile = null;
        DocumentFile[] files = directory.listFiles();
        
        for (DocumentFile file : files) {
            if (file != null && file.isFile() && "metadata.pegasus.txt".equals(file.getName())) {
                metadataFile = file;
                break;
            }
        }
        
        if (metadataFile == null) {
            return null;
        }

        try (InputStream inputStream = context.getContentResolver().openInputStream(metadataFile.getUri());
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("collection:")) {
                    // Remove "collection:" e espaços em branco
                    String consoleName = line.substring("collection:".length()).trim();
                    return consoleName.isEmpty() ? null : consoleName;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Lê todos os arquivos .desktop do diretório e extrai os nomes dos jogos usando SAF
     * @param context Contexto da aplicação
     * @param directoryUri URI do diretório
     * @return Lista de nomes dos jogos
     */
    public static List<String> readGameNamesFromDesktopFiles(Context context, Uri directoryUri) {
        List<String> gameNames = new ArrayList<>();
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);

        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return gameNames;
        }

        DocumentFile[] files = directory.listFiles();
        
        if (files == null) {
            return gameNames;
        }

        for (DocumentFile file : files) {
            if (file != null && file.isFile() && file.getName() != null && 
                file.getName().toLowerCase().endsWith(".desktop")) {
                String gameName = readGameNameFromDesktopFile(context, file);
                if (gameName != null && !gameName.isEmpty()) {
                    gameNames.add(gameName);
                }
            }
        }

        return gameNames;
    }

    /**
     * Lê um arquivo .desktop específico e extrai o nome do jogo usando SAF
     * @param context Contexto da aplicação
     * @param desktopFile DocumentFile do arquivo .desktop
     * @return Nome do jogo ou null se não encontrado
     */
    private static String readGameNameFromDesktopFile(Context context, DocumentFile desktopFile) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(desktopFile.getUri());
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            String line;
            boolean inDesktopEntry = false;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Verifica se estamos na seção [Desktop Entry]
                if (line.equals("[Desktop Entry]")) {
                    inDesktopEntry = true;
                    continue;
                } else if (line.startsWith("[") && line.endsWith("]") && !line.equals("[Desktop Entry]")) {
                    // Saiu da seção [Desktop Entry]
                    inDesktopEntry = false;
                    continue;
                }
                
                // Se estamos na seção correta e encontramos a linha Name=
                if (inDesktopEntry && line.startsWith("Name=")) {
                    String gameName = line.substring("Name=".length()).trim();
                    return gameName.isEmpty() ? null : gameName;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Cria o diretório para as imagens do jogo usando SAF na pasta Pegasus
     * @param context Contexto da aplicação
     * @param pegasusDirectoryUri URI do diretório Pegasus (onde está metadata.pegasus.txt)
     * @param gameName Nome do jogo
     * @return DocumentFile do diretório criado ou null se falhou
     */
    public static DocumentFile createGameMediaDirectory(Context context, Uri pegasusDirectoryUri, String gameName) {
        DocumentFile pegasusRoot = DocumentFile.fromTreeUri(context, pegasusDirectoryUri);
        
        if (pegasusRoot == null || !pegasusRoot.exists() || !pegasusRoot.isDirectory()) {
            return null;
        }
        
        // Procura ou cria pasta "media"
        DocumentFile mediaDir = null;
        DocumentFile[] existingDirs = pegasusRoot.listFiles();
        for (DocumentFile dir : existingDirs) {
            if (dir != null && dir.isDirectory() && "media".equals(dir.getName())) {
                mediaDir = dir;
                break;
            }
        }
        
        if (mediaDir == null) {
            // Cria pasta "media"
            mediaDir = pegasusRoot.createDirectory("media");
            if (mediaDir == null) {
                return null;
            }
        }
        
        // Procura ou cria pasta do jogo dentro de "media"
        DocumentFile[] gameDirs = mediaDir.listFiles();
        for (DocumentFile dir : gameDirs) {
            if (dir != null && dir.isDirectory() && gameName.equals(dir.getName())) {
                return dir; // Diretório do jogo já existe
            }
        }
        
        // Cria novo diretório do jogo
        return mediaDir.createDirectory(gameName);
    }

    /**
     * Obtém ou cria o diretório de um jogo específico
     * @param context Contexto da aplicação
     * @param pegasusDirectoryUri URI do diretório Pegasus
     * @param gameName Nome do jogo
     * @return DocumentFile do diretório do jogo ou null se não encontrado
     */
    public static DocumentFile getGameDirectory(Context context, Uri pegasusDirectoryUri, String gameName) {
        DocumentFile pegasusRoot = DocumentFile.fromTreeUri(context, pegasusDirectoryUri);
        
        if (pegasusRoot == null || !pegasusRoot.exists() || !pegasusRoot.isDirectory()) {
            return null;
        }
        
        // Procura pasta "media"
        DocumentFile mediaDir = null;
        DocumentFile[] existingDirs = pegasusRoot.listFiles();
        for (DocumentFile dir : existingDirs) {
            if (dir != null && dir.isDirectory() && "media".equals(dir.getName())) {
                mediaDir = dir;
                break;
            }
        }
        
        if (mediaDir == null) {
            return null;
        }
        
        // Procura pasta do jogo
        DocumentFile[] gameDirs = mediaDir.listFiles();
        for (DocumentFile dir : gameDirs) {
            if (dir != null && dir.isDirectory() && gameName.equals(dir.getName())) {
                return dir;
            }
        }
        
        return null;
    }

    /**
     * Verifica se a imagem boxFront.png existe para um jogo usando SAF
     * @param context Contexto da aplicação
     * @param gameDirectory DocumentFile do diretório do jogo
     * @return DocumentFile da imagem ou null se não existe
     */
    public static DocumentFile getBoxFrontImage(Context context, DocumentFile gameDirectory) {
        if (gameDirectory == null || !gameDirectory.exists() || !gameDirectory.isDirectory()) {
            return null;
        }
        
        DocumentFile[] files = gameDirectory.listFiles();
        for (DocumentFile file : files) {
            if (file != null && file.isFile() && "boxFront.png".equals(file.getName())) {
                return file;
            }
        }
        
        return null;
    }

    /**
     * Cria ou substitui a imagem boxFront.png no diretório do jogo
     * @param context Contexto da aplicação
     * @param gameDirectory DocumentFile do diretório do jogo
     * @return DocumentFile da imagem criada ou null se falhou
     */
    public static DocumentFile createBoxFrontImage(Context context, DocumentFile gameDirectory) {
        if (gameDirectory == null || !gameDirectory.exists() || !gameDirectory.isDirectory()) {
            return null;
        }
        
        // Remove imagem existente se houver
        DocumentFile existingImage = getBoxFrontImage(context, gameDirectory);
        if (existingImage != null) {
            existingImage.delete();
        }
        
        // Cria nova imagem
        return gameDirectory.createFile("image/png", "boxFront.png");
    }
}