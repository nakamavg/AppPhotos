package com.example.nemergentprueba;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.example.nemergentprueba.camera.CameraActivity;
import com.example.nemergentprueba.gallery.GalleryActivity;
import com.example.nemergentprueba.network.PingDialogFragment;
import com.example.nemergentprueba.utils.PermissionHelper;
import com.example.nemergentprueba.utils.XiaomiHelper;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSIONS = 100;
    private static final int REQUEST_GALLERY_PERMISSIONS = 101;
    private boolean pendingCameraLaunch = false;
    private boolean pendingGalleryLaunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Asegurar que esta actividad se muestre en el launcher de MIUI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createAppShortcuts();
        }

        // Configurar listener para el botón de cámara
        View cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(view -> checkAndRequestCameraPermissions());

        // Configurar listener para el botón de galería
        View galleryButton = findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(view -> checkAndRequestGalleryPermissions());
        
        // Configurar listener para el botón de ping
        View pingButton = findViewById(R.id.ping_button);
        pingButton.setOnClickListener(view -> openPingDialog());
        
        // Mostrar ayuda específica para dispositivos Xiaomi si es necesario
        if (XiaomiHelper.isXiaomiDevice()) {
            XiaomiHelper.showXiaomiInstallationHelp(this);
        }
    }

    /**
     * Verifica y solicita permisos para la cámara
     */
    private void checkAndRequestCameraPermissions() {
        String[] permissions = PermissionHelper.getCameraPermissions();
        if (PermissionHelper.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSIONS)) {
            openCamera();
        } else {
            pendingCameraLaunch = true;
        }
    }
    
    /**
     * Verifica y solicita permisos para la galería
     */
    private void checkAndRequestGalleryPermissions() {
        // Para la galería solo necesitamos permisos de lectura en versiones antiguas
        String[] permissions = new String[0];
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions = new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        
        if (permissions.length == 0 || PermissionHelper.requestPermissions(this, permissions, REQUEST_GALLERY_PERMISSIONS)) {
            openGallery();
        } else {
            pendingGalleryLaunch = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            if (PermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults)) {
                if (pendingCameraLaunch) {
                    pendingCameraLaunch = false;
                    openCamera();
                }
            }
        } else if (requestCode == REQUEST_GALLERY_PERMISSIONS) {
            if (PermissionHelper.handlePermissionResult(this, requestCode, permissions, grantResults)) {
                if (pendingGalleryLaunch) {
                    pendingGalleryLaunch = false;
                    openGallery();
                }
            }
        }
    }

    /**
     * Crea accesos directos de la aplicación para mejorar la visibilidad en MIUI
     */
    private void createAppShortcuts() {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            // Crear un acceso directo para la actividad principal
            ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(this, "main_shortcut")
                    .setShortLabel(getString(R.string.app_name))
                    .setLongLabel(getString(R.string.app_name))
                    .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
                    .setIntent(new Intent(this, MainActivity.class)
                            .setAction(Intent.ACTION_VIEW))
                    .build();

            ShortcutManagerCompat.requestPinShortcut(this, shortcut, null);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    private void openPingDialog() {
        PingDialogFragment pingDialogFragment = new PingDialogFragment();
        pingDialogFragment.show(getSupportFragmentManager(), "PingDialogFragment");
    }
}