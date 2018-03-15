package com.antont.player.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.antont.player.AudioItemsContainer;
import com.antont.player.R;
import com.antont.player.enums.ActionType;
import com.antont.player.events.OnServiceDestroyEvent;
import com.antont.player.events.OnTrackStartedEvent;
import com.antont.player.models.AudioItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    public static final String NOTIFICATION_CHANEL_ID = "AUDIO_PLAYER_CHANEL";
    private final static String LOG_TAG = "Audio player";

    private static final int NOTIFICATION_ID = 101;
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayer();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIncomingActions(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) {
            return;
        }

        ActionType action = ActionType.valueOf(playbackAction.getAction());
        makeAction(action);
    }

    private void makeAction(ActionType action) {
        switch (action) {
            case ACTION_PAUSE:
                playPauseTrack();
                break;
            case ACTION_PLAY:
                playPauseTrack();
                break;
            case ACTION_NEXT:
                playNextTrack();
                break;
            case ACTION_PREVIOUS:
                playPreviousTrack();
                break;
            case ACTION_STOP:
                stop();
                break;
        }
    }

    public void initPlayer() {
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        // Set MediaPlayer listeners
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
    }

    public void playPauseTrack() {
        if (AudioItemsContainer.getInstance().getCurrentSong() == null) {
            AudioItemsContainer.getInstance().skipToNextSong();
            playTrack();
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            buildNotification(ActionType.ACTION_PAUSE);
        } else {
            mMediaPlayer.start();
            buildNotification(ActionType.ACTION_PLAY);
        }

        EventBus.getDefault().post(mMediaPlayer.isPlaying());
    }

    public void playNextTrack() {
        AudioItemsContainer.getInstance().skipToNextSong();
        playTrack();
        buildNotification(ActionType.ACTION_PLAY);
    }

    public void playPreviousTrack() {
        AudioItemsContainer.getInstance().skipToPreviousSong();
        playTrack();
        buildNotification(ActionType.ACTION_PLAY);
    }

    private void stop() {
//        removeNotification();
        mMediaPlayer.stop();
        //Stop the service
        stopForeground(true);
        stopSelf();
    }

    // Called when a user clicks one of the multimedia buttons
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void actionFromActivity(ActionType actionBody) {
        makeAction(actionBody);

    }

    // Called from MainActivity when the user changes the progress of the song playback
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setTrackProgress(Integer newTrackProgress) {
        mMediaPlayer.seekTo(newTrackProgress);
    }

    // Called when a user chooses another track to play
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTrackSelected(AudioItem eventBody) {
        if (AudioItemsContainer.getInstance().getCurrentSong() == eventBody) {
            playPauseTrack();
        } else {
            AudioItemsContainer.getInstance().setCurrentSong(eventBody);
            playTrack();
        }
    }

    private void playTrack() {
        mMediaPlayer.reset();
        AudioItem item = AudioItemsContainer.getInstance().getCurrentSong();
        Uri trackUri = Uri.parse(item.getPath());
        try {
            mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error setting data source", e);
        }
        mMediaPlayer.prepareAsync();

        buildNotification(ActionType.ACTION_PLAY);
    }

    private void buildNotification(ActionType playbackStatus) {
        AudioItem song = AudioItemsContainer.getInstance().getCurrentSong();

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);

        remoteViews.setTextViewText(R.id.track_name, song.getName());
        remoteViews.setTextViewText(R.id.album_name, song.getAlbumName());

        int icon = (playbackStatus == ActionType.ACTION_PLAY) ? R.drawable.ic_pause : R.drawable.ic_play;

        remoteViews.setInt(R.id.play_button, "setBackgroundResource", icon);

        remoteViews.setOnClickPendingIntent(R.id.previous_button, generatePendingIntent(ActionType.ACTION_PREVIOUS.name()));
        remoteViews.setOnClickPendingIntent(R.id.play_button, generatePendingIntent(playbackStatus.name()));
        remoteViews.setOnClickPendingIntent(R.id.next_button, generatePendingIntent(ActionType.ACTION_NEXT.name()));
        remoteViews.setOnClickPendingIntent(R.id.close_button, generatePendingIntent(ActionType.ACTION_STOP.name()));

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_audiotrack);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder
                    .setChannelId(NOTIFICATION_CHANEL_ID)
                    .setCustomContentView(remoteViews);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANEL_ID,
                    getString(R.string.notification_chanel_title), NotificationManager.IMPORTANCE_DEFAULT);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
                mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
            }
        } else {
            notificationBuilder.setContent(remoteViews);
        }
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private PendingIntent generatePendingIntent(String intentAction) {
        Intent intent = new Intent(getApplicationContext(), AudioPlayerService.class);
        intent.setAction(intentAction);
        return PendingIntent.getService(this, 1, intent, 0);
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        int position = AudioItemsContainer.getInstance().getCurrentSongIndex();
        int duration = mMediaPlayer.getDuration();

        // Send event to MainActivity about what the song started to play
        EventBus.getDefault().post(new OnTrackStartedEvent(position, duration));
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        playNextTrack();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.d(LOG_TAG, "Playback error.");
        mediaPlayer.reset();
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().post(new OnServiceDestroyEvent());
        EventBus.getDefault().unregister(this);
    }
}
