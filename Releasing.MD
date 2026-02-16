# Releasing & Distributing Kortex

This guide covers every way to package and distribute Kortex so anyone can run it — no IntelliJ, no IDE, no build tools required on the user's machine.

---

## TL;DR — Recommended Approaches

| Audience                  | Best Option                        | User Needs              |
|---------------------------|------------------------------------|-------------------------|
| **Anyone with Docker**    | Docker Compose (Option 1)          | Docker only             |
| **Java developers**       | Uber-JAR (Option 2)               | JDK 21 only             |
| **Maximum performance**   | Native binary (Option 3)           | Nothing (self-contained)|
| **Public distribution**   | GitHub Releases + Docker Hub       | Docker or JDK 21        |

---

## Option 1: Docker Compose (Easiest for end users)

This is the **recommended** approach. The user needs nothing except Docker.

### What you publish

```
kortex/
├── docker-compose.yml
├── docker-compose.gpu.yml
├── .env
├── models/
└── scripts/
    ├── download-models.sh
    └── download-models.ps1
```

### What the user does

```bash
git clone https://github.com/moviao/kortex.git
cd kortex
./scripts/download-models.sh        # or .ps1 on Windows
docker compose up -d
curl http://localhost:8080/api/health
```

### Pre-built Docker image on Docker Hub / GHCR

Instead of building from source, publish a pre-built image so the user never needs Gradle or JDK:

**Build and push (you do this once per release):**

```bash
# Build the image
docker build -t ghcr.io/moviao/kortex:1.0.0 -t ghcr.io/moviao/kortex:latest .

# Push to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u moviao --password-stdin
docker push ghcr.io/moviao/kortex:1.0.0
docker push ghcr.io/moviao/kortex:latest

# Or push to Docker Hub
docker login
docker tag ghcr.io/moviao/kortex:1.0.0 moviao/kortex:1.0.0
docker push moviao/kortex:1.0.0
docker push moviao/kortex:latest
```

**Then update `docker-compose.yml` to use the pre-built image:**

```yaml
services:
  llm-server:
    image: ghcr.io/moviao/kortex:latest   # <-- pre-built, no build step
    # Remove the 'build:' section entirely
    ports:
      - "8080:8080"
    environment:
      LLAMACPP_BASE_URL: "http://llama-cpp:8080"
    depends_on:
      llama-cpp:
        condition: service_healthy
```

Now users just run `docker compose up -d` — no Gradle, no JDK, no build.

---

## Option 2: Uber-JAR (Single executable JAR)

A single `.jar` file that contains everything. User only needs JDK 21.

### Configure the build

Add this to `application.properties`:

```properties
quarkus.package.jar.type=uber-jar
```

Or build with the flag:

```bash
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

This produces a single file:

```
build/kortex-1.0.0-runner.jar    (~20-30 MB)
```

### What you distribute

```
kortex-1.0.0/
├── kortex-1.0.0-runner.jar
├── run.sh
├── run.bat
├── .env
├── models/
└── scripts/
    ├── download-models.sh
    └── download-models.ps1
```

### Create launcher scripts

**`run.sh`** (Linux/macOS):

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java 21+ is required. Install it from https://adoptium.net"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 21 ]; then
    echo "ERROR: Java 21+ required, found Java $JAVA_VER"
    exit 1
fi

echo "Starting Kortex..."
exec java \
    -Xms64m -Xmx256m \
    -Dllamacpp.base-url="${LLAMACPP_BASE_URL:-http://localhost:8081}" \
    -Dllamacpp.timeout="${LLAMACPP_TIMEOUT:-120}" \
    -Dllamacpp.models-dir="${MODELS_DIR:-$SCRIPT_DIR/models}" \
    -jar "$SCRIPT_DIR/kortex-1.0.0-runner.jar"
```

**`run.bat`** (Windows):

```batch
@echo off
setlocal

where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java 21+ is required. Install from https://adoptium.net
    pause
    exit /b 1
)

echo Starting Kortex...
java ^
    -Xms64m -Xmx256m ^
    -Dllamacpp.base-url=%LLAMACPP_BASE_URL:http://localhost:8081% ^
    -Dllamacpp.timeout=%LLAMACPP_TIMEOUT:120% ^
    -Dllamacpp.models-dir=%MODELS_DIR:.\models% ^
    -jar "%~dp0kortex-1.0.0-runner.jar"

pause
```

### What the user does

