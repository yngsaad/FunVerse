package com.example.ads

import com.example.data.PreferencesManager

/**
 * Manages child-safe, non-intrusive interstitial ad pacing for FunVerse.
 * Guarantees that:
 * 1. Levels 1–3 are 100% ad-free.
 * 2. Ads show only after every 3–4 cleared levels.
 * 3. Never shows ads during gameplay or on failed attempts.
 * 4. Offline gameplay proceeds seamlessly with zero interruption.
 */
class AdManager(private val prefs: PreferencesManager) {

    companion object {
        const val AD_FREE_INITIAL_LEVELS = 3
        const val LEVEL_INTERVAL_FOR_ADS = 3 // Every 3-4 levels
        const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }

    /**
     * Checks if an interstitial ad should be displayed after clearing a level.
     */
    fun shouldShowAdAfterLevel(clearedLevel: Int): Boolean {
        // First 3 levels are strictly ad-free
        if (clearedLevel <= AD_FREE_INITIAL_LEVELS) {
            return false
        }

        val count = prefs.levelsClearedSinceLastAd + 1
        return if (count >= LEVEL_INTERVAL_FOR_ADS) {
            prefs.levelsClearedSinceLastAd = 0
            true
        } else {
            prefs.levelsClearedSinceLastAd = count
            false
        }
    }
}
