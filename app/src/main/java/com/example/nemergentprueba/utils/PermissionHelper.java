package com.example.nemergentprueba.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.nemergentprueba.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase auxiliar para manejar permisos en Android, especialmente optimizada
 * para dispositivos problemáticos como Xiaomi.
 */
public class PermissionHelper {
    private static final String TAG = "PermissionHelper";
    public static final int PERMISSION_REQUEST_CODE = 100;

    /**
     * Verifica si todos los permisos necesarios han sido concedidos
     * @param context Contexto de la aplicación
     * @param permissions Array de permisos a verificar
     * @return true si todos los permisos están concedidos, false si no
     */
    public static boolean hasPermissions(Context context, String[] permissions) {
        if (context == null || permissions == null) {
            return false;
        }
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Solicita permisos y maneja diferentes escenarios
     * @param activity Actividad desde donde se solicitan los permisos
     * @param permissions Array de permisos a solicitar
     * @param requestCode Código de solicitud para onRequestPermissionsResult
     * @return true si ya se tienen todos los permisos, false si se está solicitando
     */
    public static boolean requestPermissions(Activity activity, String[] permissions, int requestCode) {
        if (hasPermissions(activity, permissions)) {
            return true;
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity,
                    permissionsToRequest.toArray(new String[0]),
                    requestCode
            );
            return false;
        }
        
        return true;
    }

    /**
     * Maneja la respuesta a la solicitud de permisos
     * @param activity Actividad que recibe la respuesta
     * @param requestCode Código de solicitud
     * @param permissions Permisos solicitados
     * @param grantResults Resultados de la solicitud
     * @param onPermissionGranted Runnable a ejecutar si todos los permisos son concedidos
     */
    public static void handlePermissionResult(
            Activity activity,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults,
            Runnable onPermissionGranted
    ) {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }

        boolean allGranted = true;
        List<String> deniedPermissions = new ArrayList<>();
        
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                deniedPermissions.add(permissions[i]);
                Log.d(TAG, "Permiso denegado: " + permissions[i]);
            }
        }

        if (allGranted) {
            if (onPermissionGranted != null) {
                onPermissionGranted.run();
            }
        } else {
            // Verificar si deberíamos mostrar explicación
            boolean shouldShowRationale = false;
            for (String permission : deniedPermissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }

            if (shouldShowRationale) {
                showPermissionExplanationDialog(activity, permissions);
            } else {
                // El usuario marcó "No volver a preguntar", necesitamos dirigirlo a la configuración
                showSettingsDialog(activity);
            }
        }
    }

    /**
     * Muestra un diálogo explicando por qué se necesitan los permisos
     */
    private static void showPermissionExplanationDialog(Activity activity, String[] permissions) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> 
                        requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE))
                .setNegativeButton(R.string.cancel, (dialog, which) -> 
                        activity.finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Muestra un diálogo para dirigir al usuario a la configuración de la aplicación
     */
    private static void showSettingsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_required_title)
                .setMessage(R.string.permission_required_settings_message)
                .setPositiveButton(R.string.settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                    activity.finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> 
                        activity.finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Método especial para dispositivos Xiaomi, que a veces requieren
     * permisos adicionales o manejo especial.
     */
    public static boolean isXiaomiDevice() {
        String manufacturer = Build.MANUFACTURER;
        String brand = Build.BRAND;
        return manufacturer.equalsIgnoreCase("Xiaomi") || 
               brand.equalsIgnoreCase("Xiaomi") || 
               brand.equalsIgnoreCase("Redmi");
    }

    /**
     * Abre la pantalla de permisos específica de MIUI (para dispositivos Xiaomi)
     */
    public static void openMIUIPermissionSettings(Activity activity) {
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.putExtra("extra_pkgname", activity.getPackageName());
            activity.startActivity(intent);
        } catch (Exception e) {
            // Si falla, usar el método estándar
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
            intent.setData(uri);
            activity.startActivity(intent);
        }
    }
}