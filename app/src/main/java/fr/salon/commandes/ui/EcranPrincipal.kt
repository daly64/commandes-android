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

// Libellés et étapes suivantes, identiques à ceux de l'interface web
val LIBELLES = mapOf(
    "en_attente" to "En attente",
    "en_preparation" to "En préparation",
    "prete" to "Prête",
    "servie" to "Servie",
    "payee" to "Payée",
)

val SUIVANT = mapOf(
    "en_attente" to "en_preparation",
    "en_preparation" to "prete",
    "prete" to "servie",
    "servie" to "payee",
)

val ACTION = mapOf(
    "en_attente" to "Démarrer",
    "en_preparation" to "Marquer prête",
    "prete" to "Marquer servie",
    "servie" to "Marquer payée",
)

@Composable
fun EcranPrincipal(vm: AppViewModel) {
    val etat by vm.etat.collectAsState()

    if (!etat.cleValide) {
        EcranCle(alerte = etat.alerte, onValider = vm::valider)
        return
    }

    val message = etat.alerte.ifEmpty { etat.connexion }

    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Prise de commande", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Nouvelle commande", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    CarteFormulaire(menu = etat.menu) { table, lignes ->
                        vm.creerCommande(table, lignes) {}
                    }
                }
            }
        }

        if (message.isNotEmpty()) {
            item {
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Text("Commandes en cours", style = MaterialTheme.typography.titleMedium)
        }

        if (etat.commandes.isEmpty()) {
            item { Text("Aucune commande en cours.") }
        }

        items(etat.commandes, key = { it.id }) { commande ->
            CarteCommande(commande) { vm.changerStatut(commande.id, SUIVANT.getValue(commande.statut)) }
        }
    }
}

@Composable
fun CarteCommande(commande: Commande, onAvancer: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Table ${commande.table} · ${LIBELLES[commande.statut] ?: commande.statut}",
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            commande.lignes.forEach { ligne ->
                Text("${ligne.quantite} × ${ligne.produit}")
            }
            if (SUIVANT.containsKey(commande.statut)) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onAvancer,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(ACTION[commande.statut] ?: "Suivant")
                }
            }
        }
    }
}
