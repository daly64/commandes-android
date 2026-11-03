@file:OptIn(ExperimentalMaterial3Api::class)

package fr.salon.commandes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.salon.commandes.AppViewModel
import fr.salon.commandes.Etat
import fr.salon.commandes.data.Produit

private const val NOMBRE_TABLES = 10
private const val TABLES_PAR_RANGEE = 5

@Composable
fun EcranNouvelleCommande(etat: Etat, vm: AppViewModel) {
    val menu = etat.menu

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "tables") {
                CarteTables(table = etat.table, onChoisir = { numero -> vm.choisirTable(numero) })
            }

            if (menu == null) {
                item(key = "chargement") { CarteInfo(texte = "Chargement du menu…") }
            } else {
                val produitsVisibles = menu.produits.filter { it.visible }
                for (categorie in menu.categories.filter { it.visible }) {
                    val produits = produitsVisibles.filter { it.categorieId == categorie.id }
                    if (produits.isEmpty()) continue

                    item(key = "categorie-${categorie.id}") {
                        TitreSection(texte = categorie.nom.trim())
                    }
                    items(items = produits, key = { produit -> "produit-${produit.id}" }) { produit ->
                        CarteProduit(
                            produit = produit,
                            quantite = etat.panier[produit.id] ?: 0,
                            onAjouter = { vm.ajouter(produit.id) },
                            onRetirer = { vm.retirer(produit.id) },
                        )
                    }
                }
            }
        }

        CartePanier(
            etat = etat,
            onVider = { vm.viderPanier() },
            onEnvoyer = { vm.envoyer() },
        )
    }
}

// Grille de 10 tables, en deux rangées de 5 grandes cases faciles à toucher
@Composable
private fun CarteTables(table: Int?, onChoisir: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Table", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            for (rangee in 0 until NOMBRE_TABLES / TABLES_PAR_RANGEE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (rangee > 0) 8.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (colonne in 1..TABLES_PAR_RANGEE) {
                        val numero = rangee * TABLES_PAR_RANGEE + colonne
                        val choisie = table == numero
                        Surface(
                            onClick = { onChoisir(numero) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (choisie) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (choisie) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$numero",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Carte produit : un appui ajoute une unité ; « − » en retire une
@Composable
private fun CarteProduit(produit: Produit, quantite: Int, onAjouter: () -> Unit, onRetirer: () -> Unit) {
    val choisi = quantite > 0

    Card(
        onClick = onAjouter,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (choisi) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = produit.nom.trim(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = formaterPrix(produit.prix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (choisi) {
                FilledTonalIconButton(onClick = onRetirer) {
                    Text(text = "−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$quantite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
            FilledIconButton(onClick = onAjouter) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Ajouter ${produit.nom.trim()}")
            }
        }
    }
}

// Récapitulatif fixe en bas de l'écran : table, nombre d'articles, total et envoi
@Composable
private fun CartePanier(etat: Etat, onVider: () -> Unit, onEnvoyer: () -> Unit) {
    val articles = etat.panier.values.sum()
    val produits = etat.menu?.produits.orEmpty()
    val total = etat.panier.entries.sumOf { (id, quantite) ->
        (produits.firstOrNull { it.id == id }?.prix ?: 0.0) * quantite
    }
    val table = etat.table

    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (table != null) "Table $table" else "Aucune table choisie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (articles == 0) "Panier vide" else "$articles article(s) · ${formaterPrix(total)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (articles > 0) {
                    TextButton(onClick = onVider) { Text("Vider") }
                }
            }
            Button(
                onClick = onEnvoyer,
                enabled = !etat.envoiEnCours && articles > 0 && table != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(52.dp),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (etat.envoiEnCours) "Envoi…" else "Envoyer en cuisine")
            }
        }
    }
}
