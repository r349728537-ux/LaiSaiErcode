# 使用独立配置启动本项目的 Nginx 本机入口。
param([string]$NginxExe = 'D:\nginx\nginx-1.22.0-web\nginx.exe')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = (Join-Path $projectRoot 'deploy\nginx\lesaire.conf').Replace('\', '/')
$prefixPath = $projectRoot.Replace('\', '/') + '/'
$runtimeDirectory = Join-Path $projectRoot '.local\nginx'
$errorLogPath = (Join-Path $runtimeDirectory 'error.log').Replace('\', '/')
$pidFile = Join-Path $runtimeDirectory 'nginx.pid'
if (!(Test-Path -LiteralPath $NginxExe)) { throw ('Nginx not found: ' + $NginxExe + '. Supply -NginxExe with the installed path.') }
$nginxPath = (Resolve-Path -LiteralPath $NginxExe).Path
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null

# 同一入口已经启动时直接返回；旧 PID 不能指向其他应用。
if (Test-Path -LiteralPath $pidFile) {
    $nginxProcessId = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
    $processInfo = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $nginxProcessId)
    if ($processInfo) {
        if ($processInfo.ExecutablePath -ne $nginxPath -or !$processInfo.CommandLine.Contains($configPath)) { throw 'The recorded PID belongs to another process. No process was changed.' }
        Write-Host 'Lesaire Nginx is already running: http://localhost/#home'
        return
    }
    Remove-Item -LiteralPath $pidFile
}
if (Get-NetTCPConnection -LocalPort 80 -State Listen -ErrorAction SilentlyContinue) { throw 'Port 80 is already in use. Check the existing listener before starting this proxy.' }

# 已在 IDEA 运行时复用 8080；没有监听时启动现有项目 JAR。
if (!(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue)) { & (Join-Path $PSScriptRoot 'Start.ps1') }
$catalog = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/catalog' -TimeoutSec 5
if ($null -eq $catalog.products -or $null -eq $catalog.fabrics) { throw 'Port 8080 is not serving the expected Lesaire catalog.' }

# 先检查配置，再隐藏启动；-p/-c 使用完整路径，避免读取其他项目的配置。
& $nginxPath -p $prefixPath -c $configPath -e $errorLogPath -t
if ($LASTEXITCODE -ne 0) { throw 'Nginx configuration check failed. See .local/nginx/error.log.' }
$arguments = @('-p', ('"' + $prefixPath + '"'), '-c', ('"' + $configPath + '"'), '-e', ('"' + $errorLogPath + '"'))
$proxyProcess = Start-Process -FilePath $nginxPath -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDirectory 'startup.log') -RedirectStandardError (Join-Path $runtimeDirectory 'startup-error.log')
for ($attempt = 0; $attempt -lt 20; $attempt++) {
    Start-Sleep -Milliseconds 250
    $proxyProcess.Refresh()
    if ($proxyProcess.HasExited) { throw 'Nginx exited. See .local/nginx/error.log and startup-error.log.' }
    try {
        $response = Invoke-WebRequest -Uri 'http://localhost/api/catalog' -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) {
            Write-Host 'Lesaire Nginx started: http://localhost/#home -> http://127.0.0.1:8080'
            return
        }
    } catch { }
}
throw 'Nginx readiness check did not finish. See .local/nginx/error.log.'
