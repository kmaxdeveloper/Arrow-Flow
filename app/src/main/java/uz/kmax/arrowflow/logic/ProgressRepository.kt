package uz.kmax.arrowflow.logic

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.TimeZone

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "progress")

class ProgressRepository(private val context: Context) {

    companion object {
        private val HIGHEST_UNLOCKED_LEVEL = intPreferencesKey("highest_unlocked_level")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val HINTS_LEFT = intPreferencesKey("hints_left")
        private val TUTORIAL_DONE = booleanPreferencesKey("tutorial_done")
        private val TOTAL_STARS = intPreferencesKey("total_stars_cache")
        private val LEVELS_COMPLETED = intPreferencesKey("levels_completed")
        private val BEST_STREAK = intPreferencesKey("best_streak")
        private val CURRENT_STREAK = intPreferencesKey("current_streak")
        private val LAST_PLAY_DATE = intPreferencesKey("last_play_date")
        private val DAILY_PUZZLE_LAST_COMPLETED = intPreferencesKey("daily_puzzle_last_completed")
        private val HINT_USED_TOTAL = intPreferencesKey("hint_used_total")
        private val UNDO_USED_TOTAL = intPreferencesKey("undo_used_total")

        const val DAILY_GOAL = 3
        const val DEFAULT_HINTS = 5

        fun getStarKey(levelId: Int) = intPreferencesKey("level_${levelId}_stars")
    }

    val highestUnlockedLevel: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HIGHEST_UNLOCKED_LEVEL] ?: 1
    }

    val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_ENABLED] ?: true
    }

    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_ENABLED] ?: true
    }

    fun getBestStars(levelId: Int): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[getStarKey(levelId)] ?: 0
    }

    val hintsLeft: Flow<Int> = context.dataStore.data.map { it[HINTS_LEFT] ?: DEFAULT_HINTS }
    val isTutorialDone: Flow<Boolean> = context.dataStore.data.map { it[TUTORIAL_DONE] ?: false }
    val levelsCompleted: Flow<Int> = context.dataStore.data.map { it[LEVELS_COMPLETED] ?: 0 }
    val currentStreak: Flow<Int> = context.dataStore.data.map { it[CURRENT_STREAK] ?: 0 }
    val bestStreak: Flow<Int> = context.dataStore.data.map { it[BEST_STREAK] ?: 0 }

    val isDailyPuzzleCompletedToday: Flow<Boolean> = context.dataStore.data.map { 
        it[DAILY_PUZZLE_LAST_COMPLETED] == todayKey()
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun saveProgress(levelId: Int, stars: Int) {
        context.dataStore.edit { preferences ->
            // Save stars if better
            val currentStars = preferences[getStarKey(levelId)] ?: 0
            if (stars > currentStars) {
                preferences[getStarKey(levelId)] = stars
            }

            // Unlock next level if this was the highest completed
            val highest = preferences[HIGHEST_UNLOCKED_LEVEL] ?: 1
            if (levelId >= highest) {
                preferences[HIGHEST_UNLOCKED_LEVEL] = levelId + 1
            }
        }
        // Statistika: streak + completed (alohida yozamiz - edit ichida date ishlatish xavfsiz)
        updateStreakAndStats()
    }

    private suspend fun updateStreakAndStats() {
        context.dataStore.edit { preferences ->
            val today = todayKey()
            val last = preferences[LAST_PLAY_DATE] ?: -1
            val cur = preferences[CURRENT_STREAK] ?: 0
            val best = preferences[BEST_STREAK] ?: 0
            val newStreak = when {
                last == today -> cur
                last == today - 1 -> cur + 1
                else -> 1
            }
            preferences[LAST_PLAY_DATE] = today
            preferences[CURRENT_STREAK] = newStreak
            if (newStreak > best) preferences[BEST_STREAK] = newStreak
            preferences[LEVELS_COMPLETED] = (preferences[LEVELS_COMPLETED] ?: 0) + 1
        }
    }

    private fun todayKey(): Int {
        val ms = System.currentTimeMillis()
        val tzOffset = TimeZone.getDefault().getOffset(ms)
        return ((ms + tzOffset) / (24 * 60 * 60 * 1000L)).toInt()
    }

    suspend fun setTutorialDone() {
        context.dataStore.edit { it[TUTORIAL_DONE] = true }
    }

    suspend fun useHint(): Boolean {
        var ok = false
        context.dataStore.edit {
            val left = it[HINTS_LEFT] ?: DEFAULT_HINTS
            if (left > 0) {
                it[HINTS_LEFT] = left - 1
                it[HINT_USED_TOTAL] = (it[HINT_USED_TOTAL] ?: 0) + 1
                ok = true
            }
        }
        return ok
    }

    suspend fun addHints(count: Int) {
        context.dataStore.edit { it[HINTS_LEFT] = (it[HINTS_LEFT] ?: DEFAULT_HINTS) + count }
    }

    suspend fun recordUndo() {
        context.dataStore.edit { it[UNDO_USED_TOTAL] = (it[UNDO_USED_TOTAL] ?: 0) + 1 }
    }

    suspend fun setDailyPuzzleCompleted() {
        context.dataStore.edit { it[DAILY_PUZZLE_LAST_COMPLETED] = todayKey() }
    }

    suspend fun getTotalStars(maxLevel: Int): Int {
        val prefs: androidx.datastore.preferences.core.Preferences = context.dataStore.data.first()
        var sum = 0
        for (id in 1..maxLevel) sum += prefs[getStarKey(id)] ?: 0
        return sum
    }
}
