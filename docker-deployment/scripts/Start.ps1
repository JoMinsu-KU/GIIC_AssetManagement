[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'

if (-not (Test-Path -LiteralPath $envPath)) {
    throw 'Migration .env is missing. Copy the complete package; do not generate new credentials.'
}
if (-not (Test-Path -LiteralPath (Join-Path $packageRoot 'current-data\SHA256SUMS.txt'))) {
    throw 'Migration snapshot is missing. Copy the complete current-data directory.'
}

$envText = Get-Content -Raw -LiteralPath $envPath
if ($envText -match 'replace-with-') {
    throw 'The .env file still contains placeholder secrets. Run Initialize-Environment.ps1 or replace them manually.'
}

& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is not running or cannot be reached.'
}

Push-Location $packageRoot
try {
    $arguments = @('compose', '--env-file', $envPath, '-f', $composePath, 'up', '-d')
    if (-not $SkipBuild) {
        $arguments += '--build'
    }

    & docker @arguments
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose startup failed.'
    }

    & docker compose --env-file $envPath -f $composePath ps
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to read Docker Compose status.'
    }
}
finally {
    Pop-Location
}

$portLine = Get-Content -LiteralPath $envPath | Where-Object { $_ -match '^FRONTEND_PORT=' } | Select-Object -First 1
$frontendPort = if ($portLine) { ($portLine -split '=', 2)[1].Trim() } else { '3000' }
$proxyLine = Get-Content -LiteralPath $envPath | Where-Object { $_ -match '^APP_CLIENT_IP_HOST_PROXY_ENABLED=' } | Select-Object -First 1
$hostProxyEnabled = $proxyLine -and ($proxyLine -split '=', 2)[1].Trim() -eq 'true'
if ($hostProxyEnabled) {
    $proxyHealthy = $false
    try {
        $proxyHealthy = (Invoke-WebRequest -Uri 'http://127.0.0.1:3000/healthz' -TimeoutSec 2 -UseBasicParsing).StatusCode -eq 200
    } catch { }
    if (-not $proxyHealthy) {
        $proxyTask = Get-ScheduledTask -TaskName 'GIIC Asset Host Proxy' -ErrorAction SilentlyContinue
        if (-not $proxyTask) {
            throw 'The Windows host proxy is not installed. Run Enable-RealClientIp.ps1.'
        }
        Start-ScheduledTask -TaskName 'GIIC Asset Host Proxy'
        for ($attempt = 0; $attempt -lt 10; $attempt++) {
            Start-Sleep -Seconds 1
            try {
                $proxyHealthy = (Invoke-WebRequest -Uri 'http://127.0.0.1:3000/healthz' -TimeoutSec 2 -UseBasicParsing).StatusCode -eq 200
                if ($proxyHealthy) { break }
            } catch { }
        }
        if (-not $proxyHealthy) { throw 'The Windows host proxy did not start.' }
    }
    Write-Host 'GIIC Asset Management: http://localhost:3000 (Windows host proxy)'
}
else {
    Write-Host "GIIC Asset Management: http://localhost:$frontendPort"
}
