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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.salon.commandes.AppViewModel
import fr.salon.commandes.Etat
import fr.salon.commandes.data.Produit

private const val NOMBRE_TABLES = 10

// Tables et recherche restent en haut ; la liste des produits défile dessous ; le panier reste en bas
@Composable
fun EcranNouvelleCommande(etat: Etat, vm: AppViewModel) {
    val menu = etat.menu
    var recherche by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                BandeauTables(table = etat.table, onChoisir = { numero -> vm.choisirTable(numero) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    placeholder = { Text("Rechercher un produit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (menu == null) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CarteInfo(texte = "Chargement du menu…")
            }
        } else {
            val terme = recherche.trim()
            val produitsVisibles = menu.produits.filter {
                it.visible && (terme.isEmpty() || it.nom.contains(terme, ignoreCase = true))
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

// Les 10 tables sur une seule ligne, toujours visibles
@Composable
private fun BandeauTables(table: Int?, onChoisir: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (numero in 1..NOMBRE_TABLES) {
            val choisie = table == numero
            Surface(
                onClick = { onChoisir(numero) },
                shape = RoundedCornerShape(8.dp),
                color = if (choisie) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (choisie) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "$numero",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// Carte produit : un appui sur la carte ajoute une unité ; le compteur « − n + » apparaît dès qu'un produit est choisi
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                FilledTonalIconButton(onClick = onRetirer, modifier = Modifier.size(40.dp)) {
                    Text(text = "−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$quantite",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 32.dp)
                        .padding(horizontal = 8.dp),
                )
            }
            FilledIconButton(onClick = onAjouter, modifier = Modifier.size(40.dp)) {
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

    // Le libellé du bouton indique ce qui manque avant l'envoi
    val libelleBouton = when {
        etat.envoiEnCours -> "Envoi…"
        table == null -> "Choisissez une table"
        articles == 0 -> "Ajoutez des produits"
        else -> "Envoyer en cuisine"
    }

    Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    .padding(top = 8.dp)
                    .height(52.dp),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = libelleBouton)
            }
        }
    }
}
