package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class ChatMessage {
    public String message;
    public Bitmap imageDrawable;

    public ChatMessage(String message, Bitmap imageDrawable) {
        this.message = message;
        this.imageDrawable = imageDrawable;
    }

    public String getMessage() {
        return message;
    }

    public Bitmap getImageDrawable() {
        return imageDrawable;
    }
}