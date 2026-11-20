param([string]$Php='C:\xampp\php\php.exe')
$ErrorActionPreference='Stop'
Write-Host 'Sending queued FCM notifications. Configure fcm_service_account in api/config.local.php first.'
while($true){
    & $Php (Join-Path $PSScriptRoot 'dispatch-notifications.php')
    if($LASTEXITCODE -ne 0){throw 'Notification dispatch failed; fix the reported configuration or connection error.'}
    Start-Sleep -Seconds 5
}
