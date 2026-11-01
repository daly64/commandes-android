package fr.salon.commandes.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import fr.salon.commandes.data.LigneCommande
import fr.salon.commandes.data.Menu

// Une ligne en cours de saisie ; la quantité reste du texte jusqu'à l'envoi
private data class Saisie(val id: Int, val produit: String = "", val quantite: String = "1")

@Composable
fun CarteFormulaire(menu: Menu?, onEnvoyer: (Int, List<LigneCommande>) -> Unit) {
    var table by remember { mutableIntStateOf(1) }
    var prochainId by remember { mutableIntStateOf(2) }
    var lignes by remember { mutableStateOf(listOf(Saisie(1))) }
    var erreur by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Table", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..10).forEach { n ->
                FilterChip(
                    selected = table == n,
                    onClick = { table = n },
                    label = { Text("$n") },
                )
            }
        }

        Text("Produits", style = MaterialTheme.typography.labelLarge)
        lignes.forEach { ligne ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SelecteurProduit(
                    menu = menu,
                    valeur = ligne.produit,
                    modifier = Modifier.weight(1f),
                    onChoisir = { nom ->
                        lignes = lignes.map { if (it.id == ligne.id) it.copy(produit = nom) else it }
                    },
                )
                OutlinedTextField(
                    value = ligne.quantite,
                    onValueChange = { saisie ->
                        val chiffres = saisie.filter { it.isDigit() }.take(2)
                        lignes = lignes.map { if (it.id == ligne.id) it.copy(quantite = chiffres) else it }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(88.dp),
                )
                TextButton(onClick = {
                    if (lignes.size > 1) lignes = lignes.filterNot { it.id == ligne.id }
                }) {
                    Text("✕")
                }
            }
        }

        OutlinedButton(
            onClick = {
                lignes = lignes + Saisie(prochainId)
                prochainId++
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("+ Ajouter un produit")
        }

        if (erreur.isNotEmpty()) {
            Text(erreur, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (lignes.any { it.produit.isEmpty() }) {
                    erreur = "Choisissez un produit pour chaque ligne."
                    return@Button
                }
                if (lignes.any { (it.quantite.toIntOrNull() ?: 0) !in 1..50 }) {
                    erreur = "Chaque quantité doit être entre 1 et 50."
                    return@Button
                }
                erreur = ""
                onEnvoyer(table, lignes.map { LigneCommande(it.produit, it.quantite.toInt()) })
                lignes = listOf(Saisie(prochainId))
                prochainId++
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Envoyer en cuisine")
        }
    }
}

// Menu déroulant des produits visibles, regroupés par catégorie
@Composable
private fun SelecteurProduit(
    menu: Menu?,
    valeur: String,
    modifier: Modifier,
    onChoisir: (String) -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    val categories = menu?.categories.orEmpty().filter { it.visible }
    val produits = menu?.produits.orEmpty().filter { it.visible }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { ouvert = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(valeur.ifEmpty { "Choisir…" }, maxLines = 1)
        }

        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            categories.forEach { cat ->
                val liste = produits.filter { it.categorieId == cat.id }
                if (liste.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(cat.nom, fontWeight = FontWeight.Bold) },
                        onClick = {},
                        enabled = false,
                    )
                    liste.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.nom) },
                            onClick = {
                                onChoisir(p.nom)
                                ouvert = false
                            },
                        )
                    }
                }
            }
        }
    }
}
