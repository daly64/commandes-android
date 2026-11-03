package fr.salon.commandes.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Abonnement temps réel en SSE : POST sur <API_URL>graphql avec « Accept: text/event-stream ».
// Le serveur envoie un bloc « event: next » suivi de « data: … » à chaque commande créée ou modifiée.
// Le flux se ferme après la durée maximale de la fonction Vercel : l'appelant se reconnecte.
class FluxCommandes(private val urlBase: String) {

    private val json = Json { ignoreUnknownKeys = true }

    // Le flux reste ouvert longtemps : un silence de plus de 2 minutes est traité comme une coupure
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .build()

    private val typeJson = "application/json".toMediaType()

    @Volatile
    private var appel: Call? = null

    /**
     * Ouvre le flux et le lit jusqu'à sa fermeture. Bloquant : à lancer hors du thread principal.
     * [surOuverture] : le flux est actif (c'est le moment de relire la liste).
     * [surCommande] : une commande a été créée ou modifiée.
     * Lève une IOException en cas de refus ou de coupure.
     */
    fun ecouter(surOuverture: () -> Unit, surCommande: (Commande) -> Unit) {
        val corps = buildJsonObject { put("query", ABONNEMENT) }.toString().toRequestBody(typeJson)
        val requete = Request.Builder()
            .url(urlBase + "graphql")
            .header("Accept", "text/event-stream")
            .post(corps)
            .build()

        val nouvelAppel = http.newCall(requete)
        appel = nouvelAppel
        nouvelAppel.execute().use { reponse ->
            if (!reponse.isSuccessful) throw IOException("Flux refusé (${reponse.code})")
            surOuverture()

            val source = reponse.body?.source() ?: throw IOException("Flux vide")
            val bloc = StringBuilder()
            while (true) {
                val ligne = source.readUtf8Line() ?: break
                if (ligne.isEmpty()) {
                    lireCommande(bloc.toString())?.let(surCommande)
                    bloc.clear()
                } else {
                    bloc.append(ligne).append('\n')
                }
            }
        }
    }

    fun arreter() {
        appel?.cancel()
        appel = null
    }

    // Lit un bloc SSE ; renvoie la commande s'il s'agit d'un événement « next »
    private fun lireCommande(bloc: String): Commande? {
        var evenement = "message"
        val donnees = StringBuilder()
        for (ligne in bloc.lines()) {
            when {
                ligne.startsWith("event:") -> evenement = ligne.removePrefix("event:").trim()
                ligne.startsWith("data:") -> donnees.append(ligne.removePrefix("data:").trim())
            }
        }
        if (evenement != "next" || donnees.isEmpty()) return null
        return try {
            val commande = json.parseToJsonElement(donnees.toString())
                .jsonObject["data"]?.jsonObject?.get("commandeModifiee")
            commande?.let { json.decodeFromJsonElement(Commande.serializer(), it) }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val ABONNEMENT = "subscription { commandeModifiee { id table statut creeeLe lignes { produit quantite } } }"
    }
}