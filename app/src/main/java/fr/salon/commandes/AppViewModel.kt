package fr.salon.commandes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.salon.commandes.data.ApiClient
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
import kotlinx.coroutines.withContext
import java.io.IOException

private const val DELAI_NOUVEL_ESSAI_MS = 3_000L
private const val QUANTITE_MAX = 50

// Tout ce que l'interface affiche
data class Etat(
    val menu: Menu? = null,
    val commandes: List<Commande> = emptyList(),
    val table: Int? = null,                    // table choisie pour la nouvelle commande
    val panier: Map<String, Int> = emptyMap(), // identifiant du produit -> quantité
    val envoiEnCours: Boolean = false,
    val enCours: Set<String> = emptySet(),     // commandes dont le statut est en train de changer
    val connexion: String = "",                // problème de liaison avec le serveur
    val tempsReel: Boolean = false,            // flux SSE ouvert
    val message: String? = null,               // message ponctuel (barre en bas de l'écran)
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ApiClient(BuildConfig.API_URL)
    private val flux = FluxCommandes(BuildConfig.API_URL)

    private val _etat = MutableStateFlow(Etat())
    val etat: StateFlow<Etat> = _etat.asStateFlow()

    init {
        // Le menu est relu jusqu'à ce qu'il soit chargé une fois
        viewModelScope.launch(Dispatchers.IO) {
            while (_etat.value.menu == null) {
                try {
                    val menu = api.menu()
                    _etat.update { it.copy(menu = menu) }
                } catch (e: Exception) {
                    delay(DELAI_NOUVEL_ESSAI_MS)
                }
            }
        }

        // Temps réel : à chaque ouverture du flux, la liste est relue avant les événements suivants
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    flux.ecouter(
                        surOuverture = {
                            chargerCommandes()
                            _etat.update { it.copy(tempsReel = true) }
                        },
                        surCommande = { appliquer(it) },
                    )
                } catch (e: Exception) {
                    _etat.update { it.copy(connexion = "Serveur injoignable, nouvelle tentative…") }
                }
                _etat.update { it.copy(tempsReel = false) }
                delay(DELAI_NOUVEL_ESSAI_MS)
            }
        }
    }

    override fun onCleared() {
        flux.arreter()
        super.onCleared()
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
                withContext(Dispatchers.IO) {
                    api.creerCommande(table, lignes)
                    chargerCommandes()
                }
                _etat.update {
                    it.copy(panier = emptyMap(), table = null, message = "Commande envoyée pour la table $table")
                }
            } catch (e: ErreurApi) {
                afficherErreur(e)
            } catch (e: IOException) {
                afficher("Serveur injoignable, la commande n'a pas été envoyée")
            } finally {
                _etat.update { it.copy(envoiEnCours = false) }
            }
        }
    }

    // --- Commandes en cours ---

    fun avancer(commande: Commande) {
        val suivant = Statut.de(commande.statut)?.suivant ?: return
        _etat.update { it.copy(enCours = it.enCours + commande.id) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    api.changerStatut(commande.id, suivant)
                    chargerCommandes()
                }
            } catch (e: ErreurApi) {
                afficherErreur(e)
            } catch (e: IOException) {
                afficher("Serveur injoignable")
            } finally {
                _etat.update { it.copy(enCours = it.enCours - commande.id) }
            }
        }
    }

    fun messageVu() {
        _etat.update { it.copy(message = null) }
    }

    // --- Interne ---

    // Relit la liste complète ; appelé hors du thread principal
    private fun chargerCommandes() {
        try {
            val liste = api.commandes()
            _etat.update { it.copy(commandes = liste, connexion = "") }
        } catch (e: ErreurApi) {
            _etat.update { it.copy(connexion = e.message ?: "Erreur du serveur") }
        } catch (e: IOException) {
            _etat.update { it.copy(connexion = "Serveur injoignable, nouvelle tentative…") }
        }
    }

    // Applique une commande reçue par le flux : remplacée ou ajoutée, retirée une fois payée
    private fun appliquer(commande: Commande) {
        _etat.update { actuel ->
            val autres = actuel.commandes.filter { it.id != commande.id }
            val liste = if (commande.statut == "PAYEE") autres else (autres + commande).sortedBy { it.creeeLe }
            actuel.copy(commandes = liste)
        }
    }

    private fun afficherErreur(e: ErreurApi) {
        afficher(if (e.code == "INTERNAL_SERVER_ERROR") "Erreur du serveur, réessayez" else e.message ?: "Erreur")
    }

    private fun afficher(texte: String) {
        _etat.update { it.copy(message = texte) }
    }
}