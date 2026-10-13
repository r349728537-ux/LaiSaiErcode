# 前台运行入口，可通过 Build 参数或缺少 JAR 的情况触发构建。
param([switch]$Build)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $projectRoot 'target\lesaire-platform-1.0.0.jar'
if ($Build -or !(Test-Path -LiteralPath $jarPath)) { & (Join-Path $PSScriptRoot 'Build.ps1') }
# 以项目根目录作为工作目录，使本地配置和数据路径解析正确。
Push-Location $projectRoot
try {
    Write-Host 'Open http://127.0.0.1:8080/ after the application starts. Press Ctrl+C to stop.'
    & java -jar $jarPath
} finally { Pop-Location }
