# MMHI.TEC Facturas Fácil — recuperar fuentes desde Drive
#requires -Version 5.1
<#
Uso:
  powershell -ExecutionPolicy Bypass -File .\tools\recover-from-drive.ps1
  powershell -ExecutionPolicy Bypass -File .\tools\recover-from-drive.ps1 -ZipPath "C:\Users\...\Downloads\MMHI_Facturas_Facil_1.1.0_fuentes.zip"
No borra ni sobrescribe un proyecto existente. No declara compilación ni pruebas.
#>
param(
  [string]$ZipPath = "",
  [string]$Destination = (Join-Path $env:USERPROFILE "Documents\MMHI-Facturas-Facil")
)
$ErrorActionPreference = "Stop"
$sourceUrl = "https://drive.google.com/file/d/1IVkxjrM-RjoR8BTu2nMf5HE9P5IompxB/view"
if (!$ZipPath) {
  $ZipPath = Join-Path $env:USERPROFILE "Downloads\MMHI_Facturas_Facil_1.1.0_fuentes.zip"
}
if (!(Test-Path -LiteralPath $ZipPath -PathType Leaf)) {
  throw "ZIP no localizado en $ZipPath. Descarga manualmente el ZIP original desde $sourceUrl y vuelve a ejecutar -ZipPath."
}
Add-Type -AssemblyName System.IO.Compression
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $ZipPath).Path)
try {
  if ($zip.Entries.Count -eq 0) { throw "El ZIP esta vacio." }
  foreach ($entry in $zip.Entries) {
    $name = $entry.FullName.Replace("\", "/")
    if ($name.StartsWith("/") -or $name -match '(^|/)\.\.(/|$)' -or $name -match '^[A-Za-z]:') {
      throw "Entrada insegura en ZIP: $name"
    }
  }
  $entryCount = $zip.Entries.Count
} finally { $zip.Dispose() }
if (Test-Path -LiteralPath $Destination) {
  throw "El destino ya existe: $Destination. Se evita sobrescribir trabajo previo; indica otro -Destination."
}
$parent = Split-Path -Parent $Destination
if (!(Test-Path $parent)) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
New-Item -ItemType Directory -Path $Destination | Out-Null
try { Expand-Archive -LiteralPath $ZipPath -DestinationPath $Destination -ErrorAction Stop }
catch {
  Remove-Item -LiteralPath $Destination -Recurse -Force
  throw
}
$settings = Get-ChildItem -LiteralPath $Destination -Recurse -File -Include settings.gradle,settings.gradle.kts -ErrorAction SilentlyContinue | Select-Object -First 1
$hash = (Get-FileHash -LiteralPath $ZipPath -Algorithm SHA256).Hash
Write-Host "Fuentes extraidas: $Destination"
Write-Host "Entradas ZIP: $entryCount"
Write-Host "SHA256 ZIP: $hash"
if ($settings) {
  Write-Host "Raiz Gradle localizada: $($settings.DirectoryName)"
  Write-Host "Siguiente paso: ejecutar tools\build-verify.ps1 desde esa raiz (tras verificar wrapper y Android SDK)."
} else {
  Write-Warning "No se encontro settings.gradle(.kts); inspeccionar estructura y confirmar que el ZIP contiene el proyecto Android completo."
}
