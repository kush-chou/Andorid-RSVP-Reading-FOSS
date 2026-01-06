package com.example.fossrsvp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PersistenceManager {
    private const val PREFS_NAME = "FossRsvpData"
    private const val KEY_SETTINGS = "app_settings"
    private const val KEY_LIBRARY = "library_books"
    private const val KEY_CHAT_HISTORY = "chat_history"
    private const val KEY_STATISTICS = "reading_sessions"

    private val gson = Gson()

    // --- Settings Persistence ---
    fun saveSettings(context: Context, settings: AppSettings) {
        val json = gson.toJson(settings)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SETTINGS, json)
            .apply()
    }

    fun loadSettings(context: Context): AppSettings {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SETTINGS, null)
        
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java)
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    // --- Library Persistence ---
    fun saveLibrary(context: Context, books: List<Book>) {
        val json = gson.toJson(books)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIBRARY, json)
            .apply()
    }

    fun loadLibrary(context: Context): List<Book> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LIBRARY, null)
        
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Book>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // --- Statistics Persistence ---
    fun saveSession(context: Context, session: ReadingSession) {
        val currentList = loadSessions(context).toMutableList()
        currentList.add(session)

        val json = gson.toJson(currentList)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATISTICS, json)
            .apply()
    }

    fun loadSessions(context: Context): List<ReadingSession> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_STATISTICS, null)

        return if (json != null) {
            try {
                val type = object : TypeToken<List<ReadingSession>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    // --- Chat History Persistence ---
    fun saveChatHistory(context: Context, messages: List<ChatMessage>) {
        val json = gson.toJson(messages)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CHAT_HISTORY, json)
            .apply()
    }

    fun loadChatHistory(context: Context): List<ChatMessage> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CHAT_HISTORY, null)
            
        return if (json != null) {
             try {
                val type = object : TypeToken<List<ChatMessage>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
}
