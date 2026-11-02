package fr.salon.commandes.data

import android.content.Context

// La clé d'accès est gardée dans le stockage privé de l'application,
// inaccessible aux autres applications de l'appareil.
class CleStore(context: Context) {

    private val prefs = context.getSharedPreferences("acces", Context.MODE_PRIVATE)

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
