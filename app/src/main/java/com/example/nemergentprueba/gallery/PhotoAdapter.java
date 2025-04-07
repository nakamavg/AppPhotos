package com.example.nemergentprueba.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nemergentprueba.R;
import com.example.nemergentprueba.database.PhotoEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final Context context;
    private final List<PhotoEntity> photos;
    private final SimpleDateFormat dateFormat;
    private OnPhotoDeleteListener deleteListener;
    private LruCache<String, Bitmap> memoryCache;

    public PhotoAdapter(Context context) {
        this.context = context;
        this.photos = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat(context.getString(R.string.date_time_format), Locale.getDefault());
        
        // Inicializar cache de memoria para imágenes
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // El tamaño del cache se mide en kilobytes
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public void setOnPhotoDeleteListener(OnPhotoDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.photo_item, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PhotoEntity photo = photos.get(position);
        
        // Cargar la imagen de forma asíncrona
        loadBitmapAsync(photo, holder.photoImageView);
        
        // Configurar fecha y ubicación usando strings localizados
        holder.dateCapturedTextView.setText(context.getString(
            R.string.photo_date_label, 
            dateFormat.format(photo.getCaptureDate())
        ));
        
        holder.locationTextView.setText(context.getString(
            R.string.photo_location_label,
            photo.getLatitude(),
            photo.getLongitude()
        ));
        
        // Configurar botón de eliminación
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onPhotoDelete(photo, position);
            }
        });
    }

    private void loadBitmapAsync(PhotoEntity photo, ImageView imageView) {
        String photoPath = photo.getRelativePath();
        
        // Intentar obtener primero del cache
        Bitmap cachedBitmap = getBitmapFromMemCache(photoPath);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }
        
        // Si no está en cache, cargar de forma asíncrona
        new BitmapWorkerTask(imageView).execute(photo);
    }
    
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }
    
    // Tarea asíncrona para cargar imágenes sin bloquear la UI
    private class BitmapWorkerTask extends AsyncTask<PhotoEntity, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private PhotoEntity photoEntity = null;

        BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(PhotoEntity... params) {
            photoEntity = params[0];
            String path = photoEntity.getRelativePath();
            Bitmap bitmap = null;
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Para Android 10 (API 29) y superior, usar MediaStore
                    if (path.startsWith("content://")) {
                        Uri imageUri = Uri.parse(path);
                        try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                            if (is != null) {
                                bitmap = BitmapFactory.decodeStream(is);
                            }
                        }
                    } else {
                        // Es una ruta de archivo
                        File photoFile = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DCIM), path.replace("DCIM/", ""));
                        if (photoFile.exists()) {
                            bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                        }
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
                    
                    if (photoFile.exists()) {
                        bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (bitmap != null) {
                // Agregar al cache
                addBitmapToMemoryCache(path, bitmap);
            }
            
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageViewReference != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    public void setPhotos(List<PhotoEntity> newPhotos) {
        this.photos.clear();
        if (newPhotos != null) {
            this.photos.addAll(newPhotos);
        }
        notifyDataSetChanged();
    }

    public void removePhoto(int position) {
        if (position >= 0 && position < photos.size()) {
            PhotoEntity removed = photos.remove(position);
            // Eliminar también del cache si existe
            if (removed != null) {
                memoryCache.remove(removed.getRelativePath());
            }
            notifyItemRemoved(position);
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        TextView dateCapturedTextView;
        TextView locationTextView;
        Button deleteButton;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImageView = itemView.findViewById(R.id.photoImageView);
            dateCapturedTextView = itemView.findViewById(R.id.dateCapturedTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    public interface OnPhotoDeleteListener {
        void onPhotoDelete(PhotoEntity photo, int position);
    }
}