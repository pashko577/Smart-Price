# Arranca la aplicación web SmartPrice (Spring Boot) en http://localhost:8085
# Usa el JDK 17 del proyecto solo para este proceso; lee MONGODB_URI del archivo .env.
#
# Uso (desde la raíz del proyecto):
#   .\app.ps1           -> compila si hace falta y arranca
#   .\app.ps1 build     -> recompila (tras cambiar el código) y arranca

param([string]$Action = "")

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$env:JAVA_HOME = "C:\tools\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$jar = Join-Path $root "smartprice-app\target\smartprice-app.jar"
if ($Action -eq "build" -or -not (Test-Path $jar)) {
    & "$root\mvnw.cmd" -B -q -pl smartprice-app package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Push-Location $root   # el .env se busca en la carpeta de ejecución
try {
    Write-Host "SmartPrice en http://localhost:8085  (Ctrl+C para detener)"
    & "$env:JAVA_HOME\bin\java.exe" "-Dfile.encoding=UTF-8" -jar $jar
} finally {
    Pop-Location
}
