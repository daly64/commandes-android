package fr.salon.commandes.data

import kotlinx.serialization.Serializable

// Modèles qui correspondent au schéma GraphQL de l'API commandes

@Serializable
data class Categorie(
    val id: String,
    val nom: String,
    val visible: Boolean = true,
)

@Serializable
data class Produit(
    val id: String,
    val nom: String,
    val prix: Double = 0.0,
    val categorieId: String,
    val visible: Boolean = true,
)

@Serializable
data class Menu(
    val categories: List<Categorie> = emptyList(),
    val produits: List<Produit> = emptyList(),
)

@Serializable
data class LigneCommande(
    val produit: String,
    val quantite: Int,
)

@Serializable
data class Commande(
    val id: String,
    val table: Int,
    val lignes: List<LigneCommande> = emptyList(),
    val statut: String,
    val creeeLe: String = "",
)

// Étapes d'une commande, dans l'ordre, avec le libellé du bouton qui mène à l'étape suivante
enum class Statut(val libelle: String, val action: String?) {
    EN_ATTENTE("En attente", "Démarrer"),
    EN_PREPARATION("En préparation", "Marquer prête"),
    PRETE("Prête", "Marquer servie"),
    SERVIE("Servie", "Marquer payée"),
    PAYEE("Payée", null);

    val suivant: Statut?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun de(valeur: String): Statut? = entries.firstOrNull { it.name == valeur }
    }
}
