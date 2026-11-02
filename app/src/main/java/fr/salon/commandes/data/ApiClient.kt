package fr.salon.commandes.data

import fr.salon.commandes.BuildConfig
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Erreur renvoyée par l'API : code HTTP et message lisible
class ErreurApi(val code: Int, message: String) : Exception(message)

// Tous les appels réseau passent par cette classe. Ces fonctions sont bloquantes :
// elles doivent être appelées hors du thread principal (Dispatchers.IO).
class ApiClient(private val cle: () -> String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val base = BuildConfig.API_URL
    private val typeJson = "application/json".toMediaType()

    fun menu(): Menu {
        val texte = executer(requete("api/menu", avecCle = false))
        return json.decodeFromString(Menu.serializer(), texte)
    }

    fun commandes(): List<Commande> {
        val texte = executer(requete("api/commandes"))
        return json.decodeFromString(ListSerializer(Commande.serializer()), texte)
    }

    fun creerCommande(nouvelle: NouvelleCommande): Commande {
        val corps = json.encodeToString(NouvelleCommande.serializer(), nouvelle).toRequestBody(typeJson)
        val texte = executer(requete("api/commandes").post(corps))
        return json.decodeFromString(Commande.serializer(), texte)
    }

    fun changerStatut(id: Int, statut: String): Commande {
        val corps = json.encodeToString(ChangementStatut.serializer(), ChangementStatut(statut))
            .toRequestBody(typeJson)
        val texte = executer(requete("api/commandes/$id/statut").patch(corps))
        return json.decodeFromString(Commande.serializer(), texte)
    }

    private fun requete(chemin: String, avecCle: Boolean = true): Request.Builder {
        val builder = Request.Builder().url(base + chemin)
        if (avecCle) {
            builder.header("X-Api-Key", cle())
        }
        return builder
    }

    // Exécute la requête et renvoie le corps de la réponse, ou lève une ErreurApi
    private fun executer(builder: Request.Builder): String {
        http.newCall(builder.build()).execute().use { reponse ->
            val texte = reponse.body?.string().orEmpty()
            if (!reponse.isSuccessful) {
                throw ErreurApi(reponse.code, messageErreur(texte, reponse.code))
            }
            return texte
        }
    }

    // L'API renvoie { "erreur": "..." } ou { "erreurs": ["...", "..."] }
    private fun messageErreur(texte: String, code: Int): String {
        return try {
            val objet = json.parseToJsonElement(texte).jsonObject
            val liste = objet["erreurs"] as? JsonArray
            liste?.joinToString(" ; ") { it.jsonPrimitive.content }
                ?: objet["erreur"]?.jsonPrimitive?.content
                ?: "Erreur $code"
        } catch (e: Exception) {
            "Erreur $code"
        }
    }
}
