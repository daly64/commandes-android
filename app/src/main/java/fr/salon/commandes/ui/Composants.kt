package fr.salon.commandes.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.util.Locale

// Prix en dinars tunisiens, avec trois décimales (millimes)
fun formaterPrix(prix: Double): String = String.format(Locale.FRANCE, "%.3f DT", prix)

// Temps écoulé depuis la création d'une commande
fun depuis(dateIso: String): String = try {
    val minutes = Duration.between(Instant.parse(dateIso), Instant.now()).toMinutes()
    when {
        minutes < 1 -> "à l'instant"
        minutes < 60 -> "il y a $minutes min"
        else -> "il y a ${minutes / 60} h ${minutes % 60} min"
    }
} catch (e: Exception) {
    ""
}

@Composable
fun TitreSection(texte: String) {
    Text(
        text = texte,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
fun CarteInfo(texte: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Text(
            text = texte,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
fun PastilleStatut(texte: String, couleur: Color, fond: Color) {
    Surface(shape = RoundedCornerShape(50), color = fond) {
        Text(
            text = texte,
            color = couleur,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
