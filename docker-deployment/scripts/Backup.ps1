[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $packageRoot "backups\$timestamp"

if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Missing environment file: $envPath"
}

New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

Push-Location $packageRoot
try {
    $dumpCommand = 'mysqldump --default-character-set=utf8mb4 --single-transaction --routines --triggers --events -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" > /backups/{0}/database.sql' -f $timestamp
    & docker compose --env-file $envPath -f $composePath exec -T db sh -c $dumpCommand
    if ($LASTEXITCODE -ne 0) {
        throw 'Database backup failed.'
    }

    & docker compose --env-file $envPath -f $composePath exec -T backend tar -czf "/backups/$timestamp/uploads.tar.gz" -C /app/uploads .
    if ($LASTEXITCODE -ne 0) {
        throw 'Uploaded-image backup failed.'
    }

    $status = & docker compose --env-file $envPath -f $composePath ps
    $status | Set-Content -LiteralPath (Join-Path $backupRoot 'containers.txt') -Encoding UTF8

    $hashLines = Get-ChildItem -File -LiteralPath $backupRoot |
        Where-Object Name -in @('database.sql', 'uploads.tar.gz') |
        ForEach-Object {
            $hash = Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName
            "$($hash.Hash.ToLowerInvariant())  $($_.Name)"
        }
    [System.IO.File]::WriteAllLines(
        (Join-Path $backupRoot 'SHA256SUMS.txt'),
        $hashLines,
        [System.Text.UTF8Encoding]::new($false)
    )
}
finally {
    Pop-Location
}

Write-Host "Backup created: $backupRoot"
