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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.salon.commandes.Etat
import fr.salon.commandes.data.Commande
import fr.salon.commandes.data.Statut
import fr.salon.commandes.ui.theme.couleursStatut

@Composable
fun EcranCommandes(etat: Etat, onAvancer: (Commande) -> Unit) {
    if (etat.commandes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CarteInfo(texte = "Aucune commande en cours.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = etat.commandes, key = { commande -> commande.id }) { commande ->
            CarteCommande(
                commande = commande,
                enCours = commande.id in etat.enCours,
                onAvancer = { onAvancer(commande) },
            )
        }
    }
}

// Carte d'une commande : bande de couleur selon le statut, table, ancienneté, produits, étape suivante
@Composable
private fun CarteCommande(commande: Commande, enCours: Boolean, onAvancer: () -> Unit) {
    val statut = Statut.de(commande.statut)
    val couleurs = couleursStatut(statut)
    val action = statut?.action

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(couleurs.principale),
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
                        text = anciennete,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
