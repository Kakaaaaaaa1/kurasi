<#
  Bangun APK lalu terbitkan sebagai rilis GitHub, supaya aplikasi di HP bisa menawarkan pembaruan.

  Sebelum menjalankan:
    1. Naikkan versionCode dan versionName di app/build.gradle (versionName harus lebih besar dari rilis sebelumnya).
    2. Commit perubahan kodenya.
  Pemakaian (dari folder proyek):
    powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Notes "Perbaikan dock dan tutorial"
  Syarat: GitHub CLI (gh) sudah terpasang dan login (gh auth login).
#>
param(
    [Parameter(Mandatory = $true)][string]$Notes
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

$gradle = Get-Content app\build.gradle -Raw
$version = [regex]::Match($gradle, "versionName\s+'([^']+)'").Groups[1].Value
if (-not $version) { throw "versionName tidak ditemukan di app/build.gradle" }
$tag = "v$version"

if (git status --porcelain) { throw "Ada perubahan yang belum di-commit. Commit dulu supaya rilis sesuai kodenya." }
if (git tag -l $tag) { throw "Tag $tag sudah ada. Naikkan versionName dulu." }

$env:JAVA_HOME = $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" })
$env:ANDROID_HOME = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" })

.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { throw "Build atau tes gagal, rilis dibatalkan." }

New-Item -ItemType Directory -Force dist | Out-Null
$apk = "dist\Kurasi-$tag.apk"
Copy-Item app\build\outputs\apk\debug\app-debug.apk $apk -Force
Copy-Item $apk dist\Kurasi-v1.0-debug.apk -Force   # salinan terbaru dengan nama lama

git tag $tag
git push origin HEAD
git push origin $tag
gh release create $tag $apk --title "Kurasi $version" --notes $Notes
Write-Host "Rilis $tag terbit. Aplikasi di HP akan menawarkan pembaruan saat dibuka."
