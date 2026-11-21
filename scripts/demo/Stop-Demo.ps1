param([string]$RuntimeRoot = (Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'))
$ErrorActionPreference = 'Stop'
$qaStatePath = Join-Path $RuntimeRoot 'state.json'
if (-not (Test-Path -LiteralPath $qaStatePath)) { Write-Output 'Khong co tien trinh demo duoc script ghi nhan.'; return }
$qaState = Get-Content -LiteralPath $qaStatePath -Raw | ConvertFrom-Json
foreach ($qaName in @('frontend', 'backend')) {
    $qaRecord = $qaState.$qaName
    if ($null -eq $qaRecord) { continue }
    $qaProcess = Get-Process -Id $qaRecord.pid -ErrorAction SilentlyContinue
    if ($null -eq $qaProcess) { $qaState.$qaName = $null; continue }
    if ($qaProcess.StartTime.ToUniversalTime().Ticks.ToString() -ne $qaRecord.startTicks) {
        Write-Output "Bo qua PID cu da duoc tai su dung: $qaName."
        $qaState.$qaName = $null
        continue
    }
    $qaLivePath = $qaProcess.Path
    $qaExpectedPath = $qaRecord.executable
    if ([string]::IsNullOrWhiteSpace($qaExpectedPath)) {
        # Earlier state files could capture Path before Windows populated it.
        # Recover only after matching creation time, executable and owned script.
        $qaNative = Get-CimInstance Win32_Process -Filter ("ProcessId="+$qaProcess.Id)
        $qaLivePath = $qaNative.ExecutablePath
        if ($qaName -eq 'frontend') {
            $qaExpectedPath = (Get-Command node).Source
            $qaBuild = if ($qaState.buildRoot) { $qaState.buildRoot } else { $qaState.repository }
            $qaOwnedFile = Join-Path $qaBuild 'frontend\node_modules\vite\bin\vite.js'
        } else {
            $qaExpectedPath = (Get-Command java).Source
            $qaOwnedFile = Join-Path ([IO.Path]::GetFullPath($RuntimeRoot)) 'demo-backend.jar'
        }
        if (-not $qaNative.CommandLine -or $qaNative.CommandLine.IndexOf($qaOwnedFile,[StringComparison]::OrdinalIgnoreCase) -lt 0) {
            throw "Cannot verify legacy $qaName ownership; no process was stopped for this record."
        }
    }
    if ($qaLivePath -ne $qaExpectedPath) {
        Write-Output "Bo qua executable khong khop ban ghi: $qaName."
        $qaState.$qaName = $null
        continue
    }
    Stop-Process -Id $qaProcess.Id -ErrorAction Stop
    $qaProcess.WaitForExit(10000) | Out-Null
    $qaState.$qaName = $null
    Write-Output "Da dung $qaName cua ban demo nay."
}
$qaState | Add-Member -NotePropertyName stoppedAt -NotePropertyValue (Get-Date).ToUniversalTime().ToString('o') -Force
$qaState | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $qaStatePath -Encoding UTF8
