package com.strangequark.musicplayer;

import java.io.File;
import java.util.Locale;

public class Song {
    public final long mediaStoreId;
    public final String title;
    public final String artist;
    public final String album;
    public final String path;
    public final int durationMs;
    public final int trackNumber;

    public Song(long mediaStoreId, String title, String artist, String album, String path, int durationMs, int trackNumber) {
        this.mediaStoreId = mediaStoreId;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.durationMs = durationMs;
        this.trackNumber = trackNumber;
    }

    public File getFile() {
        return new File(path);
    }

    public String getStableKey() {
        if (mediaStoreId >= 0) {
            return "id:" + mediaStoreId;
        }
        return "path:" + path;
    }

    public String getArtistDurationText() {
        int minutes = (durationMs / (1000 * 60)) % 60;
        int seconds = (durationMs / 1000) % 60;
        return artist + " \u00B7 " + minutes + ":" + String.format(Locale.US, "%02d", seconds);
    }
}
