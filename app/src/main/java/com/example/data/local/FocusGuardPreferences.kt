package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BlockedAppRule
import com.example.data.model.InterventionConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.focusGuardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focusguard_prefs"
)

/**
 * Single persistence boundary for FocusGuard.
 *
 * Structured values (rule list, intervention config, active session) are stored as
 * JSON strings via Moshi, which is already on the classpath with KSP codegen. Room is
 * intentionally not used here: nothing in Phase 1 needs querying or history, only
 * whole-object read/write.
 *
 * Every read is defensive. A corrupt or schema-changed JSON blob must never crash the
 * app on launch, so decode failures fall back to the caller-supplied default.
 */
class FocusGuardPreferences(context: Context) {

    private val dataStore = context.applicationContext.focusGuardDataStore
    private val moshi: Moshi = Moshi.Builder().build()

    private val ruleListAdapter =
        moshi.adapter<List<BlockedAppRule>>(
            Types.newParameterizedType(List::class.java, BlockedAppRule::class.java)
        )
    private val interventionAdapter = moshi.adapter(InterventionConfig::class.java)
    private val sessionAdapter = moshi.adapter(PersistedSession::class.java)

    private object Keys {
        val RULES = stringPreferencesKey("rules_json")
        val INTERVENTION = stringPreferencesKey("intervention_json")
        val SESSION = stringPreferencesKey("session_json")
        val GOAL = stringPreferencesKey("today_goal")
        val MASTER_SHIELD = booleanPreferencesKey("master_shield")
        val SEEDED = booleanPreferencesKey("defaults_seeded")
    }

    /** IOException on read means the file is unreadable; surface empty rather than crash. */
    private val preferences: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Log.w(TAG, "Could not read preferences, falling back to defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }

    fun rulesFlow(default: List<BlockedAppRule>): Flow<List<BlockedAppRule>> =
        preferences.map { prefs ->
            val json = prefs[Keys.RULES] ?: return@map default
            decode(json, default) { ruleListAdapter.fromJson(it) }
        }

    fun interventionFlow(default: InterventionConfig): Flow<InterventionConfig> =
        preferences.map { prefs ->
            val json = prefs[Keys.INTERVENTION] ?: return@map default
            decode(json, default) { interventionAdapter.fromJson(it) }
        }

    fun sessionFlow(): Flow<PersistedSession?> =
        preferences.map { prefs ->
            val json = prefs[Keys.SESSION] ?: return@map null
            decode(json, null) { sessionAdapter.fromJson(it) }
        }

    fun goalFlow(default: String): Flow<String> =
        preferences.map { it[Keys.GOAL] ?: default }

    fun masterShieldFlow(default: Boolean): Flow<Boolean> =
        preferences.map { it[Keys.MASTER_SHIELD] ?: default }

    fun seededFlow(): Flow<Boolean> = preferences.map { it[Keys.SEEDED] ?: false }

    suspend fun saveRules(rules: List<BlockedAppRule>) =
        write { it[Keys.RULES] = ruleListAdapter.toJson(rules) }

    suspend fun saveIntervention(config: InterventionConfig) =
        write { it[Keys.INTERVENTION] = interventionAdapter.toJson(config) }

    suspend fun saveSession(session: PersistedSession?) = write { prefs ->
        if (session == null) prefs.remove(Keys.SESSION)
        else prefs[Keys.SESSION] = sessionAdapter.toJson(session)
    }

    suspend fun saveGoal(goal: String) = write { it[Keys.GOAL] = goal }

    suspend fun saveMasterShield(enabled: Boolean) = write { it[Keys.MASTER_SHIELD] = enabled }

    suspend fun markSeeded() = write { it[Keys.SEEDED] = true }

    private suspend fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            dataStore.edit(block)
        } catch (io: IOException) {
            // A failed write must not take the app down; the in-memory state stays correct
            // and the next successful write will reconcile it.
            Log.e(TAG, "Failed to persist preferences", io)
        }
    }

    private fun <T> decode(json: String, fallback: T, parse: (String) -> T?): T =
        try {
            parse(json) ?: fallback
        } catch (e: Exception) {
            Log.w(TAG, "Corrupt persisted value, using fallback", e)
            fallback
        }

    private companion object {
        const val TAG = "FocusGuardPrefs"
    }
}
