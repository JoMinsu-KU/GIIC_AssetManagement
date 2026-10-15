[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $packageRoot '.env'
$composePath = Join-Path $packageRoot 'compose.yml'
$proxyRoot = Join-Path $packageRoot 'host-proxy'
$caddyfile = Join-Path $proxyRoot 'Caddyfile'
$binaryDirectory = Join-Path $proxyRoot 'bin'
$caddy = Join-Path $binaryDirectory 'caddy.exe'
$taskName = 'GIIC Asset Host Proxy'
$version = '2.11.4'
$archiveName = "caddy_${version}_windows_amd64.zip"
$expectedSha512 = 'cd5ccfd86a4b40732cf715890d0dca5bf3f63adefec5a7914de85adf240c60ce7e5d2791631b88ef9758e46b23bb1730e020b9c5d696889740b284ffd4788e35'

if (-not (Test-Path -LiteralPath $envPath)) {
    throw "Deployment .env is missing: $envPath"
}
if (-not (Test-Path -LiteralPath $caddyfile)) {
    throw "Host proxy configuration is missing: $caddyfile"
}
& docker info *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker Desktop is unavailable.'
}

New-Item -ItemType Directory -Path $binaryDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath $caddy)) {
    $archive = Join-Path $binaryDirectory $archiveName
    $download = "https://github.com/caddyserver/caddy/releases/download/v$version/$archiveName"
    Invoke-WebRequest -Uri $download -OutFile $archive
    if ((Get-FileHash -Algorithm SHA512 -LiteralPath $archive).Hash.ToLowerInvariant() -ne $expectedSha512) {
        throw 'The Caddy release archive failed its SHA-512 check.'
    }
    Expand-Archive -LiteralPath $archive -DestinationPath $binaryDirectory -Force
    if (-not (Test-Path -LiteralPath $caddy)) {
        throw 'The Caddy archive did not contain caddy.exe.'
    }
    Remove-Item -LiteralPath $archive -Force
}

& $caddy validate --config $caddyfile --adapter caddyfile
if ($LASTEXITCODE -ne 0) {
    throw 'Caddy configuration validation failed.'
}

$existingTask = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue

function Register-HostProxyTask {
    $account = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
    $action = New-ScheduledTaskAction -Execute $caddy -Argument ('run --config "{0}" --adapter caddyfile' -f $caddyfile) -WorkingDirectory $proxyRoot
    $trigger = New-ScheduledTaskTrigger -AtLogOn -User $account
    $principal = New-ScheduledTaskPrincipal -UserId $account -LogonType Interactive -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([timespan]::Zero)
    Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Accept GIIC client connections on Windows and forward their real peer IP to Docker.' -Force | Out-Null
}

$envContent = [System.IO.File]::ReadAllText($envPath, [System.Text.Encoding]::UTF8)
$alreadyConfigured = $envContent -match '(?m)^APP_CLIENT_IP_HOST_PROXY_ENABLED=true\s*$' -and
    $envContent -match '(?m)^FRONTEND_BIND_ADDRESS=127\.0\.0\.1\s*$' -and
    $envContent -match '(?m)^FRONTEND_PORT=3001\s*$' -and
    $envContent -match '(?m)^NGINX_CONFIG_PATH=\./nginx/host-proxy\.conf\s*$'

if (-not $alreadyConfigured) {
    $backup = Join-Path $packageRoot ('backups/.env.before-host-proxy.' + (Get-Date -Format 'yyyyMMddHHmmss'))
    New-Item -ItemType Directory -Path (Split-Path -Parent $backup) -Force | Out-Null
    Copy-Item -LiteralPath $envPath -Destination $backup
    $values = [ordered]@{
        FRONTEND_BIND_ADDRESS = '127.0.0.1'
        FRONTEND_PORT = '3001'
        NGINX_CONFIG_PATH = './nginx/host-proxy.conf'
        APP_CLIENT_IP_HOST_PROXY_ENABLED = 'true'
    }
    foreach ($key in $values.Keys) {
        $line = "$key=$($values[$key])"
        if ($envContent -match "(?m)^$key=") {
            $envContent = [regex]::Replace($envContent, "(?m)^$key=.*$", $line)
        } else {
            $envContent = $envContent.TrimEnd("`r", "`n") + "`r`n$line`r`n"
        }
    }

    Write-Host 'Building the backend before changing the public port.'
    & docker compose --env-file $envPath -f $composePath build backend
    if ($LASTEXITCODE -ne 0) {
        throw 'Backend build failed; the current deployment was not changed.'
    }

    Register-HostProxyTask
    $startedCaddy = $null
    try {
        [System.IO.File]::WriteAllText($envPath, $envContent, [System.Text.UTF8Encoding]::new($false))
        & docker compose --env-file $envPath -f $composePath config --quiet
        if ($LASTEXITCODE -ne 0) { throw 'The updated Docker Compose configuration is invalid.' }
        & docker compose --env-file $envPath -f $composePath up -d --no-deps --force-recreate backend frontend
        if ($LASTEXITCODE -ne 0) { throw 'The Docker services could not be recreated.' }
        $startedCaddy = Start-Process -FilePath $caddy -ArgumentList @('run', '--config', "`"$caddyfile`"", '--adapter', 'caddyfile') -WorkingDirectory $proxyRoot -WindowStyle Hidden -PassThru
        $healthy = $false
        for ($attempt = 0; $attempt -lt 30; $attempt++) {
            try {
                $response = Invoke-WebRequest -Uri 'http://127.0.0.1:3000/healthz' -TimeoutSec 2 -UseBasicParsing
                if ($response.StatusCode -eq 200) { $healthy = $true; break }
            } catch { Start-Sleep -Seconds 1 }
        }
        if (-not $healthy) { throw 'The Windows host proxy did not pass its health check.' }
    }
    catch {
        if ($startedCaddy -and -not $startedCaddy.HasExited) {
            Stop-Process -Id $startedCaddy.Id -Force
        }
        Copy-Item -LiteralPath $backup -Destination $envPath -Force
        & docker compose --env-file $envPath -f $composePath up -d --no-deps --force-recreate backend frontend
        if (-not $existingTask) { Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue }
        throw
    }
}
else {
    Register-HostProxyTask
    $running = Get-CimInstance Win32_Process -Filter "Name = 'caddy.exe'" | Where-Object { $_.CommandLine -like "*$caddyfile*" }
    if (-not $running) {
        Start-Process -FilePath $caddy -ArgumentList @('run', '--config', "`"$caddyfile`"", '--adapter', 'caddyfile') -WorkingDirectory $proxyRoot -WindowStyle Hidden | Out-Null
    }
}

$healthy = $false
for ($attempt = 0; $attempt -lt 10; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri 'http://127.0.0.1:3000/healthz' -TimeoutSec 2 -UseBasicParsing
        if ($response.StatusCode -eq 200) { $healthy = $true; break }
    } catch { Start-Sleep -Seconds 1 }
}
if (-not $healthy) {
    throw 'The Windows host proxy is configured but is not serving requests.'
}

Write-Host 'Real client IP proxy is active on http://localhost:3000.'
Write-Host 'Docker Nginx is bound to 127.0.0.1:3001; the public port is handled by Windows Caddy.'
Write-Host "Startup task: $taskName"
