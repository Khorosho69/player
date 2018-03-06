package com.antont.player.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.antont.player.R;
import com.antont.player.models.AudioItem;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private List<AudioItem> mAudioItems;
    private int mPreviousItem = -1;

    public RecyclerViewAdapter(List<AudioItem> audioItems) {
        this.mAudioItems = audioItems;
    }



    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AudioItem item = mAudioItems.get(position);

        holder.mNumTextView.setText(String.valueOf(position + 1));
        holder.mNameTextView.setText(item.getName());
        holder.mAlbumTextView.setText(item.getAlbumName());

        if (position == mPreviousItem) {
            holder.mIsPlayView.setVisibility(View.VISIBLE);
        } else {
            holder.mIsPlayView.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener((View v) -> onItemSelected(holder, position));
    }

    private void onItemSelected(ViewHolder holder, int position) {
        holder.mIsPlayView.setVisibility(View.VISIBLE);
        notifyItemChanged(mPreviousItem);
        mPreviousItem = position;

        EventBus.getDefault().post(mAudioItems.get(position));
    }


    @Override
    public int getItemCount() {
        return mAudioItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView mNumTextView;
        TextView mNameTextView;
        TextView mAlbumTextView;
        View mIsPlayView;

        ViewHolder(View v) {
            super(v);
            mNumTextView = v.findViewById(R.id.item_id);
            mNameTextView = v.findViewById(R.id.item_name);
            mAlbumTextView = v.findViewById(R.id.item_album);
            mIsPlayView = v.findViewById(R.id.is_play_image);
        }
    }
}