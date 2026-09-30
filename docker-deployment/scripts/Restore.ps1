[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupDirectory,

    [switch]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'
if (-not $ConfirmRestore) {
    throw 'Restore changes the target database. Run again with -ConfirmRestore after checking the backup path.'
}

$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'
$resolvedBackup = (Resolve-Path -LiteralPath $BackupDirectory).Path
$databaseBackup = Join-Path $resolvedBackup 'database.sql'
$uploadsBackup = Join-Path $resolvedBackup 'uploads.tar.gz'

if (-not (Test-Path -LiteralPath $databaseBackup)) {
    throw "Database backup not found: $databaseBackup"
}
if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Environment file not found: $envPath"
}

$checksumPath = Join-Path $resolvedBackup 'SHA256SUMS.txt'
if (Test-Path -LiteralPath $checksumPath) {
    foreach ($line in Get-Content -LiteralPath $checksumPath) {
        if ($line -notmatch '^([0-9a-fA-F]{64})\s+(.+)$') {
            throw "Invalid checksum entry: $line"
        }

        $expectedHash = $matches[1].ToLowerInvariant()
        $fileName = $matches[2].Trim()
        $filePath = Join-Path $resolvedBackup $fileName
        if (-not (Test-Path -LiteralPath $filePath)) {
            throw "Backup file listed in checksum is missing: $fileName"
        }

        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $filePath).Hash.ToLowerInvariant()
        if ($actualHash -ne $expectedHash) {
            throw "Backup checksum mismatch: $fileName"
        }
    }
}

Push-Location $packageRoot
try {
    & docker compose --env-file $envPath -f $composePath up -d db
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to start the database container.'
    }

    $databaseReady = $false
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        & docker compose --env-file $envPath -f $composePath exec -T db sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' *> $null
        if ($LASTEXITCODE -eq 0) {
            $databaseReady = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $databaseReady) {
        throw 'Database did not become ready in time.'
    }

    & docker compose --env-file $envPath -f $composePath cp $databaseBackup db:/tmp/giic-restore.sql
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to copy the database backup into the container.'
    }

    & docker compose --env-file $envPath -f $composePath exec -T db sh -c 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /tmp/giic-restore.sql'
    if ($LASTEXITCODE -ne 0) {
        throw 'Database restore failed.'
    }
    & docker compose --env-file $envPath -f $composePath exec -T db rm -f /tmp/giic-restore.sql | Out-Null

    & docker compose --env-file $envPath -f $composePath up -d backend
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to start the backend container.'
    }

    if (Test-Path -LiteralPath $uploadsBackup) {
        & docker compose --env-file $envPath -f $composePath cp $uploadsBackup backend:/tmp/uploads.tar.gz
        if ($LASTEXITCODE -ne 0) {
            throw 'Unable to copy the uploaded-image backup into the container.'
        }
        & docker compose --env-file $envPath -f $composePath exec -T backend tar -xzf /tmp/uploads.tar.gz -C /app/uploads
        if ($LASTEXITCODE -ne 0) {
            throw 'Uploaded-image restore failed.'
        }
        & docker compose --env-file $envPath -f $composePath exec -T --user root backend rm -f /tmp/uploads.tar.gz | Out-Null
    }

    & docker compose --env-file $envPath -f $composePath restart backend
}
finally {
    Pop-Location
}

Write-Host 'Restore completed. Run Start.ps1 to start or rebuild the complete system.'
