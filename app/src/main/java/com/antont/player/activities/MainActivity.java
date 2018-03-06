package com.antont.player.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.antont.player.R;
import com.antont.player.adapters.RecyclerViewAdapter;
import com.antont.player.models.AudioItem;
import com.antont.player.services.AudioPlayer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String ARG_AUDIO_LIST =  "ARG_AUDIO_LIST";
    private static final int PERMISSION_REQUEST_CODE = 1024;

    private List<AudioItem> mAudioItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventBus.getDefault().register(this);

        startService(new Intent(this, AudioPlayer.class));

        checkAndroidPermission();
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

    private void checkAndroidPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!isPermissionGranted()) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Please allow permission in App Settings.", Toast.LENGTH_LONG).show();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }
            }
        }
    }

    private boolean isPermissionGranted() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void setupRecyclerView(List<AudioItem> items) {
        RecyclerView recyclerView = findViewById(R.id.audio_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        RecyclerView.Adapter adapter = new RecyclerViewAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == PERMISSION_REQUEST_CODE) {



        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
