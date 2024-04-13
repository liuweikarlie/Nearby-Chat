package com.example.myapplication;

import android.graphics.drawable.Drawable;

public class ChatMessage {
    public String message;
    public Drawable imageDrawable;

    public ChatMessage(String message, Drawable imageDrawable) {
        this.message = message;
        this.imageDrawable = imageDrawable;
    }

    public String getMessage() {
        return message;
    }

    public Drawable getImageDrawable() {
        return imageDrawable;
    }
}