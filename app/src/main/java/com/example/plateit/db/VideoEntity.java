package com.example.plateit.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "video_recommendations")
public class VideoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String link;
    public String thumbnail;
    public String channel;
    public String views;
    public String length;

    public VideoEntity(String title, String link, String thumbnail, String channel, String views, String length) {
        this.title = title;
        this.link = link;
        this.thumbnail = thumbnail;
        this.channel = channel;
        this.views = views;
        this.length = length;
    }
}
