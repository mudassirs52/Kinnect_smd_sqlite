param([string]$Xampp='C:\xampp',[int]$DbPort=3307,[int]$ApiPort=8088)
$ErrorActionPreference='Stop'
$projectDir=Split-Path $PSScriptRoot -Parent
$runtimeDir=Join-Path $projectDir '.runtime'
$dataDir=Join-Path $runtimeDir 'mysql'
$uploadTemp=Join-Path $runtimeDir 'uploads'
New-Item -ItemType Directory -Force $runtimeDir,$uploadTemp | Out-Null
$mysql=Join-Path $Xampp 'mysql\bin\mysql.exe'
$php=Join-Path $Xampp 'php\php.exe'
if(!(Test-Path -LiteralPath $mysql) -or !(Test-Path -LiteralPath $php)){throw 'Set -Xampp to your XAMPP installation directory.'}
& $mysql -h 127.0.0.1 -P $DbPort -u root --connect-timeout=2 -e 'SELECT 1' 2>$null | Out-Null
if($LASTEXITCODE -ne 0){
    if(!(Test-Path -LiteralPath (Join-Path $dataDir 'mysql'))){
        & (Join-Path $Xampp 'mysql\bin\mysql_install_db.exe') "--datadir=$dataDir"
        if($LASTEXITCODE -ne 0){throw 'Could not initialize the separate demo database.'}
    }
    Start-Process -FilePath (Join-Path $Xampp 'mysql\bin\mysqld.exe') -ArgumentList @('--no-defaults',"--datadir=`"$dataDir`"","--port=$DbPort",'--bind-address=127.0.0.1') -WindowStyle Hidden
    $ready=$false
    for($attempt=0;$attempt -lt 30;$attempt++){
        & $mysql -h 127.0.0.1 -P $DbPort -u root --connect-timeout=1 -e 'SELECT 1' 2>$null | Out-Null
        if($LASTEXITCODE -eq 0){$ready=$true;break};Start-Sleep -Seconds 1
    }
    if(!$ready){throw 'Demo MySQL did not start. Check .runtime/mysql/*.err.'}
}
$schema=(Join-Path $PSScriptRoot 'schema.sql').Replace('\','/')
& $mysql -h 127.0.0.1 -P $DbPort -u root -e "source $schema"
if($LASTEXITCODE -ne 0){throw 'Could not import schema.sql.'}
$configPath=Join-Path $projectDir 'api\config.local.php'
if(!(Test-Path -LiteralPath $configPath)){
    "<?php return array_replace(require __DIR__.'/config.example.php', ['dsn'=>'mysql:host=127.0.0.1;port=$DbPort;dbname=kinnect_i230507_i230017;charset=utf8mb4']);" | Set-Content -LiteralPath $configPath -Encoding utf8
}
Write-Host "Kinnect API: http://localhost:$ApiPort/health.php"
Write-Host "Android emulator URL: http://10.0.2.2:$ApiPort/"
Write-Host 'Keep this window open. This uses a separate MySQL instance and leaves XAMPP databases unchanged.'
& $php -d display_errors=0 -d log_errors=1 "-d upload_tmp_dir=$uploadTemp" -d upload_max_filesize=25M -d post_max_size=260M -S "0.0.0.0:$ApiPort" -t (Join-Path $projectDir 'api')
