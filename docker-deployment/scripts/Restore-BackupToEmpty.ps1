[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupArchive,

    [ValidatePattern('^[a-z0-9][a-z0-9_-]*$')]
    [string]$ProjectName,

    [switch]$SkipApplicationStart
)

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'
$seedIdPath = Join-Path $packageRoot 'current-data\seed-id.txt'
$stagingParent = Join-Path $packageRoot 'backups'

foreach ($path in @($envPath, $composePath, $seedIdPath)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required deployment file is missing: $path"
    }
}

if (-not $ProjectName) {
    $projectLine = Get-Content -LiteralPath $envPath | Where-Object { $_ -match '^COMPOSE_PROJECT_NAME=' } | Select-Object -First 1
    $ProjectName = if ($projectLine) { ($projectLine -split '=', 2)[1].Trim() } else { 'giic_asset' }
}
if ($ProjectName -notmatch '^[a-z0-9][a-z0-9_-]*$') {
    throw "Invalid Docker Compose project name: $ProjectName"
}

$archive = (Resolve-Path -LiteralPath $BackupArchive).Path
$databaseVolume = "${ProjectName}_mysql-data"
$uploadsVolume = "${ProjectName}_uploads-data"
$temporaryDatabase = 'giic-restore-db-' + [guid]::NewGuid().ToString('N').Substring(0, 8)
$stagingName = 'restore-' + [guid]::NewGuid().ToString('N')
New-Item -ItemType Directory -Path $stagingParent -Force | Out-Null
$stagingParent = (Resolve-Path -LiteralPath $stagingParent).Path
$stage = Join-Path $stagingParent $stagingName
$databaseContainerStarted = $false

