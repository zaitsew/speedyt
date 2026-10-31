package com.fabriquefutur.speedyt.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fabriquefutur.speedyt.analysis.ConnectionKind
import com.fabriquefutur.speedyt.analysis.WifiBand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One stored result. Nothing here identifies a network or a person. */
@Serializable
data class HistoryEntry(
    val timestampMillis: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val connection: ConnectionKind,
    val wifiBand: WifiBand? = null,
)

/** JSON in, JSON out, newest first, capped. Pure, so it is unit-tested. */
object HistoryCodec {
    const val MAX_ENTRIES = 50

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun encode(entries: List<HistoryEntry>): String =
        json.encodeToString(entries.take(MAX_ENTRIES))

    /** Corrupt or unreadable data reads as an empty history, never as a crash. */
    fun decode(text: String?): List<HistoryEntry> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<HistoryEntry>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Puts [entry] first and drops everything beyond [MAX_ENTRIES]. */
    fun add(existing: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        (listOf(entry) + existing).take(MAX_ENTRIES)
}

private val Context.historyStore by preferencesDataStore(name = "history")

class HistoryRepository(context: Context) {
    private val store = context.applicationContext.historyStore
    private val key = stringPreferencesKey("entries")

    val entries: Flow<List<HistoryEntry>> = store.data.map { HistoryCodec.decode(it[key]) }

    suspend fun add(entry: HistoryEntry) {
        store.edit { prefs ->
            prefs[key] = HistoryCodec.encode(HistoryCodec.add(HistoryCodec.decode(prefs[key]), entry))
        }
    }
}
