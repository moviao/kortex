package com.moviao.kortex.resource

import com.moviao.kortex.model.ChatRequest
import com.moviao.kortex.service.LlamaCppService
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import org.jboss.logging.Logger

@Path("/api")
@Tag(name = "Chat", description = "LLM Chat endpoints")
class ChatResource {

    @Inject
    lateinit var llamaService: LlamaCppService

    companion object {
        private val LOG = Logger.getLogger(ChatResource::class.java)
    }

    @POST
    @Path("/chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Chat completion", description = "Send a message and get a complete response")
    fun chat(request: ChatRequest): Response {
        LOG.debug("Chat request — message length: ${request.message.length}")

        return try {
            val response = llamaService.chat(request)
            Response.ok(response).build()
        } catch (e: Exception) {
            LOG.error("Chat error", e)
            Response.serverError()
                .entity(mapOf("error" to (e.message ?: "Unknown error")))
                .build()
        }
    }

    @POST
    @Path("/chat/stream")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(summary = "Streaming chat", description = "Send a message and receive streaming SSE response")
    fun chatStream(request: ChatRequest): Response {
        LOG.debug("Stream chat request")

        return try {
            val sseBody = llamaService.chatStream(request)
            Response.ok(sseBody, MediaType.SERVER_SENT_EVENTS).build()
        } catch (e: Exception) {
            LOG.error("Stream error", e)
            Response.serverError()
                .entity(mapOf("error" to (e.message ?: "Unknown error")))
                .build()
        }
    }

    @POST
    @Path("/v1/chat/completions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "OpenAI-compatible endpoint", description = "Proxies directly to llama.cpp's OpenAI-compatible API")
    fun openAiProxy(body: String): Response {
        // Direct passthrough to llama.cpp — full OpenAI compatibility
        return try {
            val httpClient = java.net.http.HttpClient.newHttpClient()
            val httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("${llamaService.config.baseUrl()}/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build()
            val httpResponse = httpClient.send(httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
            Response.status(httpResponse.statusCode())
                .entity(httpResponse.body())
                .type(MediaType.APPLICATION_JSON)
                .build()
        } catch (e: Exception) {
            Response.serverError()
                .entity(mapOf("error" to e.message))
                .build()
        }
    }
}