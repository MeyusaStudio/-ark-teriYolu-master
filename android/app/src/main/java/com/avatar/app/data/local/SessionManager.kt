package com.avatar.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.avatar.app.data.model.User
import com.google.gson.Gson

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "avatar_session_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER = "user_json"
        private const val KEY_SERVER_URL = "server_url"
        
        // Varsayılan olarak bilgisayarınızın yerel Wi-Fi IP adresi (192.168.18.5) tanımlıdır
        const val DEFAULT_SERVER_URL = "http://192.168.18.5:3000/api/"
        const val USB_REVERSE_URL = "http://127.0.0.1:3000/api/"
        const val EMULATOR_URL = "http://10.0.2.2:3000/api/"
    }

    fun saveSession(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null && getUser() != null
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER)
            .apply()
    }

    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }

    fun setServerUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_SERVER_URL, cleanUrl).apply()
    }
}
