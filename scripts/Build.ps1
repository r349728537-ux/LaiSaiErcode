# 构建入口：准备前端依赖，检查类型，构建静态资源，再运行 Java 测试并打包。
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
# 优先使用已安装 pnpm，其次使用随运行环境提供的 pnpm。
$pnpmCommand = Get-Command pnpm.cmd -ErrorAction SilentlyContinue
$bundledPnpm = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\bin\fallback\pnpm.cmd'
Push-Location (Join-Path $projectRoot 'frontend')
try {
    if ($pnpmCommand) { & $pnpmCommand.Source install --frozen-lockfile --ignore-scripts }
    elseif (Test-Path -LiteralPath $bundledPnpm) { & $bundledPnpm install --frozen-lockfile --ignore-scripts }
    else { & npm.cmd install --ignore-scripts --cache .npm-cache }
    if ($LASTEXITCODE -ne 0) { throw 'Frontend dependency installation failed.' }
# 先检查 TypeScript 和 Vue 类型，再进行 Vite 构建。
    & node node_modules/vue-tsc/bin/vue-tsc.js -b
    if ($LASTEXITCODE -ne 0) { throw 'TypeScript validation failed.' }
    & node node_modules/vite/bin/vite.js build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
} finally { Pop-Location }
Push-Location $projectRoot
try {
# 前端产物完成后执行后端测试，并将静态文件一起打包。
    & mvn.cmd clean package
    if ($LASTEXITCODE -ne 0) { throw 'Java tests or packaging failed.' }
    Write-Host 'Build complete: target/lesaire-platform-1.0.0.jar'
} finally { Pop-Location }
