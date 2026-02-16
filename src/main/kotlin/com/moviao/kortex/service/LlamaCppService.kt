package com.moviao.kortex.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.moviao.kortex.config.LlamaCppConfig
import com.moviao.kortex.model.*
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@ApplicationScoped
class LlamaCppService {

    @Inject
    lateinit var config: LlamaCppConfig

    @Inject
    lateinit var objectMapper: ObjectMapper

    private lateinit var httpClient: HttpClient

    companion object {
        private val LOG = Logger.getLogger(LlamaCppService::class.java)
    }

    @PostConstruct
    fun init() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        LOG.info("LlamaCppService initialized — endpoint: ${config.baseUrl()}, models dir: ${config.modelsDir()}")
    }

    // ── Chat Completion ─────────────────────────────────────

    fun chat(request: ChatRequest): ChatResponse {
        val startTime = System.currentTimeMillis()

        val messages = buildMessages(request)

        val openAiRequest = OpenAIChatRequest(
            messages = messages,
            temperature = request.temperature,
            max_tokens = request.maxTokens,
            top_p = request.topP,
            stream = false,
            repeat_penalty = request.repeatPenalty
        )

        val body = objectMapper.writeValueAsString(openAiRequest)

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("${config.baseUrl()}/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(config.timeout().toLong()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        val elapsed = System.currentTimeMillis() - startTime

        if (httpResponse.statusCode() != 200) {
            throw RuntimeException("llama.cpp returned ${httpResponse.statusCode()}: ${httpResponse.body()}")
        }

        val openAiResp = objectMapper.readValue(httpResponse.body(), OpenAIChatResponse::class.java)
        val content = openAiResp.choices.firstOrNull()?.message?.content ?: ""
        val activeModel = getActiveModelName()

        return ChatResponse(
            content = content,
            model = activeModel,
            processingTimeMs = elapsed,
            usage = openAiResp.usage?.let {
                UsageInfo(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens
                )
            }
        )
    }

    // ── Streaming Chat (returns raw SSE string) ─────────────

    fun chatStream(request: ChatRequest): String {
        val messages = buildMessages(request)

        val openAiRequest = OpenAIChatRequest(
            messages = messages,
            temperature = request.temperature,
            max_tokens = request.maxTokens,
            top_p = request.topP,
            stream = true,
            repeat_penalty = request.repeatPenalty
        )

        val body = objectMapper.writeValueAsString(openAiRequest)

        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("${config.baseUrl()}/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(config.timeout().toLong()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())

        if (httpResponse.statusCode() != 200) {
            throw RuntimeException("llama.cpp stream error ${httpResponse.statusCode()}: ${httpResponse.body()}")
        }

        return httpResponse.body()
    }

    // ── Model Discovery ─────────────────────────────────────

    fun listLocalModels(): List<ModelInfo> {
        val modelsDir = File(config.modelsDir())
        if (!modelsDir.exists() || !modelsDir.isDirectory) {
            return emptyList()
        }

        return modelsDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".gguf") }
            ?.sortedBy { it.name }
            ?.map { file ->
                ModelInfo(
                    filename = file.name,
                    sizeBytes = file.length(),
                    sizeHuman = formatSize(file.length())
                )
            } ?: emptyList()
    }

    fun getActiveModelName(): String {
        return try {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("${config.baseUrl()}/props"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            val props = objectMapper.readValue(response.body(), LlamaCppProps::class.java)
            props.default_generation_settings?.model?.substringAfterLast("/") ?: "unknown"
        } catch (e: Exception) {
            LOG.warn("Could not fetch active model name: ${e.message}")
            "unknown"
        }
    }

    // ── Health ───────────────────────────────────────────────

    fun healthCheck(): LlamaCppHealth {
        return try {
            val httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("${config.baseUrl()}/health"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()
            val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
            objectMapper.readValue(response.body(), LlamaCppHealth::class.java)
        } catch (e: Exception) {
            LlamaCppHealth(status = "error: ${e.message}")
        }
    }

    fun isHealthy(): Boolean {
        return try {
            healthCheck().status == "ok"
        } catch (e: Exception) {
            false
        }
    }

    // ── Helpers ──────────────────────────────────────────────

    private fun buildMessages(request: ChatRequest): List<OpenAIMessage> {
        val messages = mutableListOf<OpenAIMessage>()

        // System prompt
        val systemPrompt = request.systemPrompt ?: "You are a helpful, concise assistant."
        messages.add(OpenAIMessage(role = "system", content = systemPrompt))

        // Conversation history
        request.conversationHistory?.forEach { msg ->
            messages.add(OpenAIMessage(role = msg.role, content = msg.content))
        }

        // Current user message
        messages.add(OpenAIMessage(role = "user", content = request.message))

        return messages
    }

    private fun formatSize(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        val mb = bytes / (1024.0 * 1024.0)
        return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(mb)
    }
}