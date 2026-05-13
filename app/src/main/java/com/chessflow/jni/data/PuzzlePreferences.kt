package com.chessflow.jni.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore by preferencesDataStore("puzzle_prefs")

@Singleton
class PuzzlePreferences @Inject constructor(
    @ApplicationContext private val context: Context
){

companion object {
        private val LAST_DIFFICULTY = stringPreferencesKey("last_difficulty")
        private const val DEFAULT_DIFFICULTY = "easy"
    }

    val lastDifficulty: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LAST_DIFFICULTY] ?: DEFAULT_DIFFICULTY
    }

    suspend fun saveDifficulty( difficulty: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_DIFFICULTY] = difficulty
        }
    }
}