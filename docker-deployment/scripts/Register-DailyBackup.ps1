[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$DestinationDirectory,

    [ValidatePattern('^([01][0-9]|2[0-3]):[0-5][0-9]$')]
    [string]$Time = '04:00',

    [string]$TaskName = 'GIIC Asset Daily Backup'
)

$ErrorActionPreference = 'Stop'
$backupScript = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot 'Backup-Scheduled.ps1')).Path
New-Item -ItemType Directory -Path $DestinationDirectory -Force | Out-Null
$destination = (Resolve-Path -LiteralPath $DestinationDirectory).Path
$account = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$runAt = [datetime]::Today.Add([timespan]::ParseExact($Time, 'hh\:mm', [System.Globalization.CultureInfo]::InvariantCulture))
$arguments = '-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "{0}" -DestinationDirectory "{1}"' -f $backupScript, $destination

$action = New-ScheduledTaskAction -Execute "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -Argument $arguments
$trigger = New-ScheduledTaskTrigger -Daily -At $runAt
$principal = New-ScheduledTaskPrincipal -UserId $account -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -WakeToRun -MultipleInstances IgnoreNew -ExecutionTimeLimit (New-TimeSpan -Hours 4)

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Principal $principal -Settings $settings -Description 'Back up the complete GIIC MySQL database and uploaded asset images to a dated archive.' -Force | Out-Null

$task = Get-ScheduledTask -TaskName $TaskName
$info = Get-ScheduledTaskInfo -TaskName $TaskName
[pscustomobject]@{
    TaskName = $task.TaskName
    Account = $task.Principal.UserId
    Destination = $destination
    DailyAt = $Time
    NextRunTime = $info.NextRunTime
    State = $task.State
}
