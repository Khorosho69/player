package com.antont.player.services;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import com.antont.player.activities.MainActivity;
import com.antont.player.models.AudioItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class AudioPlayer extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private final static String LOG_TAG = "Audio player";

    private MediaPlayer mMediaPlayer = new MediaPlayer();
    private AudioItem mAudioItem;

    private List<AudioItem> mAudioItems = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayer();
        EventBus.getDefault().register(this);

        mAudioItems = getAudioItemList();
        EventBus.getDefault().post(mAudioItems);
    }

    public void initPlayer() {
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // Set MediaPlayer listeners
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onItemSelected(AudioItem event) {
        mAudioItem = event;
        playSong(mAudioItem);
    }

    public void playSong(AudioItem item) {
        mMediaPlayer.reset();
        Uri trackUri = Uri.parse(item.getPath());
        try {
            mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error setting data source", e);
        }
        mMediaPlayer.prepareAsync();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNextSong();
    }

    private void playNextSong() {
        if (mAudioItems.contains(mAudioItem)) {
            if (mAudioItems.indexOf(mAudioItem) != mAudioItems.size()) {
                mAudioItem = mAudioItems.get(mAudioItems.indexOf(mAudioItem) + 1);
            } else {
                mAudioItem = mAudioItems.get(0);
            }
            playSong(mAudioItem);
        }
    }

    private List<AudioItem> getAudioItemList() {
        List<AudioItem> items = new ArrayList<>();

        String[] params = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ALBUM};
        Cursor audioCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, params, null, null, null);

        if (audioCursor == null) {
            return items;
        }
        if (audioCursor.moveToFirst()) {
            do {
                String path = audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                String name = audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                String album = audioCursor.getString(audioCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));

                items.add(new AudioItem(path, name, album));
            } while (audioCursor.moveToNext());
        }
        audioCursor.close();

        return items;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(LOG_TAG, "Playback error.");
        mp.reset();

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
