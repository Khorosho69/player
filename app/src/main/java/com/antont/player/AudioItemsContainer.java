package com.antont.player;

import com.antont.player.models.AudioItem;

import java.util.ArrayList;
import java.util.List;

public class AudioItemsContainer {

    private static AudioItemsContainer mInstance;
    private List<AudioItem> mAudioItems;
    private AudioItem mCurrentSong;

    private AudioItemsContainer() {
    }

    public static AudioItemsContainer getInstance() {
        if (mInstance == null) {
            mInstance = new AudioItemsContainer();
        }
        return mInstance;
    }

    public List<AudioItem> getAudioItems() {
        if (mAudioItems == null) {
            mAudioItems = new ArrayList<>();
        }
        return mAudioItems;
    }

    public void setAudioItems(List<AudioItem> audioItems) {
        mAudioItems = audioItems;
    }

    public void skipToNextSong() {
        if (mCurrentSong == null) {
            mCurrentSong = mAudioItems.get(0);
            return;
        }
        if (mAudioItems.indexOf(mCurrentSong) == mAudioItems.size() - 1) {
            mCurrentSong = mAudioItems.get(0);
        } else {
            mCurrentSong = mAudioItems.get(mAudioItems.indexOf(mCurrentSong) + 1);
        }
    }

    public void skipToPreviousSong() {
        if (mCurrentSong == null) {
            mCurrentSong = mAudioItems.get(0);
            return;
        }
        if (mAudioItems.indexOf(mCurrentSong) == 0) {
            mCurrentSong = mAudioItems.get(mAudioItems.size() - 1);
        } else {
            mCurrentSong = mAudioItems.get(mAudioItems.indexOf(mCurrentSong) - 1);
        }
    }

    public int getCurrentSongIndex() {
        if (mCurrentSong == null) {
            return -1;
        } else {
            return mAudioItems.indexOf(mCurrentSong);
        }
    }

    public AudioItem getCurrentSong() {
        return mCurrentSong;
    }

    public void setCurrentSong(AudioItem currentSong) {
        mCurrentSong = currentSong;
    }
}
