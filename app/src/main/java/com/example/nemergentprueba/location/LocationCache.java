package com.example.nemergentprueba.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

/**
 * Clase para gestionar la ubicación con sistema de caché
 * Reduce las consultas al GPS cuando se toman varias fotos en sucesión rápida
 */
public class LocationCache {
    private static final String TAG = "LocationCache";
    
    // Constantes para la gestión de la caché
    private static final long MIN_TIME_BETWEEN_UPDATES = 30000; // 30 segundos en milisegundos
    private static final float MIN_DISTANCE_FOR_UPDATE = 10.0f; // 10 metros
    
    // Valores por defecto cuando no hay ubicación disponible
    private static final double DEFAULT_LATITUDE = 0.0;
    private static final double DEFAULT_LONGITUDE = 0.0;
    
    // Variables para la caché
    private Location lastKnownLocation;
    private long lastLocationTime;
    private boolean isRequestingLocation;
    
    // Referencias al sistema
    private LocationManager locationManager;
    private Context context;
    private LocationListener locationListener;
    
    // Instancia única (patrón Singleton)
    private static LocationCache instance;
    
    /**
     * Obtiene la instancia única de LocationCache
     */
    public static synchronized LocationCache getInstance(Context context) {
        if (instance == null) {
            instance = new LocationCache(context.getApplicationContext());
        }
        return instance;
    }
    
    private LocationCache(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.lastLocationTime = 0;
        this.isRequestingLocation = false;
        
        // Inicializar con una ubicación predeterminada
        this.lastKnownLocation = new Location("default");
        this.lastKnownLocation.setLatitude(DEFAULT_LATITUDE);
        this.lastKnownLocation.setLongitude(DEFAULT_LONGITUDE);
        
        // Crear el listener para actualizaciones de ubicación
        this.locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateCachedLocation(location);
                
                // Si estamos obteniendo actualizaciones continuas, detenerlas después de recibir una ubicación válida
                if (isRequestingLocation) {
                    stopLocationUpdates();
                }
            }
            
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // No necesitamos implementar esto para API nivel 29+
            }
            
            @Override
            public void onProviderEnabled(@NonNull String provider) {
                // Opcional: intenta obtener la ubicación cuando el proveedor se habilita
                if (provider.equals(LocationManager.GPS_PROVIDER) ||
                    provider.equals(LocationManager.NETWORK_PROVIDER)) {
                    requestLocationUpdate();
                }
            }
            
            @Override
            public void onProviderDisabled(@NonNull String provider) {
                // Opcional: manejar cuando se deshabilita el proveedor
            }
        };
        
        // Iniciar con una solicitud de ubicación para tener datos lo antes posible
        requestLocationUpdate();
    }
    
    /**
     * Actualiza la ubicación en caché
     */
    private synchronized void updateCachedLocation(Location location) {
        if (location != null) {
            Log.d(TAG, "Ubicación actualizada: " + location.getLatitude() + ", " + location.getLongitude());
            lastKnownLocation = location;
            lastLocationTime = SystemClock.elapsedRealtime();
        }
    }
    
    /**
     * Detiene las actualizaciones de ubicación para ahorrar batería
     */
    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            isRequestingLocation = false;
            Log.d(TAG, "Actualizaciones de ubicación detenidas");
        }
    }
    
    /**
     * Solicita una actualización de ubicación
     */
    private void requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != 
                PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != 
                PackageManager.PERMISSION_GRANTED) {
            // No tenemos permisos, no podemos continuar
            Log.w(TAG, "Sin permisos de ubicación");
            return;
        }
        
        isRequestingLocation = true;
        
        // Intentar primero con GPS para mayor precisión
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_FOR_UPDATE,
                locationListener
            );
            Log.d(TAG, "Solicitando actualizaciones GPS");
        }
        
        // También solicitar con NETWORK por si el GPS falla o tarda demasiado
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_FOR_UPDATE,
                locationListener
            );
            Log.d(TAG, "Solicitando actualizaciones NETWORK");
        }
        
        // Intentar obtener una ubicación de la última conocida (rápido, pero puede ser obsoleta)
        try {
            Location lastNetworkLocation = null;
            Location lastGpsLocation = null;
            
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lastGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            
            // Usar la más reciente de las dos
            if (lastGpsLocation != null && lastNetworkLocation != null) {
                if (lastGpsLocation.getTime() > lastNetworkLocation.getTime()) {
                    updateCachedLocation(lastGpsLocation);
                } else {
                    updateCachedLocation(lastNetworkLocation);
                }
            } else if (lastGpsLocation != null) {
                updateCachedLocation(lastGpsLocation);
            } else if (lastNetworkLocation != null) {
                updateCachedLocation(lastNetworkLocation);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener la última ubicación conocida", e);
        }
    }
    
    /**
     * Obtiene la ubicación actual o actualiza si es necesario
     * @return Un objeto Location con la ubicación actual
     */
    public synchronized Location getCurrentLocation() {
        long currentTime = SystemClock.elapsedRealtime();
        
        // Si ha pasado demasiado tiempo desde la última actualización, solicitar una nueva
        if (currentTime - lastLocationTime > MIN_TIME_BETWEEN_UPDATES || 
            lastKnownLocation.getLatitude() == DEFAULT_LATITUDE && 
            lastKnownLocation.getLongitude() == DEFAULT_LONGITUDE) {
            
            requestLocationUpdate();
        }
        
        return lastKnownLocation;
    }
    
    /**
     * Obtiene la latitud actual
     * @return Latitud de la ubicación cacheada
     */
    public double getLatitude() {
        return getCurrentLocation().getLatitude();
    }
    
    /**
     * Obtiene la longitud actual
     * @return Longitud de la ubicación cacheada
     */
    public double getLongitude() {
        return getCurrentLocation().getLongitude();
    }
    
    /**
     * Fuerza una actualización de ubicación, ignorando la caché
     */
    public void forceLocationUpdate() {
        stopLocationUpdates();
        requestLocationUpdate();
    }
    
    /**
     * Limpia los recursos cuando ya no se necesitan
     */
    public void cleanup() {
        stopLocationUpdates();
    }
}