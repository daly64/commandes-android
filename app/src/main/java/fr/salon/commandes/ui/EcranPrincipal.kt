@file:OptIn(ExperimentalMaterial3Api::class)

package fr.salon.commandes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.salon.commandes.AppViewModel

@Composable
fun EcranPrincipal(vm: AppViewModel) {
    val etat by vm.etat.collectAsState()

    var onglet by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    // Messages ponctuels (commande envoyée, erreur…) affichés en bas de l'écran
    LaunchedEffect(etat.message) {
        val message = etat.message
        if (message != null) {
            snackbar.showSnackbar(message)
            vm.messageVu()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Commandes")
                        val sousTitre = when {
                            etat.connexion.isNotEmpty() -> etat.connexion
                            etat.tempsReel -> "● Temps réel actif"
                            else -> "Connexion au temps réel…"
                        }
                        Text(text = sousTitre, style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = onglet == 0,
                    onClick = { onglet = 0 },
                    icon = { Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = null) },
                    label = { Text("Nouvelle commande") },
                )
                NavigationBarItem(
                    selected = onglet == 1,
                    onClick = { onglet = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (etat.commandes.isNotEmpty()) {
                                    Badge { Text(text = "${etat.commandes.size}") }
                                }
                            },
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = null)
                        }
                    },
                    label = { Text("En cours") },
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
    ) { marges ->
        Box(
            modifier = Modifier
                .padding(marges)
                .fillMaxSize(),
        ) {
            if (onglet == 0) {
                EcranNouvelleCommande(etat = etat, vm = vm)
            } else {
                EcranCommandes(etat = etat, onAvancer = { commande -> vm.avancer(commande) })
            }
        }
    }
}