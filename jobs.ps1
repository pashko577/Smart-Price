# Ejecuta un job de SmartPrice (Apache Spark, API Java) con el JDK 17 del proyecto.
# No modifica variables globales: JAVA_HOME y HADOOP_HOME solo se fijan para este proceso.
#
# Uso (desde la raíz del proyecto):
#   .\jobs.ps1 <comando> [argumentos]
#   .\jobs.ps1            -> muestra los comandos disponibles
#
# Compilar antes (o tras cambiar el código):
#   .\jobs.ps1 build

param([Parameter(ValueFromRemainingArguments = $true)][string[]]$JobArgs)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

$env:JAVA_HOME   = "C:\tools\jdk-17"
$env:HADOOP_HOME = "C:\tools\hadoop"
$env:PATH        = "$env:JAVA_HOME\bin;$env:HADOOP_HOME\bin;$env:PATH"

if ($JobArgs.Count -gt 0 -and $JobArgs[0] -eq "build") {
    & "$root\mvnw.cmd" -B -q package
    exit $LASTEXITCODE
}

$jar = Join-Path $root "smartprice-jobs\target\smartprice-jobs.jar"
if (-not (Test-Path $jar)) {
    Write-Host "No existe $jar. Compilando..."
    & "$root\mvnw.cmd" -B -q package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

# Opciones que Spark 3.5 necesita en Java 17 (las mismas que añade spark-submit).
$jvmOpts = @(
    "-Xmx2g",
    "-XX:+IgnoreUnrecognizedVMOptions",
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED",
    "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.action=ALL-UNNAMED",
    "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED",
    "-Djdk.reflect.useDirectMethodHandle=false",
    "-Dfile.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8"
)

Push-Location $root   # las rutas por defecto (data/...) y el .env son relativas a la raíz
try {
    & "$env:JAVA_HOME\bin\java.exe" @jvmOpts -jar $jar @JobArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
