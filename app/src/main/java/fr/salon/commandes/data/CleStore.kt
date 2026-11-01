package fr.salon.commandes.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// La clé d'accès est stockée chiffrée sur l'appareil
class CleStore(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "cles",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun lire(): String? = prefs.getString(CLE, null)

    fun enregistrer(cle: String) {
        prefs.edit().putString(CLE, cle).apply()
    }

    fun effacer() {
        prefs.edit().remove(CLE).apply()
    }

    private companion object {
        const val CLE = "cle"
    }
}
