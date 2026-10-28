package com.example.memory

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class SavedChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String, // "user" or "maya"
    val text: String,
    val timestamp: String = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
)

@Serializable
data class MemoryItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val dateAdded: String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
)

object MayaMemoryManager {
    private const val TAG = "MayaMemory"
    private const val PREF_NAME = "MayaMemoryPrefs"
    private const val KEY_MEMORIES = "key_saved_memories"
    private const val KEY_CHAT_HISTORY = "key_saved_chats"

    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null

    private val _memoriesFlow = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memoriesFlow = _memoriesFlow.asStateFlow()

    private val _chatHistoryFlow = MutableStateFlow<List<SavedChatMessage>>(emptyList())
    val chatHistoryFlow = _chatHistoryFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadMemories()
            loadChats()
        }
    }

    private fun loadMemories() {
        val raw = prefs?.getString(KEY_MEMORIES, null)
        val list = if (!raw.isNullOrBlank()) {
            try {
                val parsed = json.decodeFromString<List<MemoryItem>>(raw)
                // Purge any old hardcoded "yadav" memories if previously saved
                parsed.map { item ->
                    if (item.text.contains("yadav", ignoreCase = true)) {
                        item.copy(text = item.text.replace("yadav", "बाबू", ignoreCase = true))
                    } else {
                        item
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            // Default sweet memories to start with
            listOf(
                MemoryItem(text = "बाबू का नाम: बाबू"),
                MemoryItem(text = "माया बाबू की वफादार और प्यारी साथी है"),
                MemoryItem(text = "माया पढ़ाई और सभी कामों में पूरी मदद करती है")
            )
        }
        _memoriesFlow.value = list
    }

    @Synchronized
    fun setUserNameFact(name: String) {
        val cleanName = name.trim().ifEmpty { "बाबू" }
        val current = _memoriesFlow.value.toMutableList()
        current.removeAll { it.text.startsWith("बाबू का नाम:") || it.text.contains("yadav", ignoreCase = true) }
        current.add(0, MemoryItem(text = "बाबू का नाम: $cleanName"))
        _memoriesFlow.value = current
        saveMemoriesToDisk(current)
        Log.i(TAG, "User name memory updated: $cleanName")
    }

    private fun loadChats() {
        val raw = prefs?.getString(KEY_CHAT_HISTORY, null)
        val list = if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<List<SavedChatMessage>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _chatHistoryFlow.value = list
    }

    @Synchronized
    fun addMemory(fact: String) {
        val current = _memoriesFlow.value.toMutableList()
        current.add(0, MemoryItem(text = fact))
        _memoriesFlow.value = current
        saveMemoriesToDisk(current)
        Log.i(TAG, "New memory saved: $fact")
    }

    @Synchronized
    fun removeMemory(id: Long) {
        val current = _memoriesFlow.value.filter { it.id != id }
        _memoriesFlow.value = current
        saveMemoriesToDisk(current)
        Log.i(TAG, "Memory removed id=$id")
    }

    @Synchronized
    fun clearAllMemories() {
        _memoriesFlow.value = emptyList()
        saveMemoriesToDisk(emptyList())
        Log.i(TAG, "All memories cleared upon user request.")
    }

    @Synchronized
    fun recordChatMessage(sender: String, message: String) {
        if (message.isBlank()) return
        val current = _chatHistoryFlow.value.toMutableList()
        current.add(0, SavedChatMessage(sender = sender, text = message))
        if (current.size > 200) { // Keep last 200 messages
            current.removeAt(current.size - 1)
        }
        _chatHistoryFlow.value = current
        saveChatsToDisk(current)
    }

    @Synchronized
    fun removeChatMessage(id: Long) {
        val current = _chatHistoryFlow.value.filter { it.id != id }
        _chatHistoryFlow.value = current
        saveChatsToDisk(current)
    }

    @Synchronized
    fun clearAllChats() {
        _chatHistoryFlow.value = emptyList()
        saveChatsToDisk(emptyList())
        Log.i(TAG, "All chats cleared upon user request.")
    }

    private fun saveMemoriesToDisk(list: List<MemoryItem>) {
        try {
            val encoded = json.encodeToString(list)
            prefs?.edit()?.putString(KEY_MEMORIES, encoded)?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving memories", e)
        }
    }

    private fun saveChatsToDisk(list: List<SavedChatMessage>) {
        try {
            val encoded = json.encodeToString(list)
            prefs?.edit()?.putString(KEY_CHAT_HISTORY, encoded)?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving chats", e)
        }
    }

    fun getMemoriesSummaryForPrompt(): String {
        val list = _memoriesFlow.value
        if (list.isEmpty()) return "No specific long-term memories saved yet."
        return list.joinToString("\n") { "- ${it.text}" }
    }

    fun exportToFile(context: Context): File? {
        return try {
            val file = File(context.filesDir, "maya_memories_export.txt")
            val sb = java.lang.StringBuilder()
            sb.append("=== MAYA AI - BABU KI YAADEIN AUR BATEIN ===\n\n")
            sb.append("--- YAADEIN (MEMORIES) ---\n")
            _memoriesFlow.value.forEach {
                sb.append("[${it.dateAdded}] ${it.text}\n")
            }
            sb.append("\n--- CHAT ITIHAS (CHATS) ---\n")
            _chatHistoryFlow.value.reversed().forEach {
                sb.append("[${it.timestamp}] ${it.sender.uppercase()}: ${it.text}\n")
            }
            file.writeText(sb.toString())
            file
        } catch (e: Exception) {
            Log.e(TAG, "Export error", e)
            null
        }
    }
}
