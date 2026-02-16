# download-models.ps1 — Native PowerShell model downloader
# Usage: .\scripts\download-models.ps1

$ErrorActionPreference = "Stop"

$ModelsDir = Join-Path (Split-Path $PSScriptRoot) "models"
New-Item -ItemType Directory -Force -Path $ModelsDir | Out-Null

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Downloading GGUF models to: $ModelsDir"     -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

function Download-Model {
    param(
        [string]$Name,
        [string]$Filename,
        [string]$Url
    )

    $FilePath = Join-Path $ModelsDir $Filename

    if (Test-Path $FilePath) {
        Write-Host "[SKIP] $Name - already exists" -ForegroundColor Yellow
        return
    }

    Write-Host "[DOWNLOADING] $Name..." -ForegroundColor Green
    Write-Host "  -> $Filename"

    try {
        $ProgressPreference = 'SilentlyContinue'  # Speeds up Invoke-WebRequest
        Invoke-WebRequest -Uri $Url -OutFile $FilePath -UseBasicParsing
        $size = (Get-Item $FilePath).Length / 1MB
        Write-Host "[DONE] $Name ($([math]::Round($size, 1)) MB)" -ForegroundColor Green
    }
    catch {
        Write-Host "[ERROR] Failed to download $Name : $_" -ForegroundColor Red

        # Fallback to curl if available (much faster on Windows)
        if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
            Write-Host "  Retrying with curl.exe..." -ForegroundColor Yellow
            & curl.exe -L --progress-bar -o $FilePath $Url
            if ($LASTEXITCODE -eq 0) {
                $size = (Get-Item $FilePath).Length / 1MB
                Write-Host "[DONE] $Name ($([math]::Round($size, 1)) MB)" -ForegroundColor Green
            } else {
                Write-Host "[FAILED] Could not download $Name" -ForegroundColor Red
            }
        }
    }
    Write-Host ""
}

# ── Qwen 2.5 1.5B — ultra-fast, ~1GB ────────────────────────
Download-Model `
    -Name "Qwen 2.5 1.5B Instruct (Q4_K_M)" `
    -Filename "qwen2.5-1.5b-instruct-q4_k_m.gguf" `
    -Url "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"

# ── TinyLlama 1.1B — smallest, ~0.6GB ───────────────────────
#Download-Model `
#    -Name "TinyLlama 1.1B Chat (Q4_K_M)" `
#    -Filename "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf" `
#    -Url "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf"

# ── Phi-3 Mini 3.8B — strong reasoning, ~2GB ────────────────
#Download-Model `
#    -Name "Phi-3 Mini 3.8B Instruct (Q4)" `
#    -Filename "Phi-3-mini-4k-instruct-q4.gguf" `
#    -Url "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf"

# ── Gemma 2 2B — balanced, ~1.5GB ───────────────────────────
#Download-Model `
#    -Name "Gemma 2 2B Instruct (Q4_K_M)" `
#    -Filename "gemma-2-2b-it-Q4_K_M.gguf" `
#    -Url "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host " All models ready!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Available models:"
Get-ChildItem -Path $ModelsDir -Filter "*.gguf" | ForEach-Object {
    $sizeMB = [math]::Round($_.Length / 1MB, 1)
    Write-Host "  $($_.Name)  ($sizeMB MB)"
}
Write-Host ""
Write-Host "Set DEFAULT_MODEL_FILE in .env to choose the active model." -ForegroundColor Yellow