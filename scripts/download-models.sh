#!/usr/bin/env bash
set -euo pipefail

# Download small, fast GGUF models from Hugging Face
# Usage: ./scripts/download-models.sh

MODELS_DIR="$(cd "$(dirname "$0")/.." && pwd)/models"
mkdir -p "$MODELS_DIR"

echo "Downloading models to $MODELS_DIR ..."

# ── Qwen 2.5 1.5B (ultra-fast, ~1GB) ─────────────────────
echo ">> Qwen 2.5 1.5B Instruct Q4_K_M..."
curl -L -o "$MODELS_DIR/qwen2.5-1.5b-instruct-q4_k_m.gguf" \
  "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"

# ── TinyLlama 1.1B (smallest, ~0.6GB) ────────────────────
#echo ">> TinyLlama 1.1B Chat Q4_K_M..."
#curl -L -o "$MODELS_DIR/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf" \
#  "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

# ── Phi-3 Mini 3.8B (strong reasoning, ~2GB) ─────────────
#echo ">> Phi-3 Mini 3.8B Instruct Q4_K_M..."
#curl -L -o "$MODELS_DIR/Phi-3-mini-4k-instruct-q4.gguf" \
#  "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf"

# ── Gemma 2 2B (Google, balanced, ~1.5GB) ─────────────────
#echo ">> Gemma 2 2B Instruct Q4_K_M..."
#curl -L -o "$MODELS_DIR/gemma-2-2b-it-Q4_K_M.gguf" \
#  "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"

echo ""
echo "Done! Available models:"
ls -lh "$MODELS_DIR"/*.gguf
echo ""
echo "Set DEFAULT_MODEL_FILE in .env to switch the active model."