package fr.salon.commandes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.salon.commandes.data.Statut

// Palette « matcha et cuivre » : vert matcha profond pour les actions, cuivre pour les accents, crème pour le fond
private val Couleurs = lightColorScheme(
    primary = Color(0xFF2E6B5E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCDEBE2),
    onPrimaryContainer = Color(0xFF05201A),
    secondary = Color(0xFFB8733A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF7E3CF),
    onSecondaryContainer = Color(0xFF3A2108),
    background = Color(0xFFF5F3EE),
    onBackground = Color(0xFF1B1D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1D1A),
    surfaceVariant = Color(0xFFE8E6DF),
    onSurfaceVariant = Color(0xFF55524B),
    error = Color(0xFFB3261E),
)

// Couleur des commandes en retard
val CouleurRetard = Color(0xFFB3261E)

@Composable
fun ThemeCommandes(contenu: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Couleurs, content = contenu)
}

// Couleur principale et couleur de fond de chaque statut
data class CouleursStatut(val principale: Color, val fond: Color)

fun couleursStatut(statut: Statut?): CouleursStatut = when (statut) {
    Statut.EN_ATTENTE -> CouleursStatut(Color(0xFFB36B00), Color(0xFFFFF1D6))
    Statut.EN_PREPARATION -> CouleursStatut(Color(0xFF2563A8), Color(0xFFDDEAFA))
    Statut.PRETE -> CouleursStatut(Color(0xFF2E7D4F), Color(0xFFD9F2E3))
    Statut.SERVIE -> CouleursStatut(Color(0xFF4F5B66), Color(0xFFE4E8EC))
    Statut.PAYEE, null -> CouleursStatut(Color(0xFF4F5B66), Color(0xFFE4E8EC))
}
