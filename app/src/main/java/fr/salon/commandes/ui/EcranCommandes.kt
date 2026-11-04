@file:OptIn(ExperimentalMaterial3Api::class)

package fr.salon.commandes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.salon.commandes.Etat
import fr.salon.commandes.data.Commande
import fr.salon.commandes.data.Statut
import fr.salon.commandes.ui.theme.CouleurRetard
import fr.salon.commandes.ui.theme.couleursStatut
import java.time.Duration
import java.time.Instant

// Au-delà de ce délai, une commande qui n'est pas encore prête est signalée en retard
private const val SEUIL_RETARD_MINUTES = 15L

// Filtres par statut en haut, puis les commandes correspondantes, des plus anciennes aux plus récentes
@Composable
fun EcranCommandes(etat: Etat, onAvancer: (Commande) -> Unit) {
    // Nom du statut filtré, ou null pour afficher toutes les commandes
    var filtre by rememberSaveable { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "toutes") {
                FilterChip(
                    selected = filtre == null,
                    onClick = { filtre = null },
                    label = { Text("Toutes · ${etat.commandes.size}") },
                )
            }
            items(items = Statut.entries.filter { it != Statut.PAYEE }, key = { it.name }) { statut ->
                val nombre = etat.commandes.count { it.statut == statut.name }
                FilterChip(
                    selected = filtre == statut.name,
                    onClick = { filtre = statut.name },
                    label = { Text("${statut.libelle} · $nombre") },
                )
            }
        }

        val visibles = etat.commandes.filter { filtre == null || it.statut == filtre }
        if (visibles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CarteInfo(
                    texte = if (etat.commandes.isEmpty()) "Aucune commande en cours." else "Aucune commande à cette étape.",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = visibles, key = { commande -> commande.id }) { commande ->
                    CarteCommande(
                        commande = commande,
                        enCours = commande.id in etat.enCours,
                        onAvancer = { onAvancer(commande) },
                    )
                }
            }
        }
    }
}

// Carte d'une commande : bande de couleur selon le statut (rouge si en retard), table, ancienneté, produits, étape suivante
@Composable
private fun CarteCommande(commande: Commande, enCours: Boolean, onAvancer: () -> Unit) {
    val statut = Statut.de(commande.statut)
    val couleurs = couleursStatut(statut)
    val action = statut?.action
    val minutes = minutesDepuis(commande.creeeLe)
    val enRetard = statut != Statut.PRETE && statut != Statut.SERVIE && minutes >= SEUIL_RETARD_MINUTES
    val couleurBande = if (enRetard) CouleurRetard else couleurs.principale

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(couleurBande),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Table ${commande.table}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    PastilleStatut(
                        texte = statut?.libelle ?: commande.statut,
                        couleur = couleurs.principale,
                        fond = couleurs.fond,
                    )
                }
                val anciennete = depuis(commande.creeeLe)
                if (anciennete.isNotEmpty()) {
                    Text(
                        text = if (enRetard) "$anciennete · en retard" else anciennete,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enRetard) CouleurRetard else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (enRetard) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                for (ligne in commande.lignes) {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "${ligne.quantite} ×",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(44.dp),
                        )
                        Text(text = ligne.produit.trim())
                    }
                }

                if (action != null) {
                    Button(
                        onClick = onAvancer,
                        enabled = !enCours,
                        colors = ButtonDefaults.buttonColors(containerColor = couleurs.principale),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(48.dp),
                    ) {
                        Text(text = if (enCours) "…" else action)
                    }
                }
            }
        }
    }
}

// Minutes écoulées depuis la création de la commande
private fun minutesDepuis(dateIso: String): Long = try {
    Duration.between(Instant.parse(dateIso), Instant.now()).toMinutes()
} catch (e: Exception) {
    0L
}
