package fr.salon.commandes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.salon.commandes.data.ApiClient
import fr.salon.commandes.data.CleStore
import fr.salon.commandes.data.Commande
import fr.salon.commandes.data.ErreurApi
import fr.salon.commandes.data.FluxCommandes
import fr.salon.commandes.data.LigneCommande
import fr.salon.commandes.data.Menu
import fr.salon.commandes.data.Statut
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

private const val INTERVALLE_SANS_FLUX_MS = 3_000L   // rafraîchissement quand le temps réel est coupé
private const val INTERVALLE_AVEC_FLUX_MS = 30_000L  // rafraîchissement de secours quand il fonctionne
private const val VERIFICATION_FLUX_MS = 5_000L      // délai avant de rouvrir un flux fermé
private const val QUANTITE_MAX = 50

// Tout ce que l'interface affiche
data class Etat(
    val cleValide: Boolean,
    val alerteCle: String = "",              // message de l'écran de clé
    val menu: Menu? = null,
    val commandes: List<Commande> = emptyList(),
    val table: Int? = null,                  // table choisie pour la nouvelle commande
    val panier: Map<String, Int> = emptyMap(), // identifiant du produit -> quantité
    val envoiEnCours: Boolean = false,
    val enCours: Set<String> = emptySet(),   // commandes dont le statut est en train de changer
    val connexion: String = "",              // problème de liaison avec le serveur
    val tempsReel: Boolean = false,          // flux SSE ouvert
    val message: String? = null,             // message ponctuel (barre en bas de l'écran)
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val store = CleStore(application)
    private val api = ApiClient(BuildConfig.API_URL) { store.lire().orEmpty() }
    private val flux = FluxCommandes(BuildConfig.API_URL) { store.lire().orEmpty() }

    @Volatile
    private var fluxEnConnexion = false

    // Une seule lecture à la fois, pour que les réponses s'appliquent dans l'ordre
    private val verrou = Mutex()

    private val _etat = MutableStateFlow(Etat(cleValide = store.lire() != null))
    val etat: StateFlow<Etat> = _etat.asStateFlow()

    init {
        // Rafraîchissement régulier : rapide sans temps réel, de secours avec
        viewModelScope.launch {
            while (true) {
                rafraichir()
                delay(if (_etat.value.tempsReel) INTERVALLE_AVEC_FLUX_MS else INTERVALLE_SANS_FLUX_MS)
            }
        }
        // Ouverture et réouverture du flux temps réel tant que la clé est valide
        viewModelScope.launch {
            while (true) {
                val etat = _etat.value
                if (etat.cleValide && !etat.tempsReel && !fluxEnConnexion) {
                    ouvrirFlux()
                }
                delay(VERIFICATION_FLUX_MS)
            }
        }
    }

    private fun ouvrirFlux() {
        fluxEnConnexion = true
        flux.demarrer(
            surEvenement = { viewModelScope.launch { rafraichir() } },
            surEtat = { ouvert ->
                fluxEnConnexion = false
                _etat.update { it.copy(tempsReel = ouvert) }
                // À l'ouverture, on relit la liste pour ne rien manquer pendant la coupure
                if (ouvert) viewModelScope.launch { rafraichir() }
            },
        )
    }

    override fun onCleared() {
        flux.arreter()
        super.onCleared()
    }

    // --- Clé d'accès ---

    fun valider(cle: String) {
        store.enregistrer(cle.trim())
        _etat.update { it.copy(cleValide = true, alerteCle = "") }
        viewModelScope.launch { rafraichir() }
    }

    fun deconnecter() {
        flux.arreter()
        fluxEnConnexion = false
        store.effacer()
        _etat.value = Etat(cleValide = false)
    }

    // --- Nouvelle commande ---

    fun choisirTable(numero: Int) {
        _etat.update { it.copy(table = numero) }
    }

    fun ajouter(produitId: String) {
        _etat.update {
            val quantite = minOf((it.panier[produitId] ?: 0) + 1, QUANTITE_MAX)
            it.copy(panier = it.panier + (produitId to quantite))
        }
    }

    fun retirer(produitId: String) {
        _etat.update {
            val quantite = (it.panier[produitId] ?: 0) - 1
            val panier = if (quantite <= 0) it.panier - produitId else it.panier + (produitId to quantite)
            it.copy(panier = panier)
        }
    }

    fun viderPanier() {
        _etat.update { it.copy(panier = emptyMap()) }
    }

    fun envoyer() {
        val etat = _etat.value
        val table = etat.table
        val menu = etat.menu
        if (table == null) {
            afficher("Choisissez une table")
            return
        }
        if (menu == null) return

        // L'API attend le nom du produit
        val lignes = etat.panier.mapNotNull { (id, quantite) ->
            menu.produits.firstOrNull { it.id == id }?.let { LigneCommande(it.nom.trim(), quantite) }
        }
        if (lignes.isEmpty()) {
            afficher("Le panier est vide")
            return
        }

        _etat.update { it.copy(envoiEnCours = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { api.creerCommande(table, lignes) }
                _etat.update {
                    it.copy(panier = emptyMap(), table = null, message = "Commande envoyée pour la table $table")
                }
            } catch (e: ErreurApi) {
                gererErreur(e)
            } catch (e: IOException) {
                afficher("Serveur injoignable, la commande n'a pas été envoyée")
            } finally {
                _etat.update { it.copy(envoiEnCours = false) }
            }
            rafraichir()
        }
    }

    // --- Commandes en cours ---

    fun avancer(commande: Commande) {
        val suivant = Statut.de(commande.statut)?.suivant ?: return
        _etat.update { it.copy(enCours = it.enCours + commande.id) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { api.changerStatut(commande.id, suivant) }
            } catch (e: ErreurApi) {
                gererErreur(e)
            } catch (e: IOException) {
                afficher("Serveur injoignable")
            }
            rafraichir()
            _etat.update { it.copy(enCours = it.enCours - commande.id) }
        }
    }

    fun messageVu() {
        _etat.update { it.copy(message = null) }
    }

    // --- Interne ---

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
                if (e.code == "UNAUTHENTICATED") {
                    gererErreur(e)
                } else {
                    _etat.update { it.copy(connexion = e.message ?: "Erreur du serveur") }
                }
            } catch (e: IOException) {
                _etat.update { it.copy(connexion = "Serveur injoignable, nouvelle tentative…") }
            }
        }
    }

    private fun gererErreur(e: ErreurApi) {
        when (e.code) {
            "UNAUTHENTICATED" -> {
                flux.arreter()
                fluxEnConnexion = false
                store.effacer()
                _etat.value = Etat(cleValide = false, alerteCle = "Clé refusée")
            }
            "INTERNAL_SERVER_ERROR" -> afficher("Erreur du serveur, réessayez")
            else -> afficher(e.message ?: "Erreur")
        }
    }

    private fun afficher(texte: String) {
        _etat.update { it.copy(message = texte) }
    }
}
