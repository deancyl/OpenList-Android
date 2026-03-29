package org.openlist.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openlist_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val SERVER_URL = stringPreferencesKey("server_url")
        private val USERNAME = stringPreferencesKey("username")
        private val PASSWORD = stringPreferencesKey("password")
        private val TOKEN = stringPreferencesKey("token")
        private val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val SORT_ORDER = stringPreferencesKey("sort_order")
        private val GRID_VIEW = booleanPreferencesKey("grid_view")
    }

    // Server URL
    val serverUrl: Flow<String> = dataStore.data.map { it[SERVER_URL] ?: "" }
    suspend fun getServerUrl(): String = dataStore.data.first()[SERVER_URL] ?: ""

    suspend fun setServerUrl(url: String) {
        dataStore.edit { it[SERVER_URL] = url }
    }

    // Token
    val token: Flow<String?> = dataStore.data.map { it[TOKEN] }
    suspend fun getToken(): String? = dataStore.data.first()[TOKEN]

    suspend fun setToken(token: String?) {
        dataStore.edit { it[TOKEN] = token ?: "" }
    }

    // Username
    val username: Flow<String> = dataStore.data.map { it[USERNAME] ?: "" }
    suspend fun getUsername(): String = dataStore.data.first()[USERNAME] ?: ""

    suspend fun setUsername(name: String) {
        dataStore.edit { it[USERNAME] = name }
    }

    // Password (stored temporarily for re-auth)
    suspend fun setPassword(pwd: String) {
        dataStore.edit { it[PASSWORD] = pwd }
    }

    suspend fun getPassword(): String = dataStore.data.first()[PASSWORD] ?: ""

    // Login state
    val isLoggedIn: Flow<Boolean> = dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    suspend fun getIsLoggedIn(): Boolean = dataStore.data.first()[IS_LOGGED_IN] ?: false

    suspend fun setLoggedIn(loggedIn: Boolean) {
        dataStore.edit { it[IS_LOGGED_IN] = loggedIn }
    }

    // Theme
    val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }
    suspend fun getThemeMode(): String = dataStore.data.first()[THEME_MODE] ?: "system"

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    // Sort order
    val sortOrder: Flow<String> = dataStore.data.map { it[SORT_ORDER] ?: "name" }
    suspend fun getSortOrder(): String = dataStore.data.first()[SORT_ORDER] ?: "name"

    suspend fun setSortOrder(order: String) {
        dataStore.edit { it[SORT_ORDER] = order }
    }

    // Grid view
    val gridView: Flow<Boolean> = dataStore.data.map { it[GRID_VIEW] ?: false }
    suspend fun getGridView(): Boolean = dataStore.data.first()[GRID_VIEW] ?: false

    suspend fun setGridView(enabled: Boolean) {
        dataStore.edit { it[GRID_VIEW] = enabled }
    }

    // Clear all
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    // Clear cache only (keeps login info)
    suspend fun clearCache() {
        dataStore.edit {
            it.remove(SORT_ORDER)
            it.remove(GRID_VIEW)
        }
    }
}
