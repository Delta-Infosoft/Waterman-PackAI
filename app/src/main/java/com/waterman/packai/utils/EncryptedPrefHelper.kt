package com.waterman.packai.utils

import android.content.SharedPreferences
import com.google.gson.Gson
import com.waterman.packai.network.response.LoginUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPrefHelper @Inject constructor(private val sharedPreferences: SharedPreferences) {

    private val gson = Gson()

    /*===============================================================================================*/
    /* ---------- Save ---------- */

    fun putString(key: String, value: String) = sharedPreferences.edit().putString(key, value).apply()

    fun putInt(key: String, value: Int) = sharedPreferences.edit().putInt(key, value).apply()

    fun putBoolean(key: String, value: Boolean) = sharedPreferences.edit().putBoolean(key, value).apply()

    fun putLong(key: String, value: Long) = sharedPreferences.edit().putLong(key, value).apply()

    fun saveFCMToken(token: String) {
        sharedPreferences.edit().putString("fcm_token", token).apply()
    }

    fun saveUser(user: LoginUser) {
        val json = gson.toJson(user)
        sharedPreferences.edit().putString("user", json).apply()
    }

    fun saveUserIp(ip : String){
        sharedPreferences.edit().putString("ip", ip).apply()
    }

    fun saveBaseUrl(ip: String) {
        val baseUrl = if (ip.startsWith("http")) {
            ip
        } else {
            "http://$ip/"
        }
        sharedPreferences.edit().putString("base_url", baseUrl).apply()
    }

/*===============================================================================================*/
    /* ---------- Get ---------- */
    fun getBaseUrl(): String {
        return sharedPreferences.getString("base_url", "http://localhost/")!!
    }

    fun getFCMToken() : String {
        return sharedPreferences.getString("fcm_token", "") ?: ""
    }

    fun getUser(): LoginUser? {
        val json = sharedPreferences.getString("user", "")
        return gson.fromJson(json, LoginUser::class.java)
    }

    fun getUserIp() : String{
        return sharedPreferences.getString("ip", "") ?: ""
    }

    fun getString(key: String, default: String = ""): String =
        sharedPreferences.getString(key, default) ?: default

    fun getInt(key: String, default: Int = 0): Int =
        sharedPreferences.getInt(key, default)

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        sharedPreferences.getBoolean(key, default)

    fun getLong(key: String, default: Long = 0L): Long =
        sharedPreferences.getLong(key, default)

    /*===============================================================================================*/
    /* ---------- Clear ---------- */

    fun remove(key: String) =
        sharedPreferences.edit().remove(key).apply()

    fun clear() =
        sharedPreferences.edit().clear().apply()

    private companion object {
        const val KEY_BRAND_ID = "brand_id"
        const val KEY_BRAND_NAME = "brand_name"
    }

    fun saveSelectedBrand(id: String, name: String) {
        sharedPreferences.edit()
            .putString(KEY_BRAND_ID, id)
            .putString(KEY_BRAND_NAME, name)
            .apply()
    }

    fun getSelectedBrandName(): String? {
        return sharedPreferences.getString(KEY_BRAND_NAME, "")
    }

    fun getSelectedBrandId(): String? {
        return sharedPreferences.getString(KEY_BRAND_ID, "")
    }

    fun isBrandSelected(): Boolean {
        return !getSelectedBrandId().isNullOrEmpty()
    }
}
