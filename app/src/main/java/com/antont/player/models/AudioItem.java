package com.antont.player.models;

public class AudioItem {


    private String mPath;
    private String mName;
    private String mAlbumName;

    public AudioItem(String path, String name, String albumName) {
        this.mPath = path;
        mName = name;
        mAlbumName = albumName;
    }

    public String getName() {
        return mName;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public String getPath() {
        return mPath;
    }
}
