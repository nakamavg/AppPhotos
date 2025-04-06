package com.example.nemergentprueba;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.example.nemergentprueba.camera.CameraActivity;

public class MainActivity extends AppCompatActivity {

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
        cameraButton.setOnClickListener(view -> openCamera());

        // Configurar listener para el botón de galería
        View galleryButton = findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(view -> openGallery());
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
        // La implementación de la galería se hará en el siguiente paso
        // Por ahora solo mostramos un mensaje
        // Intent intent = new Intent(this, GalleryActivity.class);
        // startActivity(intent);
        
        // Mensaje temporal hasta implementar la galería
        Toast.makeText(this, "La galería se implementará próximamente", Toast.LENGTH_SHORT).show();
    }
}