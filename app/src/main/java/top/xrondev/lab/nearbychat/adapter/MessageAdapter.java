package top.xrondev.lab.nearbychat.adapter;

import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.nearby.connection.Payload;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import top.xrondev.lab.nearbychat.R;
import top.xrondev.lab.nearbychat.models.Message;
import top.xrondev.lab.nearbychat.models.MessageType;
import top.xrondev.lab.nearbychat.ui.chat.ChatActivity;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        switch (messages.get(position).getType()) {
            case TEXT:
                return 0;
            case IMAGE:
                return 1;
            case VIDEO:
                return 2;
            case AUDIO:
                return 3;
            case FILE:
                return 4;
            default:
                return -1;
        }
    }

    private boolean isShowUsername(int position) {
        return position == 0 || !messages.get(position).getSender().equals(messages.get(position - 1).getSender());
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0: // Text
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_text, parent, false);
                return new TextMessageViewHolder(view);
            case 1: // Image
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_image, parent, false);
                return new ImageMessageViewHolder(view);
//            case 2: // Video
//                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_video, parent, false);
//                return new VideoMessageViewHolder(view);
            case 3: // Audio
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_audio, parent, false);
                return new AudioMessageViewHolder(view);
            default:
                throw new IllegalArgumentException("Invalid view type");
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean showUsername = isShowUsername(position);
        switch (holder.getItemViewType()) {
            case 0:
                ((TextMessageViewHolder) holder).bind(message, showUsername);
                break;
            case 1:
                ((ImageMessageViewHolder) holder).bind(message);
                break;
//            case 2:
//                ((VideoMessageViewHolder) holder).bind(message);
//                break;
            case 3:
                ((AudioMessageViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    // ViewHolder classes
    public static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView usernameTextView;
        private final ConstraintLayout constraintLayout;

        public TextMessageViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textMessage);
            constraintLayout = (ConstraintLayout) itemView;
            usernameTextView = itemView.findViewById(R.id.tvUsername);
        }

        public void bind(Message message, boolean showUsername) {
            if (showUsername) {
                usernameTextView.setVisibility(View.VISIBLE);
                usernameTextView.setText(message.getSender());
            } else {
                usernameTextView.setVisibility(View.GONE);
            }

            Payload payload = message.getContent();
            String text = new String(payload.asBytes());
            textView.setText(text);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            // Set the gravity of the TextView based on who sent the message
            if (message.isFromMe()) {
                constraintSet.clear(textView.getId(), ConstraintSet.START);
                constraintSet.connect(textView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                textView.setSelected(true); // This will apply the 'me' background state
            } else {
                constraintSet.clear(textView.getId(), ConstraintSet.END);
                constraintSet.connect(textView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                textView.setSelected(false); // Apply the 'other' background state
            }

            constraintSet.applyTo(constraintLayout);
        }
    }


    public static class ImageMessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private ConstraintLayout constraintLayout;

        public ImageMessageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageMessage);
            constraintLayout = (ConstraintLayout) itemView;
        }

        public void bind(Message message) {
            // Use Picasso or another library to load the image from a URL or resource
            Log.d("ImageMessageViewHolder", "bind: " + message.getContent());
            Uri file = Objects.requireNonNull(message.getContent().asFile()).asUri();
            Log.d("ImageMessageViewHolder", "URI: " + file);
            Picasso.get().load(file).into(imageView);


            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            if (message.isFromMe()) {
                constraintSet.clear(imageView.getId(), ConstraintSet.START);
                constraintSet.connect(imageView.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                imageView.setSelected(true); // Apply 'me' background state
            } else {
                constraintSet.clear(imageView.getId(), ConstraintSet.END);
                constraintSet.connect(imageView.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
                imageView.setSelected(false); // Apply 'other' background state
            }

            constraintSet.applyTo(constraintLayout);
        }
    }

    public static class AudioMessageViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView playButton;
        private ConstraintLayout constraintLayout;
        private MediaPlayer mediaPlayer;
        private boolean isPlaying;
        private Message message;

        public AudioMessageViewHolder(View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.playButton);
            constraintLayout = (ConstraintLayout) itemView;
            playButton.setOnClickListener(this);
        }

        public void bind(Message message) {
            this.message = message;

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);

            if (message.isFromMe()) {
                constraintSet.clear(playButton.getId(), ConstraintSet.START);
                constraintSet.connect(playButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
            } else {
                constraintSet.clear(playButton.getId(), ConstraintSet.END);
                constraintSet.connect(playButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START);
            }

            constraintSet.applyTo(constraintLayout);
        }

        @Override
        public void onClick(View v) {
            if (!isPlaying) {
                startPlaying();
            } else {
                stopPlaying();
            }
        }

        private void startPlaying() {
            if (message.getType() == MessageType.AUDIO) {
                Payload.File payloadFile = message.getContent().asFile();
                if (payloadFile != null) {
                    String filePath = payloadFile.asJavaFile().getPath();
                    mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(filePath);
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                        isPlaying = true;
                        playButton.setImageResource(R.drawable.ic_pause); // Update the button icon to pause
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void stopPlaying() {
            if (mediaPlayer != null && isPlaying) {
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                playButton.setImageResource(R.drawable.ic_play); // Update the button icon to play
            }
        }
    }

}

