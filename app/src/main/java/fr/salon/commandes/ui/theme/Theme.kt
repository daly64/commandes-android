package fr.salon.commandes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import fr.salon.commandes.data.Statut

// Palette sobre : vert thé pour les actions, tons crème pour le fond
private val Couleurs = lightColorScheme(
    primary = Color(0xFF3B6E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E8DC),
    onPrimaryContainer = Color(0xFF0F2A1A),
    secondary = Color(0xFF6B5B4B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E4D3),
    onSecondaryContainer = Color(0xFF2A1F14),
    background = Color(0xFFF6F5F1),
    onBackground = Color(0xFF1C1C1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1A),
    surfaceVariant = Color(0xFFECEAE3),
    onSurfaceVariant = Color(0xFF5C5A55),
    error = Color(0xFFB3261E),
)

@Composable
fun ThemeCommandes(contenu: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Couleurs, content = contenu)
}

// Couleur principale et couleur de fond de chaque statut
data class CouleursStatut(val principale: Color, val fond: Color)

fun couleursStatut(statut: Statut?): CouleursStatut = when (statut) {
    Statut.EN_ATTENTE -> CouleursStatut(Color(0xFFB26A00), Color(0xFFFFEFD2))
    Statut.EN_PREPARATION -> CouleursStatut(Color(0xFF1E5FBF), Color(0xFFDCE9FF))
    Statut.PRETE -> CouleursStatut(Color(0xFF2E7D32), Color(0xFFDDF2DE))
    Statut.SERVIE -> CouleursStatut(Color(0xFF5F6368), Color(0xFFE8EAED))
    Statut.PAYEE, null -> CouleursStatut(Color(0xFF5F6368), Color(0xFFE8EAED))
}
