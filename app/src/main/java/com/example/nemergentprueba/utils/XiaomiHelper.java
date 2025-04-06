package com.example.nemergentprueba.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * Utilidad para mostrar instrucciones específicas para dispositivos Xiaomi,
 * que a menudo tienen restricciones adicionales de seguridad.
 */
public class XiaomiHelper {

    /**
     * Muestra un diálogo con instrucciones específicas para Xiaomi cuando hay problemas de instalación
     * @param context Contexto de la aplicación
     */
    public static void showXiaomiInstallationHelp(Context context) {
        new AlertDialog.Builder(context)
            .setTitle("Configuración de seguridad adicional")
            .setMessage("Tu dispositivo Xiaomi puede requerir configuración adicional para instalar la aplicación:\n\n" +
                        "1. Ve a Ajustes > Seguridad > Instalar apps de fuentes desconocidas\n" + 
                        "2. Asegúrate que la opción 'Verificar aplicaciones' está desactivada\n" +
                        "3. En MIUI 12+, activa 'Instalar vía USB' en Opciones de desarrollador\n\n" +
                        "¿Quieres abrir los ajustes de seguridad ahora?")
            .setPositiveButton("Abrir Ajustes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    openSecuritySettings(context);
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    /**
     * Abre la configuración de seguridad del dispositivo
     */
    private static void openSecuritySettings(Context context) {
        try {
            // Intentar abrir directamente la configuración de seguridad de MIUI
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return;
            }
            
            // Si no funciona, intentar con los ajustes generales de seguridad
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return;
            }
            
            // Como última opción, abrir los ajustes generales
            context.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception e) {
            // Si nada funciona, intentar con los ajustes generales
            context.startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
    
    /**
     * Comprueba si el dispositivo es un Xiaomi con MIUI
     */
    public static boolean isXiaomiDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("xiaomi") || 
               Build.BRAND.toLowerCase().contains("xiaomi") ||
               Build.BRAND.toLowerCase().contains("redmi");
    }
}