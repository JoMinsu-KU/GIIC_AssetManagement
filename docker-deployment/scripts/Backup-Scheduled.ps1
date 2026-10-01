[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$DestinationDirectory,

    [ValidateRange(1, 10)]
    [int]$MaxAttempts = 3
)

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'
$stagingParent = Join-Path $packageRoot 'backups'
$timestamp = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'
$dateFolderName = $timestamp.Substring(0, 10)
$archiveName = "GIIC_Asset_$timestamp.tar.gz"
$logName = "GIIC_Asset_$timestamp.log"

if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Deployment environment file is missing: $envPath"
}
if (-not (Test-Path -LiteralPath $composePath)) {
    throw "Docker Compose file is missing: $composePath"
}

New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $stagingParent -Force | Out-Null
$destination = (Resolve-Path -LiteralPath $DestinationDirectory).Path
$stagingParent = (Resolve-Path -LiteralPath $stagingParent).Path
$dailyFolder = Join-Path $destination $dateFolderName
New-Item -ItemType Directory -Path $dailyFolder -Force | Out-Null
$dailyFolder = (Resolve-Path -LiteralPath $dailyFolder).Path
$archivePath = Join-Path $dailyFolder $archiveName
$partialPath = "$archivePath.partial"
$logPath = Join-Path $dailyFolder $logName
$lockPath = Join-Path $destination '.giic-backup.lock'
$lockStream = $null
$stage = $null
$completed = $false

function Write-BackupLog {
    param([string]$Message)
    $line = '[{0}] {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Host $line
    [System.IO.File]::AppendAllText($logPath, "$line`r`n", [System.Text.UTF8Encoding]::new($false))
}

