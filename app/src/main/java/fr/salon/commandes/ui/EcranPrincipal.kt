package fr.salon.commandes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.salon.commandes.AppViewModel
import fr.salon.commandes.data.Commande

// Libellés et étapes, identiques à ceux de l'interface web
private val LIBELLES = mapOf(
    "en_attente" to "En attente",
    "en_preparation" to "En préparation",
    "prete" to "Prête",
    "servie" to "Servie",
    "payee" to "Payée",
)

private val SUIVANT = mapOf(
    "en_attente" to "en_preparation",
    "en_preparation" to "prete",
    "prete" to "servie",
    "servie" to "payee",
)

private val ACTION = mapOf(
    "en_attente" to "Démarrer",
    "en_preparation" to "Marquer prête",
    "prete" to "Marquer servie",
    "servie" to "Marquer payée",
)

@Composable
fun EcranPrincipal(vm: AppViewModel) {
    val etat by vm.etat.collectAsState()

    if (!etat.cleValide) {
        EcranCle(alerte = etat.alerte, onValider = { cle -> vm.valider(cle) })
        return
    }

    val message = etat.alerte.ifEmpty { etat.connexion }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(text = "Prise de commande", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Nouvelle commande", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    CarteFormulaire(
                        menu = etat.menu,
                        onEnvoyer = { table, lignes -> vm.creerCommande(table, lignes) },
                    )
                }
            }
        }

        if (message.isNotEmpty()) {
            item {
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Text(text = "Commandes en cours", style = MaterialTheme.typography.titleMedium)
        }

        if (etat.commandes.isEmpty()) {
            item {
                Text(text = "Aucune commande en cours.")
            }
        }

        items(items = etat.commandes, key = { it.id }) { commande ->
            CarteCommande(
                commande = commande,
                onAvancer = { statut -> vm.changerStatut(commande.id, statut) },
            )
        }
    }
}

@Composable
private fun CarteCommande(commande: Commande, onAvancer: (String) -> Unit) {
    val suivant = SUIVANT[commande.statut]

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Table ${commande.table} · ${LIBELLES[commande.statut] ?: commande.statut}",
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            for (ligne in commande.lignes) {
                Text(text = "${ligne.quantite} × ${ligne.produit}")
            }
            if (suivant != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onAvancer(suivant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(text = ACTION[commande.statut] ?: "Suivant")
                }
            }
        }
    }
}
