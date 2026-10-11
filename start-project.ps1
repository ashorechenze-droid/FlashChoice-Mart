param(
    [switch]$SkipBuild,
    [switch]$SkipInfra,
    [switch]$DryRun,
    [string]$FrontendPath = (Join-Path $PSScriptRoot '..\FlashChoiceMart-frontend\mall-web'),
    [string]$MySqlRoot = 'D:\MySQL\mysql-9.7.1-winx64',
    [string]$NacosRoot = 'D:\Nacos\nacos',
    [string]$RedisRoot = 'D:\Redis\redis-windows-8.8.0',
    [string]$RabbitRoot = 'D:\RabbitMQ\rabbitmq_server-4.3.5',
    [string]$ErlangRoot = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$frontend = [System.IO.Path]::GetFullPath($FrontendPath)
$bundledErlang = Join-Path $projectRoot '.local\erlang27'
if (-not $ErlangRoot) {
    $ErlangRoot = if (Test-Path -LiteralPath (Join-Path $bundledErlang 'bin\erl.exe')) {
        $bundledErlang
    } else {
        'D:\RabbitMQ\Erlang OTP'
    }
}
$logDir = Join-Path $projectRoot 'runlogs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Test-Port([int]$port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync('127.0.0.1', $port)
        return $task.Wait(1200) -and $client.Connected
    } catch { return $false } finally { $client.Dispose() }
}

function Wait-Port([string]$name, [int]$port, [int]$seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        if (Test-Port $port) { Write-Host "$name 已就绪 ($port)"; return }
        Start-Sleep -Seconds 1
    }
    throw "$name 启动超时（端口 $port）。请查看 $logDir 中的日志。"
}

$started = @()
function Start-Logged([string]$name, [string]$file, [string[]]$arguments, [string]$cwd, [int]$port) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "$name 启动文件不存在：$file" }
    if ($DryRun) { Write-Host "将启动 $name：$file $($arguments -join ' ')"; return }
    $process = Start-Process -FilePath $file -ArgumentList $arguments -WorkingDirectory $cwd `
        -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDir "$name.out.log") `
        -RedirectStandardError (Join-Path $logDir "$name.err.log")
    $script:started += [pscustomobject]@{ Name=$name; Pid=$process.Id; Port=$port }
    Write-Host "已启动 $name (PID $($process.Id))"
}

foreach ($name in @('java', 'npm.cmd')) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "未找到 $name，请先安装 JDK 25 和 Node.js 并加入 PATH。"
    }
}
if (-not (Test-Path -LiteralPath $frontend -PathType Container)) {
    throw "前端目录不存在：$frontend。可通过 -FrontendPath 指定。"
}

