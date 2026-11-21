param(
    [string]$CredentialPath = (Join-Path $env:LOCALAPPDATA 'TourDiscoveryDemo\credential.clixml'),
    [Security.SecureString]$Password
)
$ErrorActionPreference = 'Stop'
$qaRepo = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$qaCredential = [IO.Path]::GetFullPath($CredentialPath)
if ($qaCredential.StartsWith($qaRepo + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Store the demo credential outside the repository.'
}
if ($null -eq $Password) { $Password = Read-Host 'Dat mat khau demo cuc bo (toi thieu 12 ky tu)' -AsSecureString }
if ($Password.Length -lt 12) { throw 'Use at least 12 characters for the local demo password.' }
New-Item -ItemType Directory -Path (Split-Path -Parent $qaCredential) -Force | Out-Null
# On Windows, Export-Clixml encrypts SecureString with DPAPI for the current user.
[PSCredential]::new('local-demo', $Password) | Export-Clixml -LiteralPath $qaCredential
Write-Output 'Da luu mat khau demo ma hoa cho tai khoan Windows hien tai. Khong dua tep nay vao Git.'
