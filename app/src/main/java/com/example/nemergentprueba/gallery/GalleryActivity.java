package com.example.nemergentprueba.gallery;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.database.PhotoEntity;
import com.example.nemergentprueba.database.PhotoRepository;

import java.io.File;
import java.util.List;

public class GalleryActivity extends AppCompatActivity implements PhotoAdapter.OnPhotoDeleteListener {

    private static final String TAG = "GalleryActivity";
    private RecyclerView photoRecyclerView;
    private PhotoAdapter photoAdapter;
    private TextView emptyGalleryMessage;
    private PhotoRepository photoRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        // Inicializar vistas
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        emptyGalleryMessage = findViewById(R.id.emptyGalleryMessage);

        // Configurar RecyclerView
        photoRecyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columnas
        photoAdapter = new PhotoAdapter(this);
        photoAdapter.setOnPhotoDeleteListener(this);
        photoRecyclerView.setAdapter(photoAdapter);

        // Inicializar el repositorio
        photoRepository = new PhotoRepository(this);

        // Observar cambios en la lista de fotos
        observePhotosList();
    }

    private void observePhotosList() {
        LiveData<List<PhotoEntity>> photosLiveData = photoRepository.getAllPhotos();
        
        // Observer que actualiza la UI cuando cambia la lista de fotos
        photosLiveData.observe(this, photos -> {
            if (photos != null && !photos.isEmpty()) {
                photoAdapter.setPhotos(photos);
                showPhotoList();
            } else {
                showEmptyState();
            }
        });
    }

    private void showPhotoList() {
        photoRecyclerView.setVisibility(View.VISIBLE);
        emptyGalleryMessage.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        photoRecyclerView.setVisibility(View.GONE);
        emptyGalleryMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPhotoDelete(PhotoEntity photo, int position) {
        // Mostrar diálogo de confirmación
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_photo)
                .setMessage(R.string.confirm_delete_photo)
                .setPositiveButton(R.string.ok, (dialog, which) -> deletePhoto(photo, position))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void deletePhoto(PhotoEntity photo, int position) {
        boolean fileDeleted = false;
        String path = photo.getRelativePath();
        
        try {
            // Intentar eliminar según la versión de Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (path.startsWith("content://")) {
                    // Es una URI de MediaStore
                    ContentResolver resolver = getContentResolver();
                    Uri photoUri = Uri.parse(path);
                    int rowsDeleted = resolver.delete(photoUri, null, null);
                    fileDeleted = rowsDeleted > 0;
                } else {
                    // Es una ruta de archivo
                    File photoFile = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM), path.replace("DCIM/", ""));
                    fileDeleted = photoFile.exists() && photoFile.delete();
                }
            } else {
                // Para versiones anteriores
                File photoFile;
                if (path.startsWith("DCIM/")) {
                    photoFile = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM), path.replace("DCIM/", ""));
                } else {
                    photoFile = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM), "Camera/" + path);
                }
                fileDeleted = photoFile.exists() && photoFile.delete();
            }
            
            // Eliminar de la base de datos independientemente de si se eliminó el archivo
            photoRepository.deletePhoto(photo);
            
            // Actualizar adapter
            photoAdapter.removePhoto(position);
            
            // Verificar si la lista quedó vacía después de eliminar
            if (photoAdapter.getItemCount() == 0) {
                showEmptyState();
            }
            
            Toast.makeText(this, fileDeleted ? 
                    R.string.photo_deleted : R.string.error_deleting_photo, 
                    Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar foto: " + e.getMessage(), e);
            Toast.makeText(this, R.string.error_deleting_photo, Toast.LENGTH_SHORT).show();
        }
    }
}