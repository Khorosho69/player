package com.antont.player.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.antont.player.AudioItemsContainer;
import com.antont.player.R;
import com.antont.player.models.AudioItem;

import org.greenrobot.eventbus.EventBus;

import static android.support.v4.media.session.MediaControllerCompat.TransportControls;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String MEDIA_SESSION_COMPAT_TAG = "AudioPlayerService";
    private final static String LOG_TAG = "Audio player";

    private MediaSessionManager mMediaSessionManager;
    private MediaSessionCompat mMediaSessionCompat;
    private TransportControls mTransportControls;

    //AudioPlayerService notification ID
    private static final int NOTIFICATION_ID = 101;

    private MediaPlayer mMediaPlayer = new MediaPlayer();

    ServiceBinder mServiceBinder = new ServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceBinder;
    }

    public class ServiceBinder extends Binder {
        public AudioPlayerService getService() {
            return AudioPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayer();
        try {
            initMediaSession();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            mTransportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            mTransportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            mTransportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mTransportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            mTransportControls.stop();
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

    private void initMediaSession() throws RemoteException {
        if (mMediaSessionManager != null) {
            return;
        }
        mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mMediaSessionCompat = new MediaSessionCompat(getApplicationContext(), MEDIA_SESSION_COMPAT_TAG);
        mTransportControls = mMediaSessionCompat.getController().getTransportControls();
        mMediaSessionCompat.setActive(true);
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Attach Callback to receive MediaSession updates
        mMediaSessionCompat.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                play_pauseTrack();
            }

            @Override
            public void onPause() {
                super.onPause();
                play_pauseTrack();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                playNextTrack();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                playPreviousTrack();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    // Called when a user chooses another track to play
    public void onItemSelected(AudioItem event) {
        if (AudioItemsContainer.getInstance().getCurrentSong() == event) {
            play_pauseTrack();
        } else {
            AudioItemsContainer.getInstance().setCurrentSong(event);
            playTrack();
        }
    }

    public void playNextTrack() {
        AudioItemsContainer.getInstance().getNextSong();
        playTrack();
        buildNotification(PlaybackStatus.PLAYING);
    }

    public void playPreviousTrack() {
        AudioItemsContainer.getInstance().getPreviousSong();
        playTrack();
        buildNotification(PlaybackStatus.PLAYING);
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

        buildNotification(PlaybackStatus.PLAYING);
    }

    public void play_pauseTrack() {
        if (AudioItemsContainer.getInstance().getCurrentSong() == null) {
            AudioItemsContainer.getInstance().getNextSong();
            playTrack();
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            buildNotification(PlaybackStatus.PAUSED);
        } else {
            mMediaPlayer.start();
            buildNotification(PlaybackStatus.PLAYING);
        }

        EventBus.getDefault().post(mMediaPlayer.isPlaying());
    }

    // Called from MainActivity when the user changes the progress of the song playback
    public void setTrackProgress(int newTrackProgress) {
        mMediaPlayer.seekTo(newTrackProgress);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        // Send event to MainActivity about what the song started to play
        EventBus.getDefault().post(AudioItemsContainer.getInstance().getCurrentSongIndex());
        mediaPlayer.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        playNextTrack();
    }

    private void buildNotification(PlaybackStatus playbackStatus) {
        AudioItem song = AudioItemsContainer.getInstance().getCurrentSong();

        PendingIntent play_pauseAction = playbackAction(playbackStatus.ordinal());
        int notificationActionDrawable = (playbackStatus == PlaybackStatus.PLAYING)
                ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setShowWhen(false)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mMediaSessionCompat.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentText(song.getAlbumName())
                .setContentTitle(song.getName())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, getString(R.string.notification_title_previous), playbackAction(3))
                .addAction(notificationActionDrawable, getString(R.string.notification_title_pause), play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, getString(R.string.notification_title_next), playbackAction(2));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, AudioPlayerService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    public int getCurrentTrackDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.d(LOG_TAG, "Playback error.");
        mediaPlayer.reset();
        return false;
    }

    private enum PlaybackStatus {
        PAUSED,
        PLAYING
    }
}
