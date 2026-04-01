# Kortex

**Lightweight REST gateway for local LLM inference, powered by llama.cpp**

Kortex is a fast, self-hosted REST API server built with Kotlin and Quarkus that lets you run small, open-source language models locally through llama.cpp. No cloud dependencies, no API keys, no Ollama — just raw inference performance with a clean API on top.

---

## Why Kortex?

Running LLMs locally gives you full privacy, zero per-token costs, and offline capability. Kortex sits between your application and llama.cpp's inference engine, providing:

- A clean, documented REST API for chat completions
- An OpenAI-compatible `/v1/chat/completions` proxy endpoint (drop-in replacement)
- Streaming support via Server-Sent Events
- Local GGUF model discovery and management
- Health monitoring and Swagger UI
- Multi-model Docker Compose setups for easy switching
- Native GPU acceleration support (NVIDIA CUDA)

---

## Architecture

```
                                         ┌──────────────────────────┐
                                    ┌───▶│  llama.cpp  (qwen2.5)   │
┌──────────┐     ┌──────────────┐   │    └──────────────────────────┘
│  Client   │────▶│   Kortex     │───┤
│  curl/SDK │◀────│  (Quarkus)   │   │    ┌──────────────────────────┐
└──────────┘     └──────────────┘   ├───▶│  llama.cpp  (phi-3)      │
                   Port 8080        │    └──────────────────────────┘
                                    │
                                    │    ┌──────────────────────────┐
                                    └───▶│  llama.cpp  (tinyllama)  │
                                         └──────────────────────────┘
```

Kortex uses **llama.cpp server** directly — the same C/C++ engine that powers Ollama, LM Studio, and Jan under the hood, but without any wrapper overhead. You manage GGUF model files yourself, giving you full control over quantization, versions, and what's loaded.

---

## Tech Stack

| Component       | Technology                                         |
|-----------------|----------------------------------------------------|
| Language        | Kotlin                                             |
| Framework       | Quarkus (supersonic, subatomic Java)               |
| Build Tool      | Gradle (Kotlin DSL)                                |
| LLM Engine      | llama.cpp server (OpenAI-compatible API)           |
| Model Format    | GGUF (quantized, single-file models)               |
| Containerization| Docker Compose                                     |
| API Docs        | OpenAPI 3.0 + Swagger UI                           |

---

## Recommended Models

These are small, fast models that run well on consumer hardware:

| Model                | Parameters | GGUF Size | RAM Needed | Best For                   |
|----------------------|------------|-----------|------------|----------------------------|
| `qwen2.5-1.5b`      | 1.5B       | ~1 GB     | 4 GB       | Ultra-fast, basic tasks    |
| `tinyllama-1.1b`     | 1.1B       | ~0.6 GB   | 2 GB       | Fastest, minimal footprint |
| `phi-3-mini`         | 3.8B       | ~2 GB     | 4 GB       | Reasoning, logic, code     |
| `gemma-2-2b`         | 2B         | ~1.5 GB   | 4 GB       | Balanced quality & speed   |
| `llama-3.2-3b`       | 3B         | ~2 GB     | 4 GB       | Good all-rounder           |
| `mistral-7b`         | 7B         | ~4 GB     | 8 GB       | Best quality at 7B class   |
| `qwen2.5-7b`         | 7B         | ~4 GB     | 8 GB       | Multilingual, coding       |
| `deepseek-r1-7b`     | 7B         | ~4 GB     | 8 GB       | Strong reasoning & code    |

All models use Q4_K_M quantization for the best balance of size and quality.

---

## Quick Start

The fastest way to get Kortex running. You only need **Docker** installed — no JDK, no Gradle, no IDE.

### 1. Create a project folder and download a model

```bash
mkdir kortex && cd kortex
mkdir models

# Download Qwen 2.5 1.5B (~1 GB, fast and capable)
curl -L -o models/qwen2.5-1.5b-instruct-q4_k_m.gguf \
  "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
```

On Windows PowerShell:

```powershell
mkdir kortex; cd kortex
mkdir models

Invoke-WebRequest `
  -Uri "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf" `
  -OutFile "models\qwen2.5-1.5b-instruct-q4_k_m.gguf"
```

### 2. Download the compose file

```bash
curl -O https://raw.githubusercontent.com/moviao/kortex/main/docker-compose.hub.yml
```

