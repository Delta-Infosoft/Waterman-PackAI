package com.waterman.packai.securepref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EncryptedPrefModule {

    private const val PREF_NAME = "secure_shared_pre_delta_attendance_app"

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return try {
            createEncryptedSharedPreferences(context)
        } catch (e: Exception) {
            // Keystore key is corrupted or invalidated (e.g. app reinstall, backup/restore,
            // or lock screen change). Wipe corrupted data and recreate from scratch.
            // NOTE: This will log the user out — their encrypted prefs cannot be recovered.
            clearCorruptedPreferences(context)
            createEncryptedSharedPreferences(context)
        }
    }

    private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false) // prevents invalidation on lock screen/biometric changes
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun clearCorruptedPreferences(context: Context) {
        // Clear the prefs file itself
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()

        // Delete the XML file on disk
        val prefFile = File(context.filesDir.parent, "shared_prefs/$PREF_NAME.xml")
        if (prefFile.exists()) prefFile.delete()

        // Delete the Tink keyset that AndroidKeysetManager stores alongside the prefs
        val keysetPrefName = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().remove(keysetPrefName).apply()
    }
}