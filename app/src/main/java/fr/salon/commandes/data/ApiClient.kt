package fr.salon.commandes.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Erreur renvoyée par l'API GraphQL : code (extensions.code) et message lisible
class ErreurApi(val code: String, message: String) : Exception(message)

private val REQUETE_MENU = """
    query { menu { categories { id nom visible } produits { id nom prix categorieId visible } } }
"""

private val REQUETE_COMMANDES = """
    query { commandes { id table statut creeeLe lignes { produit quantite } } }
"""

private val MUTATION_CREER = """
    mutation(${'$'}table: Int!, ${'$'}lignes: [LigneCommandeInput!]!) {
      creerCommande(table: ${'$'}table, lignes: ${'$'}lignes) { id }
    }
"""

private val MUTATION_STATUT = """
    mutation(${'$'}id: ID!, ${'$'}statut: Statut!) {
      changerStatut(id: ${'$'}id, statut: ${'$'}statut) { id statut }
    }
"""

// Accès à l'API GraphQL. Fonctions bloquantes : à appeler hors du thread principal (Dispatchers.IO).
class ApiClient(private val urlBase: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val typeJson = "application/json".toMediaType()

    fun menu(): Menu {
        val donnees = executer(REQUETE_MENU)
        return json.decodeFromJsonElement(Menu.serializer(), donnees.getValue("menu"))
    }

    fun commandes(): List<Commande> {
        val donnees = executer(REQUETE_COMMANDES)
        return json.decodeFromJsonElement(ListSerializer(Commande.serializer()), donnees.getValue("commandes"))
    }

    fun creerCommande(table: Int, lignes: List<LigneCommande>) {
        val variables = buildJsonObject {
            put("table", table)
            put("lignes", json.encodeToJsonElement(ListSerializer(LigneCommande.serializer()), lignes))
        }
        executer(MUTATION_CREER, variables)
    }

    fun changerStatut(id: String, statut: Statut) {
        val variables = buildJsonObject {
            put("id", id)
            put("statut", statut.name)
        }
        executer(MUTATION_STATUT, variables)
    }

    // Envoie une opération GraphQL et renvoie « data », ou lève une ErreurApi
    private fun executer(requete: String, variables: JsonObject = JsonObject(emptyMap())): JsonObject {
        val corps = buildJsonObject {
            put("query", requete.trimIndent())
            put("variables", variables)
        }.toString().toRequestBody(typeJson)

        val requeteHttp = Request.Builder()
            .url(urlBase + "graphql")
            .post(corps)
            .build()

        http.newCall(requeteHttp).execute().use { reponse ->
            val texte = reponse.body?.string().orEmpty()
            val racine = try {
                json.parseToJsonElement(texte).jsonObject
            } catch (e: Exception) {
                throw ErreurApi("HTTP_${reponse.code}", "Réponse inattendue du serveur (${reponse.code})")
            }

            val erreurs = racine["errors"] as? JsonArray
            if (erreurs != null && erreurs.isNotEmpty()) {
                val premiere = erreurs[0].jsonObject
                val code = premiere["extensions"]?.jsonObject?.get("code")?.jsonPrimitive?.contentOrNull ?: "ERREUR"
                val message = premiere["message"]?.jsonPrimitive?.contentOrNull ?: "Erreur"
                throw ErreurApi(code, message)
            }

            return racine["data"] as? JsonObject ?: throw ErreurApi("ERREUR", "Réponse vide du serveur")
        }
    }
}