```bash
# 1. Download and extract the release
# 2. Download models
./scripts/download-models.sh

# 3. Start llama.cpp (still needed separately)
docker run -d -p 8081:8080 -v ./models:/models:ro \
  ghcr.io/ggml-org/llama.cpp:server \
  -m /models/qwen2.5-1.5b-instruct-q4_k_m.gguf \
  --host 0.0.0.0 --port 8080

# 4. Start Kortex
./run.sh
# or on Windows: run.bat
```

---

## Option 3: Native Binary (GraalVM — no JDK needed at runtime)

Compiles to a native executable. ~15ms startup, ~50MB binary, zero dependencies.

### Build

```bash
# Requires GraalVM 21+ with native-image installed
./gradlew build -Dquarkus.native.enabled=true

# Or build inside a container (no local GraalVM needed)
./gradlew build -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true
```

Produces:

```
build/kortex-1.0.0-runner    # Native binary (~50 MB)
```

### Distribute

```bash
# The user just runs it directly — no Java, no JDK, nothing
./kortex-1.0.0-runner
```

Build separate binaries for each OS/arch:

```
releases/
├── kortex-1.0.0-linux-amd64
├── kortex-1.0.0-linux-arm64
├── kortex-1.0.0-macos-amd64
├── kortex-1.0.0-macos-arm64
└── kortex-1.0.0-windows-amd64.exe
```

---

## Option 4: Gradle Wrapper (Build from source — developer-friendly)

Include the Gradle wrapper so users can build without installing Gradle.

### Generate the wrapper

```bash
gradle wrapper --gradle-version 8.11
```

This creates:

```
kortex/
├── gradlew          # Linux/macOS launcher
├── gradlew.bat      # Windows launcher
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

### What the user does

```bash
git clone https://github.com/moviao/kortex.git
cd kortex

# Build — no Gradle installation needed, just JDK 21
./gradlew build

# Run
java -jar build/quarkus-app/quarkus-run.jar
```

Or dev mode:

```bash
./gradlew quarkusDev
```

---

## GitHub Releases — Automated CI/CD

Create a `.github/workflows/release.yml` to automate building and publishing on every tag:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write
  packages: write

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      # ── Build Uber-JAR ─────────────────────────────
      - name: Build Uber-JAR
        run: ./gradlew build -Dquarkus.package.jar.type=uber-jar

      # ── Build Docker Image ─────────────────────────
      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ghcr.io/moviao/kortex:${{ github.ref_name }}
            ghcr.io/moviao/kortex:latest

      # ── Create GitHub Release ──────────────────────
      - name: Create Release
        uses: softprops/action-gh-release@v2
        with:
          files: |
            build/*-runner.jar
          body: |
            ## Kortex ${{ github.ref_name }}

            ### Quick Start (Docker — recommended)
            ```bash
            git clone https://github.com/moviao/kortex.git
            cd kortex
            docker compose up -d
            ```

            ### Quick Start (JAR — requires JDK 21)
            ```bash
            java -jar kortex-*-runner.jar
            ```

            ### Docker Image
            ```bash
            docker pull ghcr.io/moviao/kortex:${{ github.ref_name }}
            ```
```

### How to create a release

```bash
# Tag and push
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will automatically build the uber-jar, Docker image, and create a release page with downloadable assets.

---

## Summary: What the end user experience looks like

### Docker user (knows nothing about Java/Kotlin)

```bash
git clone https://github.com/moviao/kortex.git
cd kortex
./scripts/download-models.sh
docker compose up -d
# Done. API at http://localhost:8080
```

### Java developer (has JDK 21)

```bash
# Download the uber-jar from GitHub Releases
curl -LO https://github.com/moviao/kortex/releases/latest/download/kortex-runner.jar
java -jar kortex-runner.jar
```

### Building from source (has JDK 21, no Gradle needed)

```bash
git clone https://github.com/moviao/kortex.git
cd kortex
./gradlew quarkusDev    # Dev mode with hot reload
```

---

## Checklist before releasing

- [ ] Bump version in `build.gradle.kts` and `gradle.properties`
- [ ] Run tests: `./gradlew test`
- [ ] Build and verify uber-jar: `./gradlew build -Dquarkus.package.jar.type=uber-jar`
- [ ] Test Docker compose from scratch: `docker compose up --build`
- [ ] Verify all endpoints work
- [ ] Update README.md with any new features
- [ ] Commit Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)
- [ ] Tag: `git tag v1.x.x && git push origin v1.x.x`
- [ ] Verify GitHub Actions builds and publishes successfully