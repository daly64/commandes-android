package fr.salon.commandes.data

import fr.salon.commandes.BuildConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Erreur renvoyée par l'API, avec son code HTTP et un message lisible
class ErreurApi(val code: Int, message: String) : Exception(message)

// Toutes les appels réseau de l'application passent par cette classe.
// La clé d'accès est lue à chaque appel, pour qu'une nouvelle clé soit prise en compte aussitôt.
class ApiClient(private val cle: () -> String) {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val base = BuildConfig.API_URL
    private val typeJson = "application/json".toMediaType()

    fun menu(): Menu = lire(Request.Builder().url("${base}api/menu").get())

    fun commandes(): List<Commande> =
        lire(avecCle(Request.Builder().url("${base}api/commandes").get()))

    fun creerCommande(nouvelle: NouvelleCommande): Commande =
        lire(avecCle(Request.Builder().url("${base}api/commandes").post(corps(json.encodeToString(nouvelle)))))

    fun changerStatut(id: Int, statut: String): Commande =
        lire(
            avecCle(
                Request.Builder()
                    .url("${base}api/commandes/$id/statut")
                    .patch(corps(json.encodeToString(ChangementStatut(statut)))),
            ),
        )

    private fun corps(texte: String): RequestBody = texte.toRequestBody(typeJson)

    private fun avecCle(requete: Request.Builder): Request.Builder = requete.header("X-Api-Key", cle())

    // Exécute la requête, puis décode la réponse ou lève une ErreurApi
    private inline fun <reified T> lire(requete: Request.Builder): T {
        http.newCall(requete.build()).execute().use { reponse ->
            val texte = reponse.body?.string().orEmpty()
            if (!reponse.isSuccessful) {
                throw ErreurApi(reponse.code, messageErreur(texte, reponse.code))
            }
            return json.decodeFromString<T>(texte)
        }
    }

    private fun messageErreur(texte: String, code: Int): String = try {
        val objet = json.parseToJsonElement(texte).jsonObject
        val liste = objet["erreurs"] as? JsonArray
        liste?.joinToString(" ; ") { it.jsonPrimitive.content }
            ?: objet["erreur"]?.jsonPrimitive?.content
            ?: "Erreur $code"
    } catch (e: Exception) {
        "Erreur $code"
    }
}
