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