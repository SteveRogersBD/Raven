package com.example.plateit.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.example.plateit.RecipeVideo;
import java.util.List;

@Dao
public interface VideoDao {
    @Query("SELECT * FROM video_recommendations")
    List<VideoEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VideoEntity> videos);

    @Query("DELETE FROM video_recommendations")
    void deleteAll();
}
