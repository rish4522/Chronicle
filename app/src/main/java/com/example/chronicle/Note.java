package com.example.chronicle;

import com.google.firebase.Timestamp;

import java.util.Date;

public class Note {
    String title;
    String content;
    Timestamp timestamp;
    private boolean isFavorite;
    private String imageUrl;
    private String selectedTime;
    private int selectedMood;


    public Note() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public String getSelectedTime() {
        return selectedTime;
    }
    public void setSelectedTime(String selectedTime) {
        this.selectedTime = selectedTime;
    }
    public int getSelectedMood() {
        return selectedMood;
    }

    public void setSelectedMood(int selectedMood) {
        this.selectedMood = selectedMood;
    }

}
