package com.example.nemergentprueba.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

/**
 * DAO (Data Access Object) para manejar operaciones de base de datos relacionadas con fotos.
 */
@Dao
public interface PhotoDao {

    /**
     * Inserta una nueva foto en la base de datos
     * @param photo La entidad de foto a insertar
     * @return ID generado para la nueva entrada
     */
    @Insert
    long insertPhoto(PhotoEntity photo);

    /**
     * Obtiene todas las fotos ordenadas por fecha de captura (más recientes primero)
     * @return Lista de todas las fotos como LiveData
     */
    @Query("SELECT * FROM photos ORDER BY captureDate DESC")
    LiveData<List<PhotoEntity>> getAllPhotos();

    /**
     * Obtiene todas las fotos tomadas entre dos fechas
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de fotos en el rango de fechas
     */
    @Query("SELECT * FROM photos WHERE captureDate BETWEEN :startDate AND :endDate ORDER BY captureDate DESC")
    List<PhotoEntity> getPhotosBetweenDates(Date startDate, Date endDate);

    /**
     * Busca fotos en un rango de coordenadas específico
     * @param minLat Latitud mínima
     * @param maxLat Latitud máxima
     * @param minLong Longitud mínima
     * @param maxLong Longitud máxima
     * @return Lista de fotos dentro de las coordenadas especificadas
     */
    @Query("SELECT * FROM photos WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLong AND :maxLong")
    List<PhotoEntity> getPhotosByLocation(double minLat, double maxLat, double minLong, double maxLong);

    /**
     * Actualiza información de una foto existente
     * @param photo La entidad de foto con los datos actualizados
     */
    @Update
    void updatePhoto(PhotoEntity photo);

    /**
     * Elimina una foto de la base de datos
     * @param photo La entidad de foto a eliminar
     */
    @Delete
    void deletePhoto(PhotoEntity photo);

    /**
     * Elimina una foto por su ID
     * @param photoId ID de la foto a eliminar
     */
    @Query("DELETE FROM photos WHERE id = :photoId")
    void deletePhotoById(long photoId);
}