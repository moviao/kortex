package com.moviao.kortex.resource

import com.moviao.kortex.model.ModelListResponse
import com.moviao.kortex.model.SwitchModelRequest
import com.moviao.kortex.model.SwitchModelResponse
import com.moviao.kortex.service.LlamaCppService
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

@Path("/api/models")
@Tag(name = "Models", description = "Model management")
class ModelResource {

    @Inject
    lateinit var llamaService: LlamaCppService

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List available GGUF models", description = "Shows all .gguf files in the models directory and the currently active model")
    fun listModels(): Response {
        return try {
            val models = llamaService.listLocalModels()
            val activeModel = llamaService.getActiveModelName()
            val health = llamaService.healthCheck()

            Response.ok(
                ModelListResponse(
                    models = models,
                    activeModel = activeModel,
                    llamaCppStatus = health.status ?: "unknown"
                )
            ).build()
        } catch (e: Exception) {
            Response.serverError()
                .entity(mapOf("error" to "Failed to list models: ${e.message}"))
                .build()
        }
    }

    @POST
    @Path("/switch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Switch the active model",
        description = """
            To switch models, you need to restart the llama-cpp container with a different model file.
            This endpoint provides instructions for doing so.
            For zero-downtime switching, run multiple llama.cpp instances (see docker-compose.multi.yml).
        """
    )
    fun switchModel(request: SwitchModelRequest): Response {
        val currentModel = llamaService.getActiveModelName()

        // Verify the requested model file exists
        val models = llamaService.listLocalModels()
        val exists = models.any { it.filename == request.model }

        return if (exists) {
            Response.ok(
                SwitchModelResponse(
                    previousModel = currentModel,
                    newModel = request.model,
                    status = "instruction",
                    message = """
                        To switch to '${request.model}', update DEFAULT_MODEL_FILE in .env and restart:
                        
                        1. Edit .env: DEFAULT_MODEL_FILE=${request.model}
                        2. Run: docker compose restart llama-cpp
                        
                        Or use the multi-instance setup for zero-downtime switching.
                    """.trimIndent()
                )
            ).build()
        } else {
            Response.status(Response.Status.NOT_FOUND)
                .entity(
                    SwitchModelResponse(
                        previousModel = currentModel,
                        newModel = request.model,
                        status = "error",
                        message = "Model file '${request.model}' not found in models directory. Available: ${models.map { it.filename }}"
                    )
                ).build()
        }
    }
}