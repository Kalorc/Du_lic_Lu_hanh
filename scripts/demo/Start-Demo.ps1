param(
    [string]$RuntimeRoot = (Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'),
    [string]$CredentialPath = (Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo\credential.clixml'),
    [ValidateRange(1024,65535)][int]$BackendPort = 18080,
    [ValidateRange(1024,65535)][int]$FrontendPort = 15173,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$qaRepo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$qaRuntime = [IO.Path]::GetFullPath($RuntimeRoot)
if ($qaRuntime.StartsWith($qaRepo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'RuntimeRoot must be outside the repository.'
}
if (-not (Test-Path -LiteralPath $CredentialPath)) {
    throw 'Run scripts/demo/Set-DemoPassword.ps1 first. No default login password is provided.'
}
if ($BackendPort -eq $FrontendPort) { throw 'Backend and frontend need different ports.' }
New-Item -ItemType Directory -Path $qaRuntime -Force | Out-Null
$qaStatePath = Join-Path $qaRuntime 'state.json'
if (Test-Path -LiteralPath $qaStatePath) {
    $qaOldState = Get-Content -LiteralPath $qaStatePath -Raw | ConvertFrom-Json
    foreach ($qaName in @('backend','frontend')) {
        $qaOldRecord = $qaOldState.$qaName
        if ($null -ne $qaOldRecord) {
            $qaOldProcess = Get-Process -Id $qaOldRecord.pid -ErrorAction SilentlyContinue
            if ($qaOldProcess -and $qaOldProcess.StartTime.ToUniversalTime().Ticks.ToString() -eq $qaOldRecord.startTicks) {
                throw 'This demo is still active. Run Stop-Demo.ps1 with this RuntimeRoot first.'
            }
        }
    }
}
foreach ($qaPort in @($BackendPort,$FrontendPort)) {
    $qaListener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,$qaPort)
    try { $qaListener.Start() } catch { throw "Port $qaPort is in use; no existing process was stopped." }
    finally { $qaListener.Stop() }
}
if (-not $SkipBuild) {
    # Build in a fresh copy so npm ci never removes dependencies used by an
    # already-running development server in the user's working checkout.
    $qaBuildRoot = Join-Path $qaRuntime ('build-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + $PID)
    $qaBackend = Join-Path $qaBuildRoot 'backend'
    $qaFrontend = Join-Path $qaBuildRoot 'frontend'
    New-Item -ItemType Directory -Path $qaBackend,$qaFrontend -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $qaRepo 'backend\pom.xml') -Destination $qaBackend
    Copy-Item -LiteralPath (Join-Path $qaRepo 'backend\src') -Destination $qaBackend -Recurse
    Copy-Item -LiteralPath (Join-Path $qaRepo 'demo-assets') -Destination $qaBuildRoot -Recurse
    foreach ($qaName in @('src','public','tests','package.json','package-lock.json','index.html','vite.config.js','playwright.config.js')) {
        Copy-Item -LiteralPath (Join-Path $qaRepo ('frontend\'+$qaName)) -Destination $qaFrontend -Recurse
    }
    Push-Location -LiteralPath $qaBackend
    try {
        & mvn clean verify *> (Join-Path $qaRuntime 'backend-build.log')
        if ($LASTEXITCODE -ne 0) { throw 'Backend verify failed; inspect backend-build.log.' }
    } finally { Pop-Location }
    Push-Location -LiteralPath $qaFrontend
    try {
        & npm ci *> (Join-Path $qaRuntime 'npm-ci.log')
        if ($LASTEXITCODE -ne 0) { throw 'Dependency install failed; inspect npm-ci.log.' }
        & npm run build *> (Join-Path $qaRuntime 'frontend-build.log')
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed; inspect frontend-build.log.' }
    } finally { Pop-Location }
} else {
    if ($null -eq $qaOldState -or -not $qaOldState.buildRoot) {
        throw 'No previously built demo copy exists; first run without -SkipBuild.'
    }
    $qaBuildRoot = [IO.Path]::GetFullPath($qaOldState.buildRoot)
    if (-not $qaBuildRoot.StartsWith($qaRuntime + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Recorded buildRoot must remain inside this RuntimeRoot.'
    }
    $qaBackend = Join-Path $qaBuildRoot 'backend'
    $qaFrontend = Join-Path $qaBuildRoot 'frontend'
}
$qaJarSource = Join-Path $qaBackend 'target\tour-discovery-1.0.0.jar'
$qaVite = Join-Path $qaFrontend 'node_modules\vite\bin\vite.js'
if (-not (Test-Path -LiteralPath $qaJarSource) -or -not (Test-Path -LiteralPath $qaVite)) {
    throw 'Build artifacts/dependencies are missing; rerun without -SkipBuild.'
}
$qaJar = Join-Path $qaRuntime 'demo-backend.jar'
Copy-Item -LiteralPath $qaJarSource -Destination $qaJar -Force
$qaState = [ordered]@{ repository=$qaRepo; buildRoot=$qaBuildRoot; startedAt=(Get-Date).ToUniversalTime().ToString('o');
    backendUrl="http://127.0.0.1:$BackendPort"; frontendUrl="http://127.0.0.1:$FrontendPort";
    database='Isolated H2 memory; recreated from schema/data.sql on every start'; backend=$null; frontend=$null }
function Save-DemoState { $qaState | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $qaStatePath -Encoding UTF8 }
function Process-Record($qaProcess, [string]$qaExpectedExecutable) {
    $qaProcess.Refresh()
    return @{pid=$qaProcess.Id;startTicks=$qaProcess.StartTime.ToUniversalTime().Ticks.ToString();executable=$qaExpectedExecutable}
}
function Wait-DemoUrl([string]$qaUrl) {
    $qaDeadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $qaDeadline) {
        try { if ((Invoke-WebRequest -Uri $qaUrl -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200) { return } }
        catch { Start-Sleep -Milliseconds 350 }
    }
    throw "Readiness timed out for $qaUrl. Inspect the runtime logs."
}
$qaPreviousPassword = $env:APP_DEMO_PASSWORD
$qaPreviousSecret = $env:APP_JWT_SECRET
$qaPreviousProxy = $env:VITE_PROXY_TARGET
try {
    $qaCredential = Import-Clixml -LiteralPath $CredentialPath
    if ($qaCredential -isnot [PSCredential]) { throw 'Credential file is not a protected PSCredential.' }
    $env:APP_DEMO_PASSWORD = $qaCredential.GetNetworkCredential().Password
    $env:APP_JWT_SECRET = $null
    $qaJavaArgs = @('-jar',('"'+$qaJar+'"'),'--spring.profiles.active=demo',"--server.port=$BackendPort",
        "--app.cors.allowed-origins=http://127.0.0.1:$FrontendPort,http://localhost:$FrontendPort",
        '--server.address=127.0.0.1','--spring.datasource.driver-class-name=org.h2.Driver',
        '"--spring.datasource.url=jdbc:h2:mem:tour_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"',
        '--spring.datasource.username=sa','--spring.datasource.password=','--spring.sql.init.mode=always')
    $qaJavaPath = (Get-Command java).Source
    $qaJava = Start-Process -FilePath $qaJavaPath -ArgumentList $qaJavaArgs -WorkingDirectory $qaRuntime -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $qaRuntime 'backend.stdout.log') -RedirectStandardError (Join-Path $qaRuntime 'backend.stderr.log')
    $qaState.backend = Process-Record $qaJava $qaJavaPath
    Save-DemoState
    Wait-DemoUrl ($qaState.backendUrl+'/api/v1/tours?page=1&size=10')
    $env:APP_DEMO_PASSWORD = $null
    $env:VITE_PROXY_TARGET = $qaState.backendUrl
    $qaNodeArgs = @(('"'+$qaVite+'"'),'--host','127.0.0.1','--port',"$FrontendPort",'--strictPort')
    $qaNodePath = (Get-Command node).Source
    $qaNode = Start-Process -FilePath $qaNodePath -ArgumentList $qaNodeArgs -WorkingDirectory $qaFrontend -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $qaRuntime 'frontend.stdout.log') -RedirectStandardError (Join-Path $qaRuntime 'frontend.stderr.log')
    $qaState.frontend = Process-Record $qaNode $qaNodePath
    Save-DemoState
    Wait-DemoUrl ($qaState.frontendUrl+'/tours')
    $qaState.readyAt = (Get-Date).ToUniversalTime().ToString('o')
    Save-DemoState
    Write-Output ('Website: '+$qaState.frontendUrl+'/tours')
    Write-Output ('Dang nhap: '+$qaState.frontendUrl+'/admin (product01 / manager01; mat khau do ban da dat)')
    Write-Output ('Logs/state: '+$qaRuntime)
} catch {
    if (Test-Path -LiteralPath $qaStatePath) { & (Join-Path $PSScriptRoot 'Stop-Demo.ps1') -RuntimeRoot $qaRuntime }
    throw
} finally {
    $env:APP_DEMO_PASSWORD = $qaPreviousPassword
    $env:APP_JWT_SECRET = $qaPreviousSecret
    $env:VITE_PROXY_TARGET = $qaPreviousProxy
}
