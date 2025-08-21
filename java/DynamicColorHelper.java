package com.example.pegasusimagemanager;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import com.google.android.material.color.DynamicColors;

public class DynamicColorHelper {

    /**
     * Aplica Dynamic Color usando Material You se disponível (Android 12+)
     * @param activity A activity onde aplicar as cores dinâmicas
     */
    public static void applyDynamicColors(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ suporta Dynamic Color
            DynamicColors.applyToActivityIfAvailable(activity);
        }
    }

    /**
     * Verifica se Dynamic Color está disponível no dispositivo
     * @param context Contexto da aplicação
     * @return true se Dynamic Color está disponível
     */
    public static boolean isDynamicColorAvailable(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return DynamicColors.isDynamicColorAvailable();
        }
        return false;
    }

    /**
     * Verifica se o usuário habilitou as cores dinâmicas nas configurações
     * @param context Contexto da aplicação
     * @return true se as cores dinâmicas estão habilitadas
     */
    public static boolean isDynamicColorEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return DynamicColors.isDynamicColorAvailable();
        }
        return false;
    }

    /**
     * Verifica se o dispositivo está em modo escuro
     * @param context Contexto da aplicação
     * @return true se está em modo escuro
     */
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}