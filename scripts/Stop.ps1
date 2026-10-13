# 停止由后台启动脚本记录的本项目进程。
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $projectRoot '.local\platform.pid'
if (!(Test-Path -LiteralPath $pidFile)) { Write-Host 'No background platform process is recorded.'; return }
$platformProcessId = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
# 停止前读取进程信息，防止旧 PID 被其他应用复用。
$processInfo = Get-CimInstance Win32_Process -Filter ('ProcessId = ' + $platformProcessId)
$expectedJar = Join-Path $projectRoot 'target\lesaire-platform-1.0.0.jar'
if ($processInfo) {
# 确认进程命令行包含本项目 JAR 路径后再停止。
    if (!$processInfo.CommandLine.Contains($expectedJar)) { throw 'The recorded PID belongs to another process. No process was stopped.' }
    Stop-Process -Id $platformProcessId
}
Remove-Item -LiteralPath $pidFile
Write-Host 'Platform stopped.'
