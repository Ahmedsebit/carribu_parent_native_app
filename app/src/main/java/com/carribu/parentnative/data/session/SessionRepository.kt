package com.carribu.parentnative.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.carribu.parentnative.data.model.User
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.sessionDataStore by preferencesDataStore("secure_session")

data class Session(
    val token: String,
    val user: User,
    val passwordRequirementSatisfied: Boolean
) {
    val requiresPasswordChange: Boolean
        get() = user.mustSetPassword == true && !passwordRequirementSatisfied
}

class SessionRepository(
    private val context: Context,
    moshi: Moshi
) {
    private val userAdapter = moshi.adapter(User::class.java)
    @Volatile private var memoryToken: String? = null

    val session: Flow<Session?> = context.sessionDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            val token = preferences[TOKEN]?.let(::decrypt)
            val userJson = preferences[USER]?.let(::decrypt)
            val user = userJson?.let(userAdapter::fromJson)
            if (token != null && user != null) {
                memoryToken = token
                Session(token, user, preferences[PASSWORD_SATISFIED] ?: (user.mustSetPassword != true))
            } else {
                memoryToken = null
                null
            }
        }

    fun token(): String? = memoryToken

    suspend fun save(token: String, user: User) {
        memoryToken = token
        context.sessionDataStore.edit {
            it[TOKEN] = encrypt(token)
            it[USER] = encrypt(userAdapter.toJson(user))
            it[PASSWORD_SATISFIED] = user.mustSetPassword != true
        }
    }

    suspend fun updateUser(user: User) {
        context.sessionDataStore.edit { it[USER] = encrypt(userAdapter.toJson(user)) }
    }

    suspend fun markPasswordSatisfied() {
        context.sessionDataStore.edit { it[PASSWORD_SATISFIED] = true }
    }

    suspend fun clear() {
        memoryToken = null
        context.sessionDataStore.edit { it.clear() }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? = try {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        if (payload.size <= IV_SIZE) return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, IV_SIZE)))
        String(cipher.doFinal(payload.copyOfRange(IV_SIZE, payload.size)), Charsets.UTF_8)
    } catch (error: Exception) {
        Log.w("CarribuSession", "Stored session could not be decrypted; it will be cleared on next sign-in.", error)
        null
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER = stringPreferencesKey("user")
        val PASSWORD_SATISFIED = booleanPreferencesKey("password_satisfied")
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "carribu_parent_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