if (-not $SkipInfra) {
    $mysqlExe = Join-Path $MySqlRoot 'mysql-9.7.1-winx64\bin\mysqld.exe'
    $myIni = Join-Path $MySqlRoot 'my.ini'
    if (-not (Test-Port 3306)) {
        if (-not (Test-Path -LiteralPath $myIni -PathType Leaf)) { throw "MySQL 配置文件不存在：$myIni" }
        Start-Logged 'mysql' $mysqlExe @("--defaults-file=`"$myIni`"", '--console') $MySqlRoot 3306
        if (-not $DryRun) { Wait-Port 'MySQL' 3306 45 }
    } else { Write-Host 'MySQL 已运行，跳过。' }

    $redisExe = Join-Path $RedisRoot 'redis-server.exe'
    $redisConf = Join-Path $RedisRoot 'redis.conf'
    if (-not (Test-Port 6379)) {
        if (-not (Test-Path -LiteralPath $redisConf -PathType Leaf)) { throw "Redis 配置文件不存在：$redisConf" }
        Start-Logged 'redis' $redisExe @('redis.conf') $RedisRoot 6379
        if (-not $DryRun) { Wait-Port 'Redis' 6379 20 }
    } else { Write-Host 'Redis 已运行，跳过。' }

    $rabbitBat = Join-Path $RabbitRoot 'sbin\rabbitmq-server.bat'
    if (-not (Test-Port 5672)) {
        $erlExe = Join-Path $ErlangRoot 'bin\erl.exe'
        if (-not (Test-Path -LiteralPath $erlExe -PathType Leaf)) {
            throw "Erlang 未找到：$ErlangRoot"
        }
        if (-not (Test-Path -LiteralPath (Join-Path $ErlangRoot 'releases\27') -PathType Container)) {
            throw "RabbitMQ 4.3.5 需要 Erlang 27；当前 $ErlangRoot 不是 Erlang 27。请安装 Erlang 27 或用 -ErlangRoot 指定路径。"
        }
        $env:ERLANG_HOME = $ErlangRoot
        Start-Logged 'rabbitmq' (Join-Path $env:SystemRoot 'System32\cmd.exe') `
            @('/d', '/c', "`"$rabbitBat`"") (Join-Path $RabbitRoot 'sbin') 5672
        if (-not $DryRun) { Wait-Port 'RabbitMQ' 5672 90 }
    } else { Write-Host 'RabbitMQ 已运行，跳过。' }

    $nacosBat = Join-Path $NacosRoot 'bin\startup.cmd'
    if (-not (Test-Port 8848)) {
        if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
            throw 'Nacos 需要 JAVA_HOME 指向已安装的 JDK。'
        }
        Start-Logged 'nacos' (Join-Path $env:SystemRoot 'System32\cmd.exe') `
            @('/d', '/c', "`"$nacosBat`" -m standalone") (Join-Path $NacosRoot 'bin') 8848
        if (-not $DryRun) { Wait-Port 'Nacos' 8848 120 }
    } else { Write-Host 'Nacos 已运行，跳过。' }
} else {
    foreach ($dep in @(@{Name='MySQL';Port=3306},@{Name='Redis';Port=6379},
                      @{Name='RabbitMQ';Port=5672},@{Name='Nacos';Port=8848})) {
        if (-not (Test-Port $dep.Port) -and -not $DryRun) {
            throw "$($dep.Name) 未运行（端口 $($dep.Port)），请去掉 -SkipInfra 重试。"
        }
    }
}

if ($DryRun) { Write-Host '预览完成：未启动任何程序。'; exit 0 }

if (-not $SkipBuild) {
    Write-Host '正在构建后端，请稍候…'
    & (Join-Path $projectRoot 'mvnw.cmd') -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "后端构建失败，退出码 $LASTEXITCODE" }
}

$services = @(
    @{ Name='FCM-brand-microservice'; Port=20001 },
    @{ Name='FCM-category-microservice'; Port=20002 },
    @{ Name='FCM-good-microservice'; Port=20003 },
    @{ Name='FCM-member-microservice'; Port=20004 },
    @{ Name='FCM-member-address-microservice'; Port=20005 },
    @{ Name='FCM-region-microservice'; Port=20006 },
    @{ Name='FCM-cart-microservice'; Port=20007 },
    @{ Name='FCM-order-microservice'; Port=20008 },
    @{ Name='FCM-user-microservice'; Port=20009 },
    @{ Name='FCM-rbac-microservice'; Port=20010 },
    @{ Name='FCM-seckill-microservice'; Port=20011 },
    @{ Name='FCM-upload-microservice'; Port=20012 },
    @{ Name='FCM-collect-microservice'; Port=20013 },
    @{ Name='FCM-gateway'; Port=8888 }
)
foreach ($service in $services) {
    if (Test-Port $service.Port) {
        Write-Host "$($service.Name) 端口 $($service.Port) 已监听，跳过。"
        continue
    }
    $module = if ($service.Name -eq 'FCM-gateway') {
        Join-Path $projectRoot 'FCM-gateway'
    } else {
        Join-Path (Join-Path $projectRoot 'FCM-microservice') $service.Name
    }
    $jar = Get-ChildItem -LiteralPath (Join-Path $module 'target') -Filter "$($service.Name)-*.jar" |
        Where-Object { $_.Name -notmatch '\.original$' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { throw "缺少 $($service.Name) 的 jar，请去掉 -SkipBuild 重试。" }
    $process = Start-Process -FilePath 'java' -ArgumentList @('-jar', "`"$($jar.FullName)`"") `
        -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDir "$($service.Name).out.log") `
        -RedirectStandardError (Join-Path $logDir "$($service.Name).err.log")
    $started += [pscustomobject]@{ Name=$service.Name; Pid=$process.Id; Port=$service.Port }
    Write-Host "已启动 $($service.Name) (PID $($process.Id))"
}

if (-not (Test-Path -LiteralPath (Join-Path $frontend 'node_modules'))) {
    Write-Host '正在安装前端依赖…'
    Push-Location $frontend
    try {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw "npm ci 失败，退出码 $LASTEXITCODE" }
    } finally { Pop-Location }
}
if (-not (Test-Port 5173)) {
    $web = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'npm.cmd run dev -- --host 127.0.0.1' `
        -WorkingDirectory $frontend -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $logDir 'frontend.out.log') `
        -RedirectStandardError (Join-Path $logDir 'frontend.err.log')
    $started += [pscustomobject]@{ Name='frontend'; Pid=$web.Id; Port=5173 }
    Write-Host "已启动前端 (PID $($web.Id))"
}

if ($started.Count -gt 0) {
    $started | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $logDir 'started-processes.json') -Encoding UTF8
}
Write-Host '正在等待微服务、网关和前端就绪…'
foreach ($service in $services) { Wait-Port $service.Name $service.Port 90 }
Wait-Port '前端' 5173 30
Write-Host '启动完成：商城 http://localhost:5173 ，网关 http://localhost:8888'