Or create `docker-compose.hub.yml` manually with the content from the [docker-compose.hub.yml reference](#docker-composehubml-reference) section below.

### 3. Start everything

```bash
docker compose -f docker-compose.hub.yml up -d
```

This pulls the pre-built Kortex image and the llama.cpp server image, then starts both containers.

### 4. Wait for the model to load and verify

```bash
# Check health (may take 10-30s on first start while the model loads)
curl http://localhost:8080/api/health
```

### 5. Chat!

```bash
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Explain quicksort in two sentences."}' | jq
```

### 6. Open the Swagger UI

Visit [http://localhost:8080/q/swagger-ui](http://localhost:8080/q/swagger-ui) in your browser to explore all endpoints interactively.

---

## docker-compose.hub.yml Reference

This is the ready-to-use compose file that pulls the pre-built image from Docker Hub:

```yaml
services:
  llama-cpp:
    image: ghcr.io/ggml-org/llama.cpp:server
    container_name: llama-cpp
    ports:
      - "8081:8080"
    volumes:
      - ./models:/models:ro
    environment:
      LLAMA_ARG_MODEL: /models/${DEFAULT_MODEL_FILE:-qwen2.5-1.5b-instruct-q4_k_m.gguf}
      LLAMA_ARG_CTX_SIZE: "${CTX_SIZE:-4096}"
      LLAMA_ARG_HOST: "0.0.0.0"
      LLAMA_ARG_PORT: "8080"
      LLAMA_ARG_N_PREDICT: "${MAX_TOKENS:-512}"
      LLAMA_ARG_THREADS: "${THREADS:-4}"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s

  kortex:
    image: moviao/kortex:latest
    container_name: kortex
    ports:
      - "8080:8080"
    environment:
      LLAMACPP_BASE_URL: "http://llama-cpp:8080"
      LLAMACPP_TIMEOUT: "${LLAMACPP_TIMEOUT:-120}"
      MODELS_DIR: "/models"
    volumes:
      - ./models:/models:ro
    depends_on:
      llama-cpp:
        condition: service_healthy
    restart: unless-stopped
```

---

## API Reference

### Chat Completion

```
POST /api/chat
```

Send a message and receive a complete response.

**Request body:**

```json
{
  "message": "What is Kotlin?",
  "systemPrompt": "You are a concise assistant.",
  "temperature": 0.7,
  "maxTokens": 256,
  "topP": 0.9,
  "topK": 40,
  "repeatPenalty": 1.1,
  "conversationHistory": [
    { "role": "user", "content": "Hi" },
    { "role": "assistant", "content": "Hello! How can I help?" }
  ]
}
```

All fields except `message` are optional.

**Response:**

```json
{
  "content": "Kotlin is a modern, statically typed programming language...",
  "model": "qwen2.5-1.5b-instruct-q4_k_m.gguf",
  "processingTimeMs": 842,
  "usage": {
    "promptTokens": 24,
    "completionTokens": 63,
    "totalTokens": 87
  }
}
```

---

### Streaming Chat (SSE)

```
POST /api/chat/stream
```

Same request body as `/api/chat`. Returns a stream of Server-Sent Events with incremental tokens.

```bash
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "Tell me a short story"}'
```

---

### OpenAI-Compatible Proxy

```
POST /v1/chat/completions
```

Direct passthrough to llama.cpp's OpenAI-compatible endpoint. Works with any OpenAI SDK or tool.

```bash
curl -s http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [{"role": "user", "content": "Hello!"}],
    "temperature": 0.7,
    "max_tokens": 100
  }' | jq
```

**Python (OpenAI SDK):**

```python
from openai import OpenAI

client = OpenAI(base_url="http://localhost:8080/v1", api_key="not-needed")

response = client.chat.completions.create(
    model="default",
    messages=[{"role": "user", "content": "Hello from Python!"}]
)
print(response.choices[0].message.content)
```

---

### List Models

```
GET /api/models
```

Returns all `.gguf` files found in the models directory, the currently active model, and the llama.cpp server status.

```json
{
  "models": [
    { "filename": "qwen2.5-1.5b-instruct-q4_k_m.gguf", "sizeBytes": 1060000000, "sizeHuman": "1.0 GB" },
    { "filename": "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf", "sizeBytes": 638000000, "sizeHuman": "608 MB" }
  ],
  "activeModel": "qwen2.5-1.5b-instruct-q4_k_m.gguf",
  "llamaCppStatus": "ok"
}
```

---

### Switch Model

```
POST /api/models/switch
```

```json
{ "model": "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf" }
```

Returns instructions for switching the active model. For zero-downtime switching, use the multi-model compose setup.

---

### Health Check

```
GET /api/health
```

```json
{
  "status": "UP",
  "llamaCpp": {
    "url": "http://llama-cpp:8080",
    "status": "ok",
    "slotsIdle": 1,
    "slotsProcessing": 0
  },
  "activeModel": "qwen2.5-1.5b-instruct-q4_k_m.gguf",
  "availableModels": 4
}
```

---

### Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/q/swagger-ui
```

---

## Configuration

### Environment Variables

You can create a `.env` file next to your `docker-compose.hub.yml` to customize behavior:

```env
# GGUF model file to load (must exist in ./models/)
DEFAULT_MODEL_FILE=qwen2.5-1.5b-instruct-q4_k_m.gguf

# Context window size (tokens)
CTX_SIZE=4096

# Max tokens to generate per request
MAX_TOKENS=512

# CPU threads for llama.cpp inference
THREADS=4

# Timeout for requests (seconds)
LLAMACPP_TIMEOUT=120
```

| Variable              | Default                                      | Description                        |
|-----------------------|----------------------------------------------|------------------------------------|
| `DEFAULT_MODEL_FILE`  | `qwen2.5-1.5b-instruct-q4_k_m.gguf`         | GGUF file to load on startup       |
| `CTX_SIZE`            | `4096`                                       | Context window size (tokens)       |
| `MAX_TOKENS`          | `512`                                        | Max tokens to generate per request |
| `THREADS`             | `4`                                          | CPU threads for inference          |
| `LLAMACPP_TIMEOUT`    | `120`                                        | Request timeout in seconds         |

---

## Adding More Models

Drop any `.gguf` file into your `./models/` directory. It will appear in `GET /api/models` immediately.

```bash
# Download TinyLlama (smallest, ~600 MB)
curl -L -o models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

# Download Phi-3 Mini (strong reasoning, ~2 GB)
curl -L -o models/Phi-3-mini-4k-instruct-q4.gguf \
  "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf"
```

Or use the bundled download scripts:

```bash
# Linux / macOS / Git Bash
./scripts/download-models.sh

# Windows PowerShell
.\scripts\download-models.ps1
```

Browse more models at [huggingface.co](https://huggingface.co/models?search=GGUF) — look for files with `Q4_K_M` quantization.

---

## Switching Models

### Option A: Single model (restart required)

Update `DEFAULT_MODEL_FILE` in your `.env` and restart llama-cpp:

```bash
# Edit .env
echo "DEFAULT_MODEL_FILE=Phi-3-mini-4k-instruct-q4.gguf" > .env

# Restart
docker compose -f docker-compose.hub.yml restart llama-cpp
```

### Option B: Multi-model (zero downtime)

Run multiple llama.cpp instances simultaneously, each serving a different model:

```bash
docker compose -f docker-compose.multi.yml up -d
```

This starts three llama.cpp servers (Qwen on 8081, TinyLlama on 8082, Phi-3 on 8083) and the Kortex gateway on 8080.

---

## GPU Acceleration

For NVIDIA GPUs, create a `docker-compose.gpu.yml` override:

```yaml
services:
  llama-cpp:
    image: ghcr.io/ggml-org/llama.cpp:server-cuda
    environment:
      LLAMA_ARG_N_GPU_LAYERS: "999"
      NVIDIA_VISIBLE_DEVICES: "all"
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
```

Then run:

```bash
docker compose -f docker-compose.hub.yml -f docker-compose.gpu.yml up -d
```

Requires [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) installed on the host.

---

## Building from Source

If you want to modify Kortex or contribute:

### Prerequisites

- JDK 21+
- Docker & Docker Compose

### Clone and run in dev mode

```bash
git clone https://github.com/moviao/kortex.git
cd kortex

# Start llama.cpp backend
docker compose up -d llama-cpp

# Run Kortex in dev mode (hot reload)
./gradlew quarkusDev
```

### Build the Docker image locally

```bash
docker compose up --build -d
```

### Run tests

```bash
./gradlew test
```

---

## Project Structure

```
kortex/
├── build.gradle.kts                    # Gradle build (Kotlin DSL)
├── settings.gradle.kts                 # Gradle settings
├── gradle.properties                   # Version catalog
├── docker-compose.yml                  # Build-from-source setup
├── docker-compose.hub.yml              # Pre-built image (for end users)
├── docker-compose.gpu.yml              # GPU overlay
├── docker-compose.multi.yml            # Multi-model setup
├── Dockerfile                          # Multi-stage build
├── .env                                # Runtime configuration
├── models/                             # GGUF model files
├── scripts/
│   ├── download-models.sh              # Linux/macOS model downloader
│   ├── download-models-win.sh          # Windows Git Bash downloader
│   └── download-models.ps1             # Windows PowerShell downloader
└── src/main/
    ├── kotlin/com/moviao/kortex/
    │   ├── config/
    │   │   └── LlamaCppConfig.kt       # Configuration mapping
    │   ├── model/
    │   │   ├── ChatModels.kt           # Request/response DTOs
    │   │   └── ModelRegistry.kt        # Model discovery DTOs
    │   ├── service/
    │   │   └── LlamaCppService.kt      # llama.cpp client service
    │   └── resource/
    │       ├── ChatResource.kt         # /api/chat endpoints
    │       ├── ModelResource.kt        # /api/models endpoints
    │       └── HealthResource.kt       # /api/health + readiness probe
    └── resources/
        └── application.properties      # Quarkus configuration
```

---

## Troubleshooting

### llama-cpp container is unhealthy

```bash
docker compose -f docker-compose.hub.yml logs llama-cpp
```

Common causes:
- **Model file not found**: Ensure the `.gguf` file specified in `DEFAULT_MODEL_FILE` exists in `./models/`
- **Insufficient memory**: Larger models need more RAM. Start with `qwen2.5-1.5b` or `tinyllama`
- **Wrong architecture**: On Apple Silicon, ensure you're pulling the right platform image

### Kortex can't reach llama-cpp

```bash
curl http://localhost:8081/health
```

If running outside Docker (dev mode), make sure `LLAMACPP_BASE_URL` is `http://localhost:8081`.

### Slow inference

- Use a smaller model (TinyLlama, Qwen 1.5B)
- Increase `THREADS` in `.env`
- Enable GPU acceleration
- Reduce `CTX_SIZE` if you don't need long context

### Stopping everything

```bash
docker compose -f docker-compose.hub.yml down
```

---

## License

MIT

---

## Acknowledgments

- [llama.cpp](https://github.com/ggml-org/llama.cpp) — The inference engine that makes local LLMs possible
- [Quarkus](https://quarkus.io) — Supersonic Subatomic Java framework
- [Hugging Face](https://huggingface.co) — Model hosting and community












# Hi, I'm H 👋

Principal Engineer & Founder of **HD International Ltd** — a Bulgarian-based software consultancy delivering
enterprise-grade solutions across Architecture, Cloud, AI, DevOps, and Operations.

---

## 🏢 HD International Ltd

> *Engineering clarity from complexity.*

We partner with startups and enterprises to design, build, and operate resilient, scalable systems.

---

## 🛠️ Services

**Software Architecture**
Designing scalable, maintainable systems — microservices, event-driven architecture, domain-driven
design, and API strategy.

**Cloud Engineering** · AWS · GCP · Azure . On Premise
Infrastructure design, cloud migrations, cost optimisation, and multi-cloud strategies.

**DevOps & CI/CD**
End-to-end pipeline automation, containerisation (Docker/Kubernetes), GitOps, and platform engineering.

**AI & ML Solutions**
Integrating LLMs, building ML pipelines, and delivering production-ready AI features into existing products.

**Operations & SRE**
Observability, incident management, SLA design, and reliability engineering for critical systems.

---

## 🧰 Core Stack

**Languages**
`Java` `C#` `PHP 8+` `Python` `Go` `TypeScript` `JavaScript` `Rust` `Bash` `Ruby` `Scala` `Kotlin`

**Frameworks & Libraries**
`Spring Boot` `ASP.NET Core` `Laravel` `Symfony` `FastAPI` `Django` `Flask` `NestJS` `Express`
`React` `Next.js` `Angular` `LangChain` `LlamaIndex` `Celery` `GraphQL`

**Cloud & Infrastructure**
`AWS` `GCP` `Azure` `Terraform` `Pulumi` `CDK` `Ansible` `Packer` `Vault` `Consul`

**Containers & Orchestration**
`Docker` `Kubernetes` `Helm` `Istio` `ArgoCD` `FluxCD` `Kustomize` `Rancher` `OpenShift`

**CI/CD & DevOps**
`GitHub Actions` `GitLab CI` `Jenkins` `CircleCI` `TeamCity` `Tekton` `SonarQube` `Nexus`

**Data & Messaging**
`PostgreSQL` `MySQL` `MongoDB` `Redis` `Elasticsearch` `Kafka` `RabbitMQ` `NATS` `Kinesis`
`Snowflake` `BigQuery` `dbt` `Airflow` `Spark`

**AI & ML**
`LangChain` `LlamaIndex` `OpenAI API` `Anthropic API` `HuggingFace` `PyTorch` `TensorFlow`
`scikit-learn` `MLflow` `Weights & Biases` `Pinecone` `Weaviate` `FAISS`

**Observability & Operations**
`Datadog` `Prometheus` `Grafana` `Loki` `Jaeger` `OpenTelemetry` `PagerDuty` `Sentry`
`ELK Stack` `New Relic` `Dynatrace`

**Security & Compliance**
`Vault` `Trivy` `Snyk` `OWASP ZAP` `Falco` `Checkov` `AWS Security Hub` `IAM` `OAuth2` `OIDC`

---

## 📬 Work With Us

We take on select consulting engagements and fractional CTO mandates.

📧 hdinternational82@gmail.com 
💼 [LinkedIn]([https://www.linkedin.com/in/time-to-move-h/])

> *Registered in Bulgaria & Plovdiv · HD International Ltd*
> VAT BG-204723748
