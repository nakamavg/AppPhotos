package com.example.nemergentprueba.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.nemergentprueba.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase auxiliar para gestionar permisos en la aplicación.
 */
public class PermissionHelper {

    public static final int PERMISSION_REQUEST_CODE = 100;

    /**
     * Verifica y solicita permisos si es necesario
     * 
     * @param activity La actividad desde donde se solicitan los permisos
     * @param permissions Lista de permisos a solicitar
     * @param requestCode Código de solicitud para onRequestPermissionsResult
     * @return true si ya se tienen todos los permisos, false si se están solicitando
     */
    public static boolean requestPermissions(Activity activity, String[] permissions, int requestCode) {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Verificar qué permisos son necesarios solicitar
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        // Si no se necesita solicitar permisos, retornar true
        if (permissionsNeeded.isEmpty()) {
            return true;
        }
        
        // Verificar si debemos mostrar explicación para algún permiso
        boolean shouldShowRationale = false;
        for (String permission : permissionsNeeded) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
                break;
            }
        }
        
        // Mostrar diálogo explicativo si es necesario
        if (shouldShowRationale) {
            showPermissionRationaleDialog(activity, permissionsNeeded.toArray(new String[0]), requestCode);
        } else {
            // Solicitar permisos directamente
            ActivityCompat.requestPermissions(activity, permissionsNeeded.toArray(new String[0]), requestCode);
        }
        
        return false;
    }
    
    /**
     * Muestra un diálogo explicando por qué se necesitan los permisos
     */
    private static void showPermissionRationaleDialog(Activity activity, String[] permissions, int requestCode) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> 
                        ActivityCompat.requestPermissions(activity, permissions, requestCode))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * Maneja la respuesta de la solicitud de permisos
     * 
     * @return true si todos los permisos fueron concedidos
     */
    public static boolean handlePermissionResult(Activity activity, int requestCode, 
                                              @NonNull String[] permissions, 
                                              @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            
            // Verificar si todos los permisos fueron concedidos
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                return true;
            } else {
                // Verificar si debemos mostrar dialogo para ir a configuración
                for (String permission : permissions) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                        // El usuario seleccionó "No volver a preguntar"
                        showSettingsDialog(activity);
                        return false;
                    }
                }
                
                // El usuario rechazó pero podemos volver a preguntar
                Toast.makeText(activity, R.string.permissions_required, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return false;
    }
    
    /**
     * Muestra un diálogo para ir a la configuración de la aplicación
     */
    public static void showSettingsDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_settings_message)
                .setPositiveButton(R.string.settings, (dialog, which) -> {
                    // Abrir configuración de la aplicación
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    /**
     * Determina si el dispositivo es un Xiaomi
     */
    public static boolean isXiaomiDevice() {
        return Build.MANUFACTURER.toLowerCase().contains("xiaomi") || 
               Build.BRAND.toLowerCase().contains("xiaomi") ||
               Build.BRAND.toLowerCase().contains("redmi");
    }
    
    /**
     * Obtiene los permisos necesarios para la cámara y almacenamiento
     */
    public static String[] getCameraPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Permiso de cámara siempre es necesario
        permissions.add(Manifest.permission.CAMERA);
        
        // Permisos de almacenamiento según versión de Android
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        
        return permissions.toArray(new String[0]);
    }
}