# Regression for recycled PIDs: an unrelated process must survive, while an
# independently recorded helper can still be stopped in the same operation.
param([string]$TestRoot = (Join-Path $env:TEMP ('tour-demo-lifecycle-'+[Guid]::NewGuid().ToString('N'))))
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Path $TestRoot -Force | Out-Null
$qaShell=(Get-Process -Id $PID).Path
$qaHelper=Start-Process -FilePath $qaShell -ArgumentList @('-NoProfile','-Command','"Start-Sleep -Seconds 60"') -WindowStyle Hidden -PassThru
try {
    $qaHelper.Refresh()
    $qaState=@{ buildRoot=(Join-Path $TestRoot 'saved-build');
        frontend=@{pid=$PID;startTicks='0';executable=$qaShell};
        backend=@{pid=$qaHelper.Id;startTicks=$qaHelper.StartTime.ToUniversalTime().Ticks.ToString();executable=$qaShell} }
    $qaState | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $TestRoot 'state.json') -Encoding UTF8
    & (Join-Path $PSScriptRoot 'Stop-Demo.ps1') -RuntimeRoot $TestRoot
    if (-not (Get-Process -Id $PID -ErrorAction SilentlyContinue)) { throw 'The unrelated PID was stopped.' }
    if (Get-Process -Id $qaHelper.Id -ErrorAction SilentlyContinue) { throw 'The recorded helper was not stopped.' }
    $qaAfter=Get-Content -LiteralPath (Join-Path $TestRoot 'state.json') -Raw | ConvertFrom-Json
    if ($null -ne $qaAfter.frontend -or $null -ne $qaAfter.backend) { throw 'Stopped/stale records were not cleared.' }
    if ($qaAfter.buildRoot -ne $qaState.buildRoot) { throw 'Build identity must survive stopping.' }
    Write-Output 'PASS: recycled PID preserved, owned helper stopped, stale records cleared, buildRoot retained.'
} finally {
    $qaRemaining=Get-Process -Id $qaHelper.Id -ErrorAction SilentlyContinue
    if ($qaRemaining -and $qaRemaining.StartTime -eq $qaHelper.StartTime) { Stop-Process -Id $qaHelper.Id }
}
