package com.moviao.kortex.resource

import com.moviao.kortex.service.LlamaCppService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness

@Readiness
@ApplicationScoped
class LlamaCppHealthCheck : HealthCheck {

    @Inject
    lateinit var llamaService: LlamaCppService

    override fun call(): HealthCheckResponse {
        return if (llamaService.isHealthy()) {
            HealthCheckResponse.up("llama-cpp-server")
        } else {
            HealthCheckResponse.down("llama-cpp-server")
        }
    }
}

@Path("/api/health")
class HealthResource {

    @Inject
    lateinit var llamaService: LlamaCppService

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): Response {
        val health = llamaService.healthCheck()
        val models = llamaService.listLocalModels()
        val activeModel = llamaService.getActiveModelName()

        val body = mapOf(
            "status" to if (llamaService.isHealthy()) "UP" else "DOWN",
            "llamaCpp" to mapOf(
                "url" to llamaService.config.baseUrl(),
                "status" to health.status,
                "slotsIdle" to health.slots_idle,
                "slotsProcessing" to health.slots_processing
            ),
            "activeModel" to activeModel,
            "availableModels" to models.size
        )

        return if (llamaService.isHealthy()) {
            Response.ok(body).build()
        } else {
            Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(body).build()
        }
    }
}