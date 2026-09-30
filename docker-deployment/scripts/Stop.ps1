[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'

Push-Location $packageRoot
try {
    & docker compose --env-file $envPath -f $composePath down
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Compose shutdown failed.'
    }
}
finally {
    Pop-Location
}

Write-Host 'Containers stopped. Database and uploaded-image volumes were preserved.'
