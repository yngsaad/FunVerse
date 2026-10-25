package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.GameDifficulty

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var isHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, value).apply()

    var selectedDifficulty: GameDifficulty
        get() {
            val name = prefs.getString(KEY_SELECTED_DIFFICULTY, GameDifficulty.EASY.name)
            return try {
                GameDifficulty.valueOf(name ?: GameDifficulty.EASY.name)
            } catch (e: Exception) {
                GameDifficulty.EASY
            }
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_DIFFICULTY, value.name).apply()

    var highestUnlockedLevel: Int
        get() = prefs.getInt(KEY_HIGHEST_UNLOCKED_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_HIGHEST_UNLOCKED_LEVEL, value.coerceAtLeast(1)).apply()

    var currentSelectedJourneyLevel: Int
        get() = prefs.getInt(KEY_CURRENT_JOURNEY_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_JOURNEY_LEVEL, value.coerceAtLeast(1)).apply()

    var levelsClearedSinceLastAd: Int
        get() = prefs.getInt(KEY_LEVELS_SINCE_AD, 0)
        set(value) = prefs.edit().putInt(KEY_LEVELS_SINCE_AD, value).apply()

    fun getBestScore(difficulty: GameDifficulty): Int {
        return prefs.getInt(KEY_BEST_SCORE_PREFIX + difficulty.name, 0)
    }

    fun recordScore(difficulty: GameDifficulty, score: Int): Boolean {
        val currentBest = getBestScore(difficulty)
        return if (score > currentBest) {
            prefs.edit().putInt(KEY_BEST_SCORE_PREFIX + difficulty.name, score).apply()
            true
        } else {
            false
        }
    }

    fun getLevelStars(level: Int): Int {
        return prefs.getInt(KEY_LEVEL_STARS_PREFIX + level, 0)
    }

    fun saveLevelCompleted(level: Int, stars: Int) {
        val prevStars = getLevelStars(level)
        if (stars > prevStars) {
            prefs.edit().putInt(KEY_LEVEL_STARS_PREFIX + level, stars).apply()
        }
        if (level >= highestUnlockedLevel) {
            highestUnlockedLevel = level + 1
        }
    }

    fun getTotalStars(): Int {
        var total = 0
        val maxLevel = highestUnlockedLevel
        for (lvl in 1..maxLevel) {
            total += getLevelStars(lvl)
        }
        return total
    }

    fun getGamesPlayed(): Int {
        return prefs.getInt(KEY_GAMES_PLAYED, 0)
    }

    fun incrementGamesPlayed() {
        prefs.edit().putInt(KEY_GAMES_PLAYED, getGamesPlayed() + 1).apply()
    }

    companion object {
        private const val PREFS_NAME = "funverse_color_match_prefs"
        private const val KEY_SOUND_ENABLED = "key_sound_enabled"
        private const val KEY_HAPTICS_ENABLED = "key_haptics_enabled"
        private const val KEY_SELECTED_DIFFICULTY = "key_selected_difficulty"
        private const val KEY_BEST_SCORE_PREFIX = "key_best_score_"
        private const val KEY_GAMES_PLAYED = "key_games_played"
        private const val KEY_HIGHEST_UNLOCKED_LEVEL = "key_highest_unlocked_level"
        private const val KEY_CURRENT_JOURNEY_LEVEL = "key_current_journey_level"
        private const val KEY_LEVEL_STARS_PREFIX = "key_level_stars_"
        private const val KEY_LEVELS_SINCE_AD = "key_levels_since_ad"
    }
}
