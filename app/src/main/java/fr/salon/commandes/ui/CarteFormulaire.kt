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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.salon.commandes.data.LigneCommande
import fr.salon.commandes.data.Menu

private const val NOMBRE_TABLES = 10

// Une ligne en cours de saisie : la quantité reste du texte jusqu'à l'envoi
private data class Saisie(
    val id: Int,
    val produit: String = "",
    val quantite: String = "1",
)

@Composable
fun CarteFormulaire(menu: Menu?, onEnvoyer: (Int, List<LigneCommande>) -> Unit) {
    var table by remember { mutableIntStateOf(1) }
    var prochainId by remember { mutableIntStateOf(2) }
    var lignes by remember { mutableStateOf(listOf(Saisie(id = 1))) }
    var erreur by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Table", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (n in 1..NOMBRE_TABLES) {
                if (table == n) {
                    Button(onClick = { table = n }) { Text(text = "$n") }
                } else {
                    OutlinedButton(onClick = { table = n }) { Text(text = "$n") }
                }
            }
        }

        Text(text = "Produits", style = MaterialTheme.typography.labelLarge)
        for (ligne in lignes) {
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
                        val chiffres = saisie.filter { c -> c.isDigit() }.take(2)
                        lignes = lignes.map { if (it.id == ligne.id) it.copy(quantite = chiffres) else it }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(80.dp),
                )
                TextButton(
                    onClick = {
                        if (lignes.size > 1) {
                            lignes = lignes.filter { it.id != ligne.id }
                        }
                    },
                ) {
                    Text(text = "✕")
                }
            }
        }

        OutlinedButton(
            onClick = {
                lignes = lignes + Saisie(id = prochainId)
                prochainId += 1
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(text = "+ Ajouter un produit")
        }

        if (erreur.isNotEmpty()) {
            Text(text = erreur, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                val quantitesValides = lignes.all { (it.quantite.toIntOrNull() ?: 0) in 1..50 }
                when {
                    lignes.any { it.produit.isEmpty() } ->
                        erreur = "Choisissez un produit pour chaque ligne."
                    !quantitesValides ->
                        erreur = "Chaque quantité doit être entre 1 et 50."
                    else -> {
                        erreur = ""
                        onEnvoyer(table, lignes.map { LigneCommande(it.produit, it.quantite.toInt()) })
                        lignes = listOf(Saisie(id = prochainId))
                        prochainId += 1
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(text = "Envoyer en cuisine")
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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text(text = valeur.ifEmpty { "Choisir…" }, maxLines = 1)
        }

        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            for (categorie in categories) {
                val liste = produits.filter { it.categorieId == categorie.id }
                if (liste.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text(text = categorie.nom, fontWeight = FontWeight.Bold) },
                        onClick = {},
                        enabled = false,
                    )
                    for (produit in liste) {
                        DropdownMenuItem(
                            text = { Text(text = produit.nom) },
                            onClick = {
                                onChoisir(produit.nom)
                                ouvert = false
                            },
                        )
                    }
                }
            }
        }
    }
}
