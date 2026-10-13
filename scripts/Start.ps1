# 后台启动入口，保存本项目进程 ID，并把标准输出和错误写入本地日志。
param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'target\lesaire-platform-1.0.0.jar'
if ($Build -or !(Test-Path -LiteralPath $jarPath)) { & (Join-Path $PSScriptRoot 'Build.ps1') }
# 启动前检查 8080 端口，避免与 IDEA 中运行的应用冲突。
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { throw 'Port 8080 is already in use. Stop the existing IDEA or platform process first.' }
$runtimeDirectory = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $runtimeDirectory -Force | Out-Null
$javaExecutable = (Get-Command java.exe).Source
# 隐藏启动 Java 进程，使用 UTF-8 编码并记录日志。
$appProcess = Start-Process -FilePath $javaExecutable -ArgumentList @('-Dstdout.encoding=UTF-8','-Dfile.encoding=UTF-8','-jar',('"' + $jarPath + '"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeDirectory 'platform.log') -RedirectStandardError (Join-Path $runtimeDirectory 'platform-error.log')
Set-Content -LiteralPath (Join-Path $runtimeDirectory 'platform.pid') -Value $appProcess.Id
# 轮询目录接口确认应用可用，同时检查进程是否提前退出。
for ($attempt = 0; $attempt -lt 40; $attempt++) {
    Start-Sleep -Milliseconds 500
    $appProcess.Refresh()
    if ($appProcess.HasExited) { throw ('Application exited. See ' + (Join-Path $runtimeDirectory 'platform-error.log')) }
    try {
        $response = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/api/catalog' -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -eq 200) { Write-Host ('Started PID ' + $appProcess.Id + ': http://127.0.0.1:8080/'); return }
    } catch { }
}
throw 'Startup did not finish within the expected time. Check .local/platform.log.'
