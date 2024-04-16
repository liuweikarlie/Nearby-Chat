package top.xrondev.lab.nearbychat.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import top.xrondev.lab.nearbychat.R;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {
    private final List<String> channels; // Assuming channels are a list of strings
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // Constructor
    public ChannelAdapter(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.channels = data;
    }

    public String getItem(int id) {
        return channels.get(id);
    }

    // Method that binds the interface through setter
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.myTextView.setText(channels.get(position));
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    // Interface for click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = itemView.findViewById(R.id.textView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }
}
