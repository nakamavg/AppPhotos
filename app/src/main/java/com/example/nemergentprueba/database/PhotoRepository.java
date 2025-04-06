package com.example.nemergentprueba.database;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repositorio que maneja las operaciones de base de datos de forma asíncrona
 * para no bloquear el hilo principal.
 */
public class PhotoRepository {
    private final PhotoDao photoDao;
    private final LiveData<List<PhotoEntity>> allPhotos;
    private final ExecutorService executor;

    public PhotoRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        photoDao = db.photoDao();
        allPhotos = photoDao.getAllPhotos();
        // Crear un pool de hilos para operaciones asíncronas
        executor = Executors.newFixedThreadPool(4);
    }

    // Obtener todas las fotos como LiveData (observables)
    public LiveData<List<PhotoEntity>> getAllPhotos() {
        return allPhotos;
    }

    // Insertar una nueva foto
    public void insertPhoto(PhotoEntity photo, OnPhotoSavedListener listener) {
        executor.execute(() -> {
            long id = photoDao.insertPhoto(photo);
            if (listener != null) {
                photo.setId(id); // Asignar el ID generado
                listener.onPhotoSaved(photo);
            }
        });
    }

    // Actualizar foto existente
    public void updatePhoto(PhotoEntity photo) {
        executor.execute(() -> photoDao.updatePhoto(photo));
    }

    // Eliminar foto
    public void deletePhoto(PhotoEntity photo) {
        executor.execute(() -> photoDao.deletePhoto(photo));
    }

    // Eliminar foto por ID
    public void deletePhotoById(long photoId) {
        executor.execute(() -> photoDao.deletePhotoById(photoId));
    }

    // Interfaz de callback para notificar cuando se guarda una foto
    public interface OnPhotoSavedListener {
        void onPhotoSaved(PhotoEntity photo);
    }
}