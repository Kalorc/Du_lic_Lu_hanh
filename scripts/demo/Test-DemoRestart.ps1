param(
    [string]$RuntimeRoot = (Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo'),
    [string]$CredentialPath = (Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo\credential.clixml')
)
$ErrorActionPreference='Stop'
$qaState=Get-Content -LiteralPath (Join-Path $RuntimeRoot 'state.json') -Raw | ConvertFrom-Json
$qaApi=$qaState.backendUrl+'/api/v1'
$qaPassword=(Import-Clixml -LiteralPath $CredentialPath).GetNetworkCredential().Password
function Login-Demo([string]$qaUsername) {
    return (Invoke-RestMethod -Uri ($qaApi+'/auth/login') -Method Post -ContentType 'application/json' -Body (@{username=$qaUsername;password=$qaPassword}|ConvertTo-Json)).data
}
$qaProduct=Login-Demo 'product01'
$qaOldToken=$qaProduct.accessToken
$qaHeaders=@{Authorization='Bearer '+$qaOldToken}
$qaBefore=Invoke-RestMethod -Uri ($qaApi+'/tours?page=1&size=10')
$qaChanged=Invoke-RestMethod -Uri ($qaApi+'/internal/tour-versions/215/interests') -Method Put -Headers $qaHeaders -ContentType 'application/json' -Body '{"interestCodes":["BEACH","CULTURE"]}'
if (($qaChanged.data.interests.code | Sort-Object) -join ',' -ne 'BEACH,CULTURE') { throw 'Could not establish the pre-restart draft fixture.' }
& (Join-Path $PSScriptRoot 'Stop-Demo.ps1') -RuntimeRoot $RuntimeRoot
& (Join-Path $PSScriptRoot 'Start-Demo.ps1') -RuntimeRoot $RuntimeRoot -CredentialPath $CredentialPath -BackendPort ([Uri]$qaState.backendUrl).Port -FrontendPort ([Uri]$qaState.frontendUrl).Port -SkipBuild
$qaOldTokenRejected=$false
try { $null=Invoke-RestMethod -Uri ($qaApi+'/auth/me') -Headers $qaHeaders }
catch { $qaOldTokenRejected=([int]$_.Exception.Response.StatusCode -eq 401) }
if (-not $qaOldTokenRejected) { throw 'Pre-restart token remained usable.' }
$qaRoleChecks=@()
foreach ($qaEntry in @(@('product01','PRODUCT'),@('manager01','MANAGER'),@('khach01','CUSTOMER'),@('admin01','ADMIN'))) {
    $qaUser=Login-Demo $qaEntry[0]
    if ($qaUser.role -ne $qaEntry[1]) { throw 'Role mismatch after restart.' }
    $qaRoleChecks+=@{username=$qaEntry[0];role=$qaUser.role;loginStatus=200}
}
$qaNewProduct=Login-Demo 'product01'
$qaDraft=Invoke-RestMethod -Uri ($qaApi+'/internal/tour-versions/215/interests') -Headers @{Authorization='Bearer '+$qaNewProduct.accessToken}
if (($qaDraft.data.interests.code | Sort-Object) -join ',' -ne 'BEACH,FOOD') { throw 'H2 did not restore the seed draft tags.' }
$qaAfter=Invoke-RestMethod -Uri ($qaApi+'/tours?page=1&size=10')
if ($qaAfter.data.pagination.totalItems -ne 12) { throw 'Public Tour count changed after restart.' }
$qaBeforeItems=$qaBefore.data.items | ForEach-Object { $_.tourId+':'+$_.priceFrom }
$qaAfterItems=$qaAfter.data.items | ForEach-Object { $_.tourId+':'+$_.priceFrom }
if (($qaBeforeItems -join ',') -ne ($qaAfterItems -join ',')) { throw 'Public price/order baseline changed after restart.' }
$qaResult=@{status='PASS';verifiedAt=(Get-Date).ToUniversalTime().ToString('o');roleChecks=$qaRoleChecks;
    oldTokenStatus=401;publicTours=12;publicPricesAndOrderUnchanged=$true;draftBefore='BEACH,CULTURE';draftAfter='BEACH,FOOD';
    credential='DPAPI only; no password/token/body recorded';database='Fresh H2 memory from schema/data.sql'}
$qaResult | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $RuntimeRoot 'restart-verification.json') -Encoding UTF8
Write-Output 'PASS: restart, four role logins, JWT rotation, seed reset, public prices/order.'
