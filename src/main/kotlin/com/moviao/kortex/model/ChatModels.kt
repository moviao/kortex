package com.moviao.kortex.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// ── Inbound Request ─────────────────────────────────────────

data class ChatRequest(
    val message: String,
    val model: String? = null,          // GGUF filename to switch to
    val systemPrompt: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val topK: Int? = null,
    val repeatPenalty: Double? = null,
    val conversationHistory: List<Message>? = null
)

data class Message(
    val role: String,       // "user", "assistant", "system"
    val content: String
)

// ── Outbound Response ───────────────────────────────────────

data class ChatResponse(
    val content: String,
    val model: String,
    val processingTimeMs: Long,
    val usage: UsageInfo? = null
)

data class UsageInfo(
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)

// ── OpenAI-compatible types (llama.cpp native format) ───────

data class OpenAIChatRequest(
    val model: String = "default",
    val messages: List<OpenAIMessage>,
    val temperature: Double? = null,
    val max_tokens: Int? = null,
    val top_p: Double? = null,
    val stream: Boolean = false,
    val repeat_penalty: Double? = null
)

data class OpenAIMessage(
    val role: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<OpenAIChoice> = emptyList(),
    val usage: OpenAIUsage? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIChoice(
    val index: Int = 0,
    val message: OpenAIMessage? = null,
    @JsonProperty("finish_reason")
    val finishReason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIUsage(
    @JsonProperty("prompt_tokens")
    val promptTokens: Int? = null,
    @JsonProperty("completion_tokens")
    val completionTokens: Int? = null,
    @JsonProperty("total_tokens")
    val totalTokens: Int? = null
)

// ── Streaming chunk (SSE) ───────────────────────────────────

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIStreamChunk(
    val id: String? = null,
    val choices: List<OpenAIStreamChoice> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIStreamChoice(
    val index: Int = 0,
    val delta: OpenAIDelta? = null,
    @JsonProperty("finish_reason")
    val finishReason: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIDelta(
    val role: String? = null,
    val content: String? = null
)