function Remove-StagingDirectory {
    param([string]$Path)
    if (-not $Path -or -not (Test-Path -LiteralPath $Path)) {
        return
    }
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $fullParent = [System.IO.Path]::GetFullPath($stagingParent).TrimEnd('\', '/')
    if (-not $fullPath.StartsWith("$fullParent\", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the backup staging folder: $fullPath"
    }
    Remove-Item -LiteralPath $fullPath -Recurse -Force
}

try {
    $lockStream = [System.IO.File]::Open(
        $lockPath,
        [System.IO.FileMode]::OpenOrCreate,
        [System.IO.FileAccess]::ReadWrite,
        [System.IO.FileShare]::None
    )

    & docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker Desktop is unavailable.'
    }

    $compose = @('compose', '--env-file', $envPath, '-f', $composePath)
    foreach ($service in @('db', 'backend')) {
        $containerId = & docker @compose ps -q $service
        if ($LASTEXITCODE -ne 0 -or -not $containerId) {
            throw "The $service service is not running."
        }
    }

    Write-BackupLog "Starting online backup to $archivePath"
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $stageName = 'auto-{0}-{1}' -f ($timestamp -replace '[^0-9]', ''), $attempt
        $stage = Join-Path $stagingParent $stageName
        Remove-StagingDirectory -Path $stage
        New-Item -ItemType Directory -Path $stage | Out-Null
        $sqlPath = Join-Path $stage 'database.sql'
        $uploadsPath = Join-Path $stage 'uploads.tar.gz'

        Write-BackupLog "Attempt $attempt of ${MaxAttempts}: exporting the complete MySQL database."
        $dumpCommand = 'mysqldump --default-character-set=utf8mb4 --single-transaction --routines --triggers --events -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" > /backups/{0}/database.sql' -f $stageName
        & docker @compose exec -T db sh -c $dumpCommand
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $sqlPath) -or (Get-Item -LiteralPath $sqlPath).Length -eq 0) {
            throw 'The MySQL export failed or produced an empty file.'
        }

        Write-BackupLog 'Archiving the complete uploads volume without stopping the application.'
        & docker @compose exec -T backend tar -czf "/backups/$stageName/uploads.tar.gz" -C /app/uploads .
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $uploadsPath) -or (Get-Item -LiteralPath $uploadsPath).Length -eq 0) {
            throw 'The upload archive failed or produced an empty file.'
        }

        $archiveEntries = @(& tar.exe -tzf $uploadsPath 2>$null)
        if ($LASTEXITCODE -ne 0) {
            throw 'The upload archive could not be read back.'
        }
        $archivedNames = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        foreach ($entry in $archiveEntries) {
            if ($entry.EndsWith('/')) { continue }
            [void]$archivedNames.Add(($entry -split '/')[-1])
        }

        $sqlText = [System.IO.File]::ReadAllText($sqlPath, [System.Text.Encoding]::UTF8)
        if (-not $sqlText.Contains('CREATE TABLE `assets`') -or -not $sqlText.Contains('CREATE TABLE `asset_images`')) {
            throw 'The database export does not contain the asset tables.'
        }
        $referencedNames = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
        $imageInserts = [regex]::Matches($sqlText, '(?m)^INSERT INTO `asset_images` VALUES .+$')
        foreach ($insert in $imageInserts) {
            foreach ($reference in [regex]::Matches($insert.Value, "'/uploads/([^']+)'")) {
                [void]$referencedNames.Add(($reference.Groups[1].Value -split '/')[-1])
            }
        }

        $missing = @($referencedNames | Where-Object { -not $archivedNames.Contains($_) })
        if ($missing.Count -gt 0) {
            Write-BackupLog "The database references $($missing.Count) image files absent from the archive; retrying."
            if ($attempt -eq $MaxAttempts) {
                throw 'The database and uploads could not be captured consistently while the application was online.'
            }
            Remove-StagingDirectory -Path $stage
            $stage = $null
            continue
        }

        $manifest = [ordered]@{
            createdAt = (Get-Date).ToString('o')
            mode = 'online'
            database = 'complete MySQL logical dump'
            uploadedFiles = $archivedNames.Count
            referencedImageFiles = $referencedNames.Count
            missingReferencedImages = 0
            note = 'The database is transaction-consistent. The image archive may include extra files added during backup.'
        }
        $manifest | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stage 'manifest.json') -Encoding UTF8
        $hashLines = @('database.sql', 'uploads.tar.gz') | ForEach-Object {
            $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath (Join-Path $stage $_)).Hash.ToLowerInvariant()
            "$hash  $_"
        }
        [System.IO.File]::WriteAllLines(
            (Join-Path $stage 'SHA256SUMS.txt'),
            $hashLines,
            [System.Text.UTF8Encoding]::new($false)
        )

        Write-BackupLog "Validated $($referencedNames.Count) referenced images; packaging $($archivedNames.Count) upload files."
        & tar.exe -czf $partialPath -C $stage database.sql uploads.tar.gz SHA256SUMS.txt manifest.json
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not create the final archive.'
        }
        $outerEntries = @(& tar.exe -tzf $partialPath 2>$null)
        if ($LASTEXITCODE -ne 0 -or $outerEntries.Count -ne 4) {
            throw 'The final archive failed its read-back check.'
        }
        Move-Item -LiteralPath $partialPath -Destination $archivePath
        $archiveHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $archivePath).Hash.ToLowerInvariant()
        [System.IO.File]::WriteAllText(
            "$archivePath.sha256",
            "$archiveHash  $archiveName`n",
            [System.Text.UTF8Encoding]::new($false)
        )
        $completed = $true
        Write-BackupLog "Backup complete: $archivePath"
        break
    }
}
catch {
    Write-BackupLog "BACKUP FAILED: $($_.Exception.Message)"
    throw
}
finally {
    if ($stage) {
        Remove-StagingDirectory -Path $stage
    }
    if (-not $completed -and (Test-Path -LiteralPath $partialPath)) {
        Remove-Item -LiteralPath $partialPath -Force
    }
    if ($lockStream) {
        $lockStream.Dispose()
    }
}