function Invoke-DockerChecked {
    param([string[]]$Arguments, [string]$FailureMessage)
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

try {
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Desktop is unavailable.'
    }

    $existingVolumes = @(& docker volume ls --format '{{.Name}}')
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not inspect Docker volumes.'
    }
    foreach ($volume in @($databaseVolume, $uploadsVolume)) {
        if ($existingVolumes -contains $volume) {
            throw "Target volume already exists; no data was overwritten: $volume"
        }
    }
    $existingContainers = @(& docker ps -a --filter "label=com.docker.compose.project=$ProjectName" --format '{{.ID}}')
    if ($LASTEXITCODE -ne 0 -or $existingContainers.Count -gt 0) {
        throw "Containers already exist for project $ProjectName; refusing to restore into it."
    }

    $outerEntries = @(& tar.exe -tzf $archive 2>$null)
    $expectedEntries = @('database.sql', 'uploads.tar.gz', 'SHA256SUMS.txt', 'manifest.json')
    if ($LASTEXITCODE -ne 0 -or (Compare-Object $expectedEntries $outerEntries)) {
        throw 'The backup archive is unreadable or does not have the expected contents.'
    }
    $outerChecksum = "$archive.sha256"
    if (Test-Path -LiteralPath $outerChecksum) {
        $checksumLine = (Get-Content -LiteralPath $outerChecksum | Select-Object -First 1)
        if ($checksumLine -notmatch '^([0-9a-fA-F]{64})\s+') {
            throw 'The archive checksum file is invalid.'
        }
        $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash
        if ($actualHash -ne $matches[1]) {
            throw 'The backup archive checksum does not match.'
        }
    }

    New-Item -ItemType Directory -Path $stage | Out-Null
    & tar.exe -xzf $archive -C $stage
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not extract the backup archive.'
    }
    $sqlPath = Join-Path $stage 'database.sql'
    $uploadsPath = Join-Path $stage 'uploads.tar.gz'
    $checksumPath = Join-Path $stage 'SHA256SUMS.txt'
    foreach ($line in Get-Content -LiteralPath $checksumPath) {
        if ($line -notmatch '^([0-9a-fA-F]{64})\s+(database\.sql|uploads\.tar\.gz)$') {
            throw 'An internal backup checksum entry is invalid.'
        }
        $expectedHash = $matches[1]
        $filePath = Join-Path $stage $matches[2]
        if ((Get-FileHash -Algorithm SHA256 -LiteralPath $filePath).Hash -ne $expectedHash) {
            throw "An internal backup checksum does not match: $filePath"
        }
    }
    $uploadEntries = @(& tar.exe -tzf $uploadsPath 2>$null)
    if ($LASTEXITCODE -ne 0 -or $uploadEntries.Count -eq 0) {
        throw 'The uploaded-image archive is unreadable or empty.'
    }
    $backupSeedId = ((& tar.exe -xOzf $uploadsPath './.giic-seed-id' 2>$null) | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $backupSeedId) {
        throw 'The uploaded-image archive does not contain its migration marker.'
    }
    $packageSeedId = (Get-Content -Raw -LiteralPath $seedIdPath).Trim()
    if ($backupSeedId -ne $packageSeedId) {
        throw 'The backup does not match this deployment package migration marker.'
    }

    Write-Host "Backup verified. Restoring only to new volumes: $databaseVolume, $uploadsVolume"
    Invoke-DockerChecked -Arguments @('volume', 'create', '--label', "com.docker.compose.project=$ProjectName", '--label', 'com.docker.compose.volume=mysql-data', $databaseVolume) -FailureMessage 'Could not create the empty database volume.'
    Invoke-DockerChecked -Arguments @('volume', 'create', '--label', "com.docker.compose.project=$ProjectName", '--label', 'com.docker.compose.volume=uploads-data', $uploadsVolume) -FailureMessage 'Could not create the empty uploads volume.'

    Invoke-DockerChecked -Arguments @(
        'run', '-d', '--name', $temporaryDatabase,
        '--env-file', $envPath,
        '--mount', "type=volume,source=$databaseVolume,target=/var/lib/mysql",
        'mysql:8.0', '--character-set-server=utf8mb4', '--collation-server=utf8mb4_unicode_ci'
    ) -FailureMessage 'Could not start the temporary MySQL server.'
    $databaseContainerStarted = $true
    $ready = $false
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        & docker exec $temporaryDatabase sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqladmin ping -h 127.0.0.1 -uroot --silent' *> $null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        throw 'The temporary MySQL server did not become ready.'
    }

    Invoke-DockerChecked -Arguments @('cp', $sqlPath, "${temporaryDatabase}:/tmp/giic-restore.sql") -FailureMessage 'Could not copy the database dump into MySQL.'
    Invoke-DockerChecked -Arguments @('exec', $temporaryDatabase, 'sh', '-c', 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --default-character-set=utf8mb4 -uroot "$MYSQL_DATABASE" < /tmp/giic-restore.sql') -FailureMessage 'The complete database import failed.'

    Invoke-DockerChecked -Arguments @(
        'run', '--rm',
        '--mount', "type=volume,source=$uploadsVolume,target=/app/uploads",
        '--mount', "type=bind,source=$stage,target=/backup,readonly",
        'alpine:3.20', 'tar', '-xzf', '/backup/uploads.tar.gz', '-C', '/app/uploads'
    ) -FailureMessage 'The uploaded-image restore failed.'

    $seedRows = @('SELECT seed_id FROM giic_migration_seed WHERE id = 1;' | & docker exec -i $temporaryDatabase sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot -N -s $MYSQL_DATABASE')
    if ($LASTEXITCODE -ne 0 -or $seedRows.Count -ne 1 -or $seedRows[0].Trim() -ne $packageSeedId) {
        throw 'The restored database migration marker does not match this deployment package.'
    }
    $imageUrls = @('SELECT image_url FROM asset_images;' | & docker exec -i $temporaryDatabase sh -c 'MYSQL_PWD=$MYSQL_ROOT_PASSWORD mysql -uroot -N -s $MYSQL_DATABASE')
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not inspect restored image references.'
    }
    $restoredFiles = @(& docker run --rm --mount "type=volume,source=$uploadsVolume,target=/app/uploads,readonly" alpine:3.20 find /app/uploads -type f)
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not inspect restored uploaded files.'
    }
    $fileNames = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($file in $restoredFiles) {
        [void]$fileNames.Add(($file -split '/')[-1])
    }
    $missing = @($imageUrls | Where-Object { -not $fileNames.Contains(($_ -split '/')[-1]) })
    if ($missing.Count -gt 0) {
        throw "The restored database references $($missing.Count) missing uploaded files."
    }
    Write-Host "Restore verified: $($imageUrls.Count) image references and $($restoredFiles.Count) uploaded files."

    Invoke-DockerChecked -Arguments @('stop', $temporaryDatabase) -FailureMessage 'Could not stop the temporary MySQL server.'
    Invoke-DockerChecked -Arguments @('rm', $temporaryDatabase) -FailureMessage 'Could not remove the temporary MySQL container.'
    $databaseContainerStarted = $false

    if (-not $SkipApplicationStart) {
        Invoke-DockerChecked -Arguments @('compose', '-p', $ProjectName, '--env-file', $envPath, '-f', $composePath, 'up', '-d', '--build') -FailureMessage 'Data was restored, but the application did not start. The restored volumes were preserved.'
        Invoke-DockerChecked -Arguments @('compose', '-p', $ProjectName, '--env-file', $envPath, '-f', $composePath, 'ps') -FailureMessage 'The restored application status could not be read.'
    }
    else {
        Write-Host 'Application startup was skipped; restored volumes were preserved for inspection.'
    }
}
finally {
    if ($databaseContainerStarted) {
        & docker stop $temporaryDatabase *> $null
        & docker rm $temporaryDatabase *> $null
    }
    if (Test-Path -LiteralPath $stage) {
        $fullStage = [System.IO.Path]::GetFullPath($stage)
        $fullParent = [System.IO.Path]::GetFullPath($stagingParent).TrimEnd('\', '/')
        if (-not $fullStage.StartsWith("$fullParent\", [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove a directory outside the backup staging folder: $fullStage"
        }
        Remove-Item -LiteralPath $fullStage -Recurse -Force
    }
}
