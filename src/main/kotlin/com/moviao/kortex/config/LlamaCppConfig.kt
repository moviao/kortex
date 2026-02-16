package com.moviao.kortex.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName

@ConfigMapping(prefix = "llamacpp")
interface LlamaCppConfig {

    @WithName("base-url")
    @WithDefault("http://localhost:8081")
    fun baseUrl(): String

    @WithName("timeout")
    @WithDefault("120")
    fun timeout(): Int

    @WithName("models-dir")
    @WithDefault("./models")
    fun modelsDir(): String
}