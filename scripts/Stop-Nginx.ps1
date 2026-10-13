# 只停止本项目独立 PID 记录的 Nginx，不停止其他 Nginx 或 Spring Boot。
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = (Join-Path $projectRoot 'deploy\nginx\lesaire.conf').Replace('\', '/')
$prefixPath = $projectRoot.Replace('\', '/') + '/'
$pidFile = Join-Path $projectRoot '.local\nginx\nginx.pid'
$errorLogPath = (Join-Path $projectRoot '.local\nginx\error.log').Replace('\', '/')
if (!(Test-Path -LiteralPath $pidFile)) { Write-Host 'No Lesaire Nginx process is recorded.'; return }
$nginxProcessId = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
$processInfo = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $nginxProcessId)
if ($processInfo) {
    if ($null -eq $processInfo.CommandLine -or !$processInfo.CommandLine.Contains($configPath) -or $processInfo.Name -ne 'nginx.exe') { throw 'The recorded PID belongs to another process. No process was stopped.' }
    & $processInfo.ExecutablePath -p $prefixPath -c $configPath -e $errorLogPath -s stop
    if ($LASTEXITCODE -ne 0) { throw 'Nginx stop failed. See .local/nginx/error.log.' }
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        if (!(Get-Process -Id $nginxProcessId -ErrorAction SilentlyContinue)) { break }
        Start-Sleep -Milliseconds 250
    }
    if (Get-Process -Id $nginxProcessId -ErrorAction SilentlyContinue) { throw 'Nginx is still running. Its PID record has been kept.' }
}
if (Test-Path -LiteralPath $pidFile) { Remove-Item -LiteralPath $pidFile }
Write-Host 'Lesaire Nginx stopped.'
