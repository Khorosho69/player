package com.antont.player.events;

public class OnTrackStartedEvent {
    private int mTrackIndex;
    private int mTrackDuration;

    public OnTrackStartedEvent(int trackIndex, int trackDuration) {
        mTrackIndex = trackIndex;
        mTrackDuration = trackDuration;
    }

    public int getTrackIndex() {
        return mTrackIndex;
    }

    public int getTrackDuration() {
        return mTrackDuration;
    }
}
