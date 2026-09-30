[CmdletBinding()]
param(
    [string]$DatabaseContainer = 'asset_management_db'
)

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $packageRoot
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $packageRoot "backups\legacy-$timestamp"
$uploadRoot = Join-Path $repositoryRoot 'uploads'

New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null

$runningContainer = & docker ps --filter "name=^/$DatabaseContainer$" --format '{{.Names}}'
if ($LASTEXITCODE -ne 0 -or $runningContainer -ne $DatabaseContainer) {
    throw "Running database container not found: $DatabaseContainer"
}

& docker exec $DatabaseContainer sh -c 'mysqldump --default-character-set=utf8mb4 --single-transaction --routines --triggers --events -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" > /tmp/giic-current.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Current database export failed.'
}

& docker cp "${DatabaseContainer}:/tmp/giic-current.sql" (Join-Path $backupRoot 'database.sql')
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to copy the current database export.'
}
& docker exec $DatabaseContainer rm -f /tmp/giic-current.sql | Out-Null

if (Test-Path -LiteralPath $uploadRoot) {
    & tar -czf (Join-Path $backupRoot 'uploads.tar.gz') -C $uploadRoot .
    if ($LASTEXITCODE -ne 0) {
        throw 'Current uploaded-image export failed.'
    }
}

$sourceCommit = (& git -C $repositoryRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or -not $sourceCommit) {
    throw 'Unable to record the source commit for this backup.'
}
[System.IO.File]::WriteAllText(
    (Join-Path $backupRoot 'source-commit.txt'),
    "$sourceCommit`n",
    [System.Text.UTF8Encoding]::new($false)
)

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

Write-Host "Current database and uploads exported: $backupRoot"
Write-Host 'The deployment .env and backups are ignored by Git. Transfer and store this folder securely.'
