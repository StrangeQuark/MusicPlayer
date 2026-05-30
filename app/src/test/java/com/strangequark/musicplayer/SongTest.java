package com.strangequark.musicplayer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SongTest {
    @Test
    public void stableKeyPrefersMediaStoreId() {
        Song song = new Song(123L, "Title", "Artist", "Album", "/music/song.mp3", 185000, 1);

        assertEquals("id:123", song.getStableKey());
    }

    @Test
    public void stableKeyFallsBackToPath() {
        Song song = new Song(-1L, "Title", "Artist", "Album", "/music/song.mp3", 185000, 1);

        assertEquals("path:/music/song.mp3", song.getStableKey());
    }

    @Test
    public void artistDurationTextFormatsSecondsWithLeadingZero() {
        Song song = new Song(1L, "Title", "Artist", "Album", "/music/song.mp3", 185000, 1);

        assertEquals("Artist \u00B7 3:05", song.getArtistDurationText());
    }
}
