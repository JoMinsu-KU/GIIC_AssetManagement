[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'

if (-not (Test-Path -LiteralPath $envPath)) {
    & (Join-Path $PSScriptRoot 'Initialize-Environment.ps1')
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
Write-Host "GIIC Asset Management: http://localhost:$frontendPort"
