package com.example.nemergentprueba.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Clase principal de la base de datos Room.
 * Define las entidades, versión y proporciona acceso a los DAOs.
 */
@Database(entities = {PhotoEntity.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    // Singleton para evitar múltiples instancias de la base de datos
    private static volatile AppDatabase INSTANCE;

    // DAO
    public abstract PhotoDao photoDao();

    // Método para obtener la instancia única de la base de datos
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "photo_database")
                            .fallbackToDestructiveMigration() // Si cambia la versión, recrea la BD
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}