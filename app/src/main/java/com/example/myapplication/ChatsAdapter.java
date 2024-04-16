package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ChatsAdapter extends ArrayAdapter<ChatMessage> {
    private LayoutInflater inflater;

    public ChatsAdapter(Context context, ArrayList<ChatMessage> messages) {
        super(context, 0, messages);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.chat_item_layout, parent, false);
        }

        // Get the data item for this position
        ChatMessage chatMessage = getItem(position);

        // Check if the chatMessage is not null
        if (chatMessage != null) {
            String message = chatMessage.getMessage();
            Bitmap imageDrawable = chatMessage.getImageDrawable();

            // Find and update the views with the chat message and image
            TextView messageTextView = convertView.findViewById(R.id.message_text_view);
            ImageView imageView = convertView.findViewById(R.id.image_view);

            messageTextView.setText(message);
            imageView.setImageBitmap(imageDrawable);
        }

        return convertView;
    }
}
