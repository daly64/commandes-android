package fr.salon.commandes.data

import kotlinx.serialization.Serializable

// Modèles qui correspondent aux réponses de l'API commandes

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
    val id: Int,
    val table: Int,
    val lignes: List<LigneCommande> = emptyList(),
    val statut: String,
    val creeeLe: String = "",
)

@Serializable
data class NouvelleCommande(
    val table: Int,
    val lignes: List<LigneCommande>,
)

@Serializable
data class ChangementStatut(
    val statut: String,
)
