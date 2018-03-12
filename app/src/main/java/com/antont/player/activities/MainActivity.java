package com.antont.player.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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

import com.antont.player.AudioItemsContainer;
import com.antont.player.R;
import com.antont.player.adapters.RecyclerViewAdapter;
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

    private static final int PERMISSION_REQUEST_CODE = 1024;
    private RecyclerView mRecyclerView;

    private boolean isServiceBound = false;
    private AudioPlayerService mPlayerService;
    private ServiceConnection mServiceConnection;
    private Intent mServiceIntent;

    private TextView mTrackNameTextView;
    private ImageButton mImageButton;
    private SeekBar mSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageButton = findViewById(R.id.play_pause_button);
        mTrackNameTextView = findViewById(R.id.track_name_text_view);

        setupSeekBar();

        EventBus.getDefault().register(this);

        if (AudioItemsContainer.getInstance().getAudioItems().isEmpty()) {
            checkAndroidPermission();
        } else {
            setupRecyclerView();
        }
        setupService();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(mServiceIntent, mServiceConnection, 0);
    }

    private void setupService() {
        mServiceIntent = new Intent(this, AudioPlayerService.class);
        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                mPlayerService = ((AudioPlayerService.ServiceBinder) binder).getService();
                isServiceBound = true;
            }

            public void onServiceDisconnected(ComponentName name) {
                isServiceBound = false;
            }
        };
        startService(mServiceIntent);
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
                mPlayerService.setTrackProgress(seekBar.getProgress());
            }
        });
    }

    private void setupSeekBarUpdater() {
        mSeekBar.setMax(mPlayerService.getCurrentTrackDuration());
        Timer timer = new Timer();
        MyTimerTask updateSeekBar = new MyTimerTask();
        timer.schedule(updateSeekBar, 0, 50);
    }

    // Called from AudioPlayerService when the song starts playing or stops
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackPause(Boolean playing) {
        if (playing) {
            mImageButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
        } else {
            mImageButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
        }
        ((RecyclerViewAdapter) mRecyclerView.getAdapter()).updatePlayingStatus(playing);
    }

    // Called from AudioPlayerService when the next song starts
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSongPlaying(Integer songIndex) {
        setupSeekBarUpdater();
        ((RecyclerViewAdapter) mRecyclerView.getAdapter()).changeCurrentSong(songIndex);
        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
        mImageButton.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
    }

    public void setupRecyclerView() {
        mRecyclerView = findViewById(R.id.audio_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        int songIndex = AudioItemsContainer.getInstance().getCurrentSongIndex();
        RecyclerView.Adapter adapter = new RecyclerViewAdapter(AudioItemsContainer.getInstance().getAudioItems(),
                songIndex, this);
        mRecyclerView.setAdapter(adapter);
    }

    public void onStop_StartPlaying(View view) {
        mPlayerService.play_pauseTrack();
        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
    }

    public void onPlayPreviousTrack(View view) {
        mPlayerService.playPreviousTrack();
        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
    }

    public void onPlayNextTrack(View view) {
        mPlayerService.playNextTrack();
        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
    }


    private void checkAndroidPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (isPermissionGranted()) {
                AudioItemsContainer.getInstance().setAudioItems(getAudioItemList());
                setupRecyclerView();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
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
            setupRecyclerView();
        } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            showSnackBar();
        }
    }

    @Override
    public void onItemSelected(AudioItem audioItem) {
        mPlayerService.onItemSelected(audioItem);
        mTrackNameTextView.setText(AudioItemsContainer.getInstance().getCurrentSong().getName());
    }

    @Override
    protected void onStop() {
        super.onStop();
        isServiceBound = false;
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(() -> mSeekBar.setProgress(mPlayerService.getCurrentPosition()));
        }
    }
}
