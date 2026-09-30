[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$packageRoot = Split-Path -Parent $PSScriptRoot
$examplePath = Join-Path $packageRoot '.env.example'
$envPath = Join-Path $packageRoot '.env'

if (Test-Path -LiteralPath $envPath) {
    Write-Host "Environment file already exists: $envPath"
    exit 0
}

function New-HexSecret {
    param([int]$ByteCount)

    $bytes = New-Object byte[] $ByteCount
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }

    return ([System.BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
}

$content = Get-Content -Raw -LiteralPath $examplePath
$content = $content.Replace('replace-with-a-random-root-password', (New-HexSecret -ByteCount 24))
$content = $content.Replace('replace-with-a-random-database-password', (New-HexSecret -ByteCount 24))
$content = $content.Replace('replace-with-a-random-64-character-secret', (New-HexSecret -ByteCount 32))

[System.IO.File]::WriteAllText($envPath, $content, [System.Text.UTF8Encoding]::new($false))

Write-Host "Created $envPath with random database and JWT secrets."
Write-Host 'Set APP_PUBLIC_URL, email, and Gemini values before production use.'
