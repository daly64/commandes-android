package fr.salon.commandes.data

import kotlinx.serialization.Serializable

// Modèles qui correspondent aux réponses de l'API commandes

@Serializable
data class Categorie(val id: String, val nom: String, val visible: Boolean)

@Serializable
data class Produit(
    val id: String,
    val nom: String,
    val prix: Double,
    val categorieId: String,
    val visible: Boolean,
)

@Serializable
data class Menu(val categories: List<Categorie>, val produits: List<Produit>)

@Serializable
data class LigneCommande(val produit: String, val quantite: Int)

@Serializable
data class Commande(
    val id: Int,
    val table: Int,
    val lignes: List<LigneCommande>,
    val statut: String,
    val creeeLe: String,
)

@Serializable
data class NouvelleCommande(val table: Int, val lignes: List<LigneCommande>)

@Serializable
data class ChangementStatut(val statut: String)
