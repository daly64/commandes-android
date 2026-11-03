package fr.salon.commandes.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

// Abonnement en temps réel à l'API (GraphQL en SSE) : prévient à chaque commande créée ou modifiée.
// Le serveur ferme le flux au bout de quelques minutes ; c'est l'appelant qui se reconnecte.
class FluxCommandes(private val urlBase: String, private val cle: () -> String) {

    // Le serveur envoie un signal de vie régulièrement : 60 s sans données signifie une connexion perdue
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var source: EventSource? = null

    /**
     * Ouvre le flux.
     * [surEvenement] : une commande a changé.
     * [surEtat] : true quand le flux est ouvert, false quand il est fermé ou en échec.
     */
    fun demarrer(surEvenement: () -> Unit, surEtat: (Boolean) -> Unit) {
        arreter()

        val corps = """{"query":"subscription { commandeModifiee { id statut } }"}"""
            .toRequestBody("application/json".toMediaType())

        val requete = Request.Builder()
            .url(urlBase + "graphql")
            .header("X-Api-Key", cle())
            .header("Accept", "text/event-stream")
            .post(corps)
            .build()

        source = EventSources.createFactory(http).newEventSource(
            requete,
            object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    surEtat(true)
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    // « next » porte un événement ; « complete » annonce la fin du flux
                    if (type == null || type == "next") {
                        if (data.contains("commandeModifiee")) surEvenement()
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    surEtat(false)
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    surEtat(false)
                }
            },
        )
    }

    fun arreter() {
        source?.cancel()
        source = null
    }
}
