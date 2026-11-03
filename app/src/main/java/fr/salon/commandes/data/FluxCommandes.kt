package fr.salon.commandes.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

// Abonnement en temps réel par WebSocket natif Vercel (adresse <API_URL>api/ws, protocole graphql-transport-ws).
// Le serveur ferme la connexion au bout de quelques minutes ; c'est l'appelant qui se reconnecte.
class FluxCommandes(private val urlBase: String, private val cle: () -> String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null

    /**
     * Ouvre la connexion et s'abonne à commandeModifiee.
     * [surEvenement] : une commande a changé.
     * [surEtat] : true quand l'abonnement est actif, false quand la connexion est fermée ou en échec.
     */
    fun demarrer(surEvenement: () -> Unit, surEtat: (Boolean) -> Unit) {
        arreter()

        val requete = Request.Builder()
            .url(urlBase + "api/ws")
            .header("Sec-WebSocket-Protocol", "graphql-transport-ws")
            .build()

        socket = http.newWebSocket(
            requete,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    // La clé passe dans le message d'ouverture du protocole
                    val ouverture = buildJsonObject {
                        put("type", "connection_init")
                        put("payload", buildJsonObject { put("apiKey", cle()) })
                    }
                    webSocket.send(ouverture.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val message = try {
                        json.parseToJsonElement(text).jsonObject
                    } catch (e: Exception) {
                        return
                    }
                    when (message["type"]?.jsonPrimitive?.contentOrNull) {
                        "connection_ack" -> {
                            webSocket.send(ABONNEMENT)
                            surEtat(true)
                        }
                        "next" -> surEvenement()
                        "ping" -> webSocket.send(PONG)
                        "error", "complete" -> webSocket.close(1000, null)
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    surEtat(false)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    surEtat(false)
                }
            },
        )
    }

    fun arreter() {
        socket?.close(1000, "Arrêt")
        socket = null
    }

    private companion object {
        const val ABONNEMENT =
            """{"id":"1","type":"subscribe","payload":{"query":"subscription { commandeModifiee { id statut } }"}}"""
        const val PONG = """{"type":"pong"}"""
    }
}
