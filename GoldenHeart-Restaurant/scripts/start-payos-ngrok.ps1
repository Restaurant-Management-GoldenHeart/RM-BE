$ErrorActionPreference = 'Stop'

$configPath = Join-Path $PSScriptRoot '..\ngrok\payos-ngrok.yml'
$resolvedConfigPath = [System.IO.Path]::GetFullPath($configPath)

Write-Host "[ngrok] Using config: $resolvedConfigPath"
Write-Host "[ngrok] Exposing backend: http://127.0.0.1:1010"
Write-Host "[ngrok] Public webhook: https://overload-stopper-substance.ngrok-free.dev/api/v1/payment-gateways/payos/webhook"

ngrok start --config "$resolvedConfigPath" goldenheart-payos-be
