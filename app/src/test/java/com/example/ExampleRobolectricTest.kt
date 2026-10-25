package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdManager
import com.example.data.PreferencesManager
import com.example.model.GameDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FunVerse", appName)
    }

    @Test
    fun `test preferences manager offline score saving`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        val isNewRecord = prefs.recordScore(GameDifficulty.EASY, 350)
        assertTrue(isNewRecord)
        assertEquals(350, prefs.getBestScore(GameDifficulty.EASY))
    }

    @Test
    fun `test level progression and stars persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        // Initial unlocked level is 1
        assertEquals(1, prefs.highestUnlockedLevel)

        // Complete level 1 with 3 stars
        prefs.saveLevelCompleted(1, 3)
        assertEquals(2, prefs.highestUnlockedLevel)
        assertEquals(3, prefs.getLevelStars(1))

        // Complete level 2 with 2 stars
        prefs.saveLevelCompleted(2, 2)
        assertEquals(3, prefs.highestUnlockedLevel)
        assertEquals(2, prefs.getLevelStars(2))

        // Total stars should be 5
        assertEquals(5, prefs.getTotalStars())
    }

    @Test
    fun `test ad manager first 3 levels ad-free and pacing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)
        prefs.levelsClearedSinceLastAd = 0
        val adManager = AdManager(prefs)

        // Levels 1, 2, 3 must be strictly ad-free
        assertFalse(adManager.shouldShowAdAfterLevel(1))
        assertFalse(adManager.shouldShowAdAfterLevel(2))
        assertFalse(adManager.shouldShowAdAfterLevel(3))

        // Level 4 (count = 1): false
        assertFalse(adManager.shouldShowAdAfterLevel(4))
        // Level 5 (count = 2): false
        assertFalse(adManager.shouldShowAdAfterLevel(5))
        // Level 6 (count = 3): should show ad!
        assertTrue(adManager.shouldShowAdAfterLevel(6))

        // Counter reset: Level 7 (count = 1): false
        assertFalse(adManager.shouldShowAdAfterLevel(7))
    }
}
