# build-exe.ps1
# Baut die App als verschickbare .exe via jlink + jpackage
# Werte unten sind bereits auf dein Projekt angepasst.
#
# Nutzung:
#   .\build-exe.ps1                 -> baut auch mit Maven (mvn clean package)
#   .\build-exe.ps1 -SkipMavenBuild -> ueberspringt Maven, nutzt vorhandenes JAR
#                                       (z.B. wenn du gerade ueber IntelliJ gebaut hast)

param(
    [switch]$SkipMavenBuild
)

# ---------------- Konfiguration ----------------
$AppName        = "HypixelTracker"
$MainJar        = "hypixeltracker-1.0.0.jar"
$MainClass      = "com.hypixeltracker.Main"
$InputDir       = "target"
$IconPath       = ""                              # leer = kein eigenes Icon
$JavaHome       = "C:\Program Files\Java\jdk-25"
$JavaFxJmods    = "C:\javafx-jmods\javafx-jmods-25.0.4"
$RuntimeImage   = "runtime-image"
$OutputDir      = "."                              # .exe landet direkt hier

$Modules = "java.base,javafx.controls,javafx.fxml,java.net.http,java.desktop"

# ---------------- Vorbereitung ----------------
Write-Host "== Baue $AppName ==" -ForegroundColor Cyan

if (Test-Path $RuntimeImage) {
    Write-Host "Loesche altes Runtime-Image..."
    Remove-Item -Recurse -Force $RuntimeImage
}

# ---------------- Schritt 1: Maven-Package ----------------
if ($SkipMavenBuild) {
    Write-Host "-- Ueberspringe mvn (nutze vorhandenes JAR aus $InputDir) --" -ForegroundColor Yellow
} else {
    Write-Host "-- mvn clean package --" -ForegroundColor Yellow
    & mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven-Build ist fehlgeschlagen. Breche ab."
        exit 1
    }
}

if (-not (Test-Path "$InputDir\$MainJar")) {
    Write-Error "$MainJar wurde nicht in $InputDir gefunden. Bitte 'dir target' pruefen."
    exit 1
}

# ---------------- Schritt 2: jlink ----------------
Write-Host "-- jlink: erstelle Runtime-Image --" -ForegroundColor Yellow

$modulePath = "$JavaHome\jmods;$JavaFxJmods"

& jlink `
    --module-path $modulePath `
    --add-modules $Modules `
    --output $RuntimeImage `
    --strip-debug `
    --no-header-files `
    --no-man-pages

if ($LASTEXITCODE -ne 0) {
    Write-Error "jlink ist fehlgeschlagen."
    exit 1
}

# ---------------- Schritt 3: jpackage ----------------
Write-Host "-- jpackage: baue .exe --" -ForegroundColor Yellow

$jpackageArgs = @(
    "--input", $InputDir,
    "--name", $AppName,
    "--main-jar", $MainJar,
    "--main-class", $MainClass,
    "--type", "exe",
    "--win-shortcut",
    "--runtime-image", $RuntimeImage,
    "--dest", $OutputDir
)

if ($IconPath -and (Test-Path $IconPath)) {
    $jpackageArgs += @("--icon", $IconPath)
}

& jpackage @jpackageArgs

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage ist fehlgeschlagen."
    exit 1
}

Write-Host "== Fertig! HypixelTracker-1.0.exe sollte jetzt hier liegen ==" -ForegroundColor Green
