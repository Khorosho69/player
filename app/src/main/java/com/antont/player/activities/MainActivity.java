package com.antont.player.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.antont.player.AudioItemsContainer;
import com.antont.player.events.OnServiceDestroyEvent;
import com.antont.player.events.OnTrackStartedEvent;
import com.antont.player.R;
import com.antont.player.adapters.RecyclerViewAdapter;
import com.antont.player.enums.ActionType;
import com.antont.player.models.AudioItem;
import com.antont.player.services.AudioPlayerService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        RecyclerViewAdapter.OnItemSelectedCallback {

    private static String ARG_TRACK_DURATION = "ARG_TRACK_DURATION";
    private static String ARG_SEEK_BAR_POSITION = "ARG_SEEK_BAR_POSITION";
    private static String ARG_TRACK_NAME = "ARG_TRACK_NAME";
    private static String ARG_IS_PLAYING = "ARG_IS_PLAYING";

    private static final int PERMISSION_REQUEST_CODE = 1024;
    private static final int TIMER_PERIOD_IN_MS = 50; // Amount of milliseconds in which the progressBar is updated

    private RecyclerView mRecyclerView;
    private TextView mTrackNameTextView;
    private ImageButton mPlayPauseButton;

    private SeekBar mSeekBar;
    private Timer mTimer = new Timer();

    private int mTrackDuration = 0;
    private Boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPlayPauseButton = findViewById(R.id.play_pause_button);
        mTrackNameTextView = findViewById(R.id.track_name_text_view);

        setupSeekBar();

        EventBus.getDefault().register(this);

        if (AudioItemsContainer.getInstance().getAudioItems().isEmpty()) {
            checkAndroidPermission();
        } else {
            setupRecyclerView();
        }

        restoreStateFromSavedInstance(savedInstanceState);

        setupService();
    }

    private void restoreStateFromSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mTrackDuration = savedInstanceState.getInt(ARG_TRACK_DURATION);
            int restoredProgress = savedInstanceState.getInt(ARG_SEEK_BAR_POSITION);

            mSeekBar.setMax(mTrackDuration);
            mSeekBar.setProgress(restoredProgress);
            isPlaying = savedInstanceState.getBoolean(ARG_IS_PLAYING);
            mTrackNameTextView.setText(savedInstanceState.getString(ARG_TRACK_NAME));
            onChangeMusicState(isPlaying);
        }
    }

    private void setupService() {
        Intent serviceIntent = new Intent(this, AudioPlayerService.class);
        startService(serviceIntent);
    }

    private void setupSeekBar() {
        mSeekBar = findViewById(R.id.seekBar);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!AudioItemsContainer.getInstance().getAudioItems().isEmpty()) {
                    EventBus.getDefault().post(seekBar.getProgress());
                }
            }
        });
    }

    private void setupSeekBarUpdater(int trackDuration) {
        mSeekBar.setMax(trackDuration);
        mTimer = new Timer();
        MyTimerTask updateSeekBar = new MyTimerTask();
        mTimer.schedule(updateSeekBar, 0, TIMER_PERIOD_IN_MS);
    }

    public void setupRecyclerView() {
        mRecyclerView = findViewById(R.id.audio_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        int songIndex = AudioItemsContainer.getInstance().getCurrentSongIndex();
        List<AudioItem> items = AudioItemsContainer.getInstance().getAudioItems();
        RecyclerView.Adapter adapter = new RecyclerViewAdapter(items, songIndex, this);
        mRecyclerView.setAdapter(adapter);
    }

    // Called from AudioPlayerService when the song starts playing or stops
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangeMusicState(Boolean playing) {
        isPlaying = playing;
        int imageId = playing ? R.drawable.ic_play : R.drawable.ic_pause;
        mPlayPauseButton.setImageResource(imageId);
        if (isPlaying) {
            setupSeekBarUpdater(mTrackDuration);
        } else {
            cancelTimer();
        }
        ((RecyclerViewAdapter) mRecyclerView.getAdapter()).updatePlayingStatus(playing);
    }

    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    // Called from AudioPlayerService when the next song starts play
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartPlayingSong(OnTrackStartedEvent actionBody) {
        mTimer.cancel();

        mTrackDuration = actionBody.getTrackDuration();
        mSeekBar.setProgress(0);
        setupSeekBarUpdater(mTrackDuration);

        ((RecyclerViewAdapter) mRecyclerView.getAdapter()).changeCurrentSong(actionBody.getTrackIndex());
        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
        mPlayPauseButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServiceDestroy(OnServiceDestroyEvent eventBody) {
        setupService();
        mTimer.cancel();
        mTrackNameTextView.setText("");
        ((RecyclerViewAdapter) mRecyclerView.getAdapter()).changeCurrentSong(-1);
        AudioItemsContainer.getInstance().setCurrentSong(null);
        mSeekBar.setProgress(0);
    }

    // Send an event that contains the type of click on the AudioPlayerService
    public void onMediaButtonPressed(View view) {
        if (!AudioItemsContainer.getInstance().getAudioItems().isEmpty()) {
            switch (view.getId()) {
                case R.id.play_pause_button:
                    EventBus.getDefault().post(ActionType.ACTION_PLAY);
                    break;
                case R.id.previous_button:
                    EventBus.getDefault().post(ActionType.ACTION_PREVIOUS);
                    break;
                case R.id.next_button:
                    EventBus.getDefault().post(ActionType.ACTION_NEXT);
                    break;
            }
//            mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
        } else {
            Toast.makeText(this, R.string.no_audio_tracks_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndroidPermission() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            if (isPermissionGranted()) {
//                AudioItemsContainer.getInstance().setAudioItems(getAudioItemList());
//                setupRecyclerView();
//            } else {
//                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
//            }
//        } else {
//            AudioItemsContainer.getInstance().setAudioItems(getAudioItemList());
//            setupRecyclerView();
//        }

        if (Build.VERSION.SDK_INT >= 23 && !isPermissionGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            AudioItemsContainer.getInstance().setAudioItems(getAudioItemList());
            setupRecyclerView();
        }
    }

    private boolean isPermissionGranted() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
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

    private void showSnackBar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.permission_denied_message, Snackbar.LENGTH_LONG)
                .setAction(android.R.string.ok, view -> {
                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (showRationale) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    } else {
                        AudioItemsContainer.getInstance().setAudioItems(getAudioItemList());
                        setupRecyclerView();
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE || grantResults.length == 0) {
            return;
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            AudioItemsContainer.getInstance().setAudioItems(getAudioItemList());
            setupRecyclerView();
        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            showSnackBar();
        }
    }

    @Override
    public void onItemSelected(AudioItem audioItem) {
        EventBus.getDefault().post(audioItem);
//        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ARG_TRACK_DURATION, mTrackDuration);
        outState.putInt(ARG_SEEK_BAR_POSITION, mSeekBar.getProgress());
        outState.putString(ARG_TRACK_NAME, mTrackNameTextView.getText().toString());
        outState.putBoolean(ARG_IS_PLAYING, isPlaying);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        cancelTimer();
    }

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> mSeekBar.setProgress(mSeekBar.getProgress() + TIMER_PERIOD_IN_MS));
        }
    }
}
