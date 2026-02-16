package com.moviao.kortex.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// ── Model file discovery ────────────────────────────────────

data class ModelListResponse(
    val models: List<ModelInfo>,
    val activeModel: String,
    val llamaCppStatus: String
)

data class ModelInfo(
    val filename: String,
    val sizeBytes: Long,
    val sizeHuman: String
)

data class SwitchModelRequest(
    val model: String       // GGUF filename
)

data class SwitchModelResponse(
    val previousModel: String,
    val newModel: String,
    val status: String,
    val message: String
)

// ── llama.cpp /health response ──────────────────────────────

@JsonIgnoreProperties(ignoreUnknown = true)
data class LlamaCppHealth(
    val status: String? = null,
    val slots_idle: Int? = null,
    val slots_processing: Int? = null
)

// ── llama.cpp /props response ───────────────────────────────

@JsonIgnoreProperties(ignoreUnknown = true)
data class LlamaCppProps(
    val total_slots: Int? = null,
    val chat_template: String? = null,
    val default_generation_settings: DefaultGenSettings? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DefaultGenSettings(
    val model: String? = null,
    val n_ctx: Int? = null,
    val n_predict: Int? = null
)