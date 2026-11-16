#requires -Version 5.1
<#
MMHI.TEC Facturas Facil - compilacion local verificable.
Ejecutar desde la raiz del proyecto Android:
  powershell -ExecutionPolicy Bypass -File .\tools\build-verify.ps1
No afirma que exista APK hasta comprobar archivo y resultado de Gradle.
#>
param([string]$ProjectRoot = (Get-Location).Path)
$ErrorActionPreference = "Stop"
Set-Location $ProjectRoot
if (!(Test-Path "settings.gradle.kts") -and !(Test-Path "settings.gradle")) {
    throw "No es raiz de proyecto Gradle Android: falta settings.gradle(.kts)"
}
if (!(Test-Path "gradlew.bat")) { throw "Falta gradlew.bat: recuperar wrapper del ZIP original." }
if (!(Get-Command java -ErrorAction SilentlyContinue) -and !(Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    throw "Java no localizado. Instalar/configurar JDK compatible con Android Gradle Plugin."
}
if (!(Test-Path "local.properties") -and !$env:ANDROID_HOME -and !$env:ANDROID_SDK_ROOT) {
    throw "No se encontro configuracion del Android SDK (local.properties/ANDROID_HOME/ANDROID_SDK_ROOT)."
}
$log = Join-Path $ProjectRoot "build-verify.log"
& .\gradlew.bat --no-daemon --stacktrace :app:assembleDebug *>&1 | Tee-Object -FilePath $log
if ($LASTEXITCODE -ne 0) { throw "Gradle fallo (exit $LASTEXITCODE). Consultar $log" }
$apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
if (!(Test-Path $apk)) { throw "Gradle termino pero no existe $apk" }
$file = Get-Item $apk
if ($file.Length -lt 1024) { throw "APK demasiado pequena: $($file.Length) bytes" }
$hash = Get-FileHash -Algorithm SHA256 -Path $apk
Write-Host "APK GENERADA (sin afirmar prueba en emulador): $apk"
Write-Host "Bytes: $($file.Length)"
Write-Host "SHA256: $($hash.Hash)"
Write-Host "Log: $log"
