package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

public class NeoTest {

    @Test
    public void pickedWallpaperStaysWithoutDailyChange() {
        assertEquals(1, Neo.index(3, 1, false, 0));
        assertEquals(1, Neo.index(3, 1, false, 40));
    }

    @Test
    public void dailyChangeStartsOnThePickedOneAndMovesOnEachDay() {
        assertEquals(1, Neo.index(3, 1, true, 0));
        assertEquals(2, Neo.index(3, 1, true, 1));
        assertEquals(0, Neo.index(3, 1, true, 2));
        assertEquals(1, Neo.index(3, 1, true, 3));
    }

    @Test
    public void unknownOrStalePickFallsBackInsideTheCollection() {
        assertEquals(0, Neo.index(3, -1, false, 0));   // nothing picked yet: the first
        assertEquals(2, Neo.index(3, 7, false, 0));    // a collection that shrank: its last
        assertEquals(0, Neo.index(3, 0, true, -5));    // a clock set back: no negative days
        assertEquals(0, Neo.index(0, 2, true, 9));     // empty collection never throws
    }

    @Test
    public void screensaverDateIsCapitalised() {
        Locale ru = new Locale("ru");
        assertEquals("Вторник, 27 октября", ClockScene.capitalized("вторник, 27 октября", ru));
        assertEquals("", ClockScene.capitalized("", ru));
    }
}
