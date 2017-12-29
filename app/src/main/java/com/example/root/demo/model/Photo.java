package com.example.root.demo.model;

import android.graphics.Bitmap;

import java.util.List;

public class Photo {
    private String id;
    private String name;
    private String description;
    private Bitmap photoBmp;

    public Photo() {}

    public Photo(String id, String name, String description, Bitmap photoBmp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.photoBmp = photoBmp;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Bitmap getPhotoBmp() {
        return photoBmp;
    }

    public void setPhotoBmp(Bitmap photoBmp) {
        this.photoBmp = photoBmp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }



}
