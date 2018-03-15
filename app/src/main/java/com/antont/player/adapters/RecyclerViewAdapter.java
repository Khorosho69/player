package com.antont.player.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.antont.player.R;
import com.antont.player.models.AudioItem;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private List<AudioItem> mAudioItems;
    private int mCurrentItemPosition;

    private Boolean isPlaying = false;

    private OnItemSelectedCallback mListener;

    public RecyclerViewAdapter(List<AudioItem> audioItems, int itemPosition, OnItemSelectedCallback listener) {
        this.mAudioItems = audioItems;
        this.mCurrentItemPosition = itemPosition;
        this.mListener = listener;
    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AudioItem item = mAudioItems.get(position);

        holder.mPositionTextView.setText(String.valueOf(position + 1));
        holder.mNameTextView.setText(item.getName());
        holder.mAlbumTextView.setText(item.getAlbumName());

        if (position != mCurrentItemPosition) {
            holder.mIsPlayView.setBackground(null);
        } else {
            if (isPlaying) {
                holder.mIsPlayView.setBackground(holder.itemView.getResources().getDrawable(android.R.drawable.ic_media_pause));
            } else {
                holder.mIsPlayView.setBackground(holder.itemView.getResources().getDrawable(android.R.drawable.ic_media_play));
            }
        }

        holder.itemView.setOnClickListener((View v) -> {
            changeCurrentSong(position);
            mListener.onItemSelected(mAudioItems.get(position));
        });
    }

    public void changeCurrentSong(int newTrackIndex) {
        isPlaying = true;
        if (mCurrentItemPosition != newTrackIndex) {
            notifyItemChanged(mCurrentItemPosition);
        }
        mCurrentItemPosition = newTrackIndex;
        notifyItemChanged(newTrackIndex);
    }

    @Override
    public int getItemCount() {
        return mAudioItems.size();
    }

    public void updatePlayingStatus(Boolean playing) {
        isPlaying = playing;
        notifyItemChanged(mCurrentItemPosition);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView mPositionTextView;
        TextView mNameTextView;
        TextView mAlbumTextView;
        View mIsPlayView;

        ViewHolder(View v) {
            super(v);
            mPositionTextView = v.findViewById(R.id.item_id);
            mNameTextView = v.findViewById(R.id.item_name);
            mAlbumTextView = v.findViewById(R.id.item_album);
            mIsPlayView = v.findViewById(R.id.is_play_image);
        }
    }

    public interface OnItemSelectedCallback {
        void onItemSelected(AudioItem audioItem);
    }
}