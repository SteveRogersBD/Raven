package com.example.plateit.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = { PantryItem.class, VideoEntity.class }, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PantryDao pantryDao();

    public abstract VideoDao videoDao();
}
