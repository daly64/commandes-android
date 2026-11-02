package fr.salon.commandes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.salon.commandes.data.ApiClient
import fr.salon.commandes.data.CleStore
import fr.salon.commandes.data.Commande
import fr.salon.commandes.data.ErreurApi
import fr.salon.commandes.data.LigneCommande
import fr.salon.commandes.data.Menu
import fr.salon.commandes.data.NouvelleCommande
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

private const val INTERVALLE_MS = 3000L

// Ce que l'écran affiche
data class Etat(
    val cleValide: Boolean,
    val menu: Menu? = null,
    val commandes: List<Commande> = emptyList(),
    val connexion: String = "", // état de la liaison avec le serveur
    val alerte: String = "",    // message de la dernière action ou de l'écran de clé
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = CleStore(application)
    private val api = ApiClient { store.lire().orEmpty() }

    // Une seule lecture à la fois, pour que les réponses s'appliquent dans l'ordre
    private val verrou = Mutex()

    private val _etat = MutableStateFlow(Etat(cleValide = store.lire() != null))
    val etat: StateFlow<Etat> = _etat.asStateFlow()

    init {
        // Interrogation régulière de l'API ; s'arrête quand le ViewModel est détruit
        viewModelScope.launch {
            while (true) {
                rafraichir()
                delay(INTERVALLE_MS)
            }
        }
    }

    fun valider(cle: String) {
        store.enregistrer(cle.trim())
        _etat.update { it.copy(cleValide = true, alerte = "") }
        viewModelScope.launch { rafraichir() }
    }

    fun creerCommande(table: Int, lignes: List<LigneCommande>) {
        action {
            withContext(Dispatchers.IO) { api.creerCommande(NouvelleCommande(table, lignes)) }
        }
    }

    fun changerStatut(id: Int, statut: String) {
        action {
            withContext(Dispatchers.IO) { api.changerStatut(id, statut) }
        }
    }

    // Exécute une action, affiche son éventuelle erreur, puis relit la liste
    private fun action(bloc: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                bloc()
                _etat.update { it.copy(alerte = "") }
            } catch (e: ErreurApi) {
                gererErreur(e)
            } catch (e: IOException) {
                _etat.update { it.copy(alerte = "Serveur injoignable") }
            }
            rafraichir()
        }
    }

    private suspend fun rafraichir() {
        verrou.withLock {
            if (!_etat.value.cleValide) return@withLock

            // Le menu est relu tant qu'il n'a pas été chargé une fois
            if (_etat.value.menu == null) {
                try {
                    val menu = withContext(Dispatchers.IO) { api.menu() }
                    _etat.update { it.copy(menu = menu) }
                } catch (e: Exception) {
                    // Nouvel essai au prochain cycle
                }
            }

            try {
                val commandes = withContext(Dispatchers.IO) { api.commandes() }
                _etat.update { it.copy(commandes = commandes, connexion = "") }
            } catch (e: ErreurApi) {
                gererErreur(e)
            } catch (e: IOException) {
                _etat.update { it.copy(connexion = "Serveur injoignable, nouvelle tentative…") }
            }
        }
    }

    // Clé refusée : retour à l'écran de saisie. Autres erreurs : message affiché.
    private fun gererErreur(e: ErreurApi) {
        if (e.code == 401) {
            store.effacer()
            _etat.update { Etat(cleValide = false, alerte = "Clé refusée") }
        } else {
            _etat.update { it.copy(alerte = e.message ?: "Erreur ${e.code}") }
        }
    }
}
