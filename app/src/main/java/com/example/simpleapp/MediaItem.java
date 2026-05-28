package com.example.simpleapp;

import android.net.Uri;

public class MediaItem {
    private String filePath;      // Путь к файлу
    private String fileName;      // Имя файла
    private String dateCreated;   // Дата создания (dd.MM.yyyy)
    private String mediaType;     // "photo" или "video"
    private Uri contentUri;       // URI для отображения

    public MediaItem(String filePath, String fileName, String dateCreated, String mediaType, Uri contentUri) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.dateCreated = dateCreated;
        this.mediaType = mediaType;
        this.contentUri = contentUri;
    }

    // Геттеры
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public String getDateCreated() { return dateCreated; }
    public String getMediaType() { return mediaType; }
    public Uri getContentUri() { return contentUri; }
}