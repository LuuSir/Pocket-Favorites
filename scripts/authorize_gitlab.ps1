<#
.SYNOPSIS
Authorizes the F-Droid submission using a GitLab token entered at a masked prompt.
.DESCRIPTION
Run in an interactive Windows PowerShell terminal, from the repository root:
  & .\scripts\authorize_gitlab.ps1 -Username LuuSir
Requires Git Credential Manager and an existing GitLab personal access token with
the api scope. Create a short-lived token at:
https://gitlab.com/-/user_settings/personal_access_tokens?name=F-Droid+submission&scopes=api
The token is checked against gitlab.com, then stored only by Git Credential
Manager in the operating system credential store. It is never written to project
files, printed, passed as a command-line argument, or saved in shell history.
Expected output: GitLab authorization ready for <username>.
#>
param(
    [ValidatePattern('^[A-Za-z0-9_.-]+$')]
    [string]$Username = 'LuuSir'
)

$ErrorActionPreference = 'Stop'
$gitExecutable = (Get-Command git -ErrorAction Stop).Source
$gitlabSecret = Read-Host 'GitLab token (masked input; not saved to project)' -AsSecureString
$secretPointer = [IntPtr]::Zero
$plainToken = $null
try {
    $secretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($gitlabSecret)
    $plainToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer)
    if ([string]::IsNullOrWhiteSpace($plainToken)) { throw 'No token entered.' }
    try {
        $profile = Invoke-RestMethod -Uri 'https://gitlab.com/api/v4/user' -Headers @{ 'PRIVATE-TOKEN' = $plainToken }
    } catch {
        throw 'GitLab authorization check failed. Verify the token, api scope, expiry, and network connection.'
    }
    if ($profile.username -ine $Username) { throw 'The token belongs to a different GitLab account.' }

    $processInfo = [Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = $gitExecutable
    $processInfo.Arguments = 'credential-manager store'
    $processInfo.UseShellExecute = $false
    $processInfo.CreateNoWindow = $true
    $processInfo.RedirectStandardInput = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $process = [Diagnostics.Process]::Start($processInfo)
    $process.StandardInput.Write("protocol=https`nhost=gitlab.com`nusername=$Username`npassword=$plainToken`n`n")
    $process.StandardInput.Close()
    $discardedOutput = $process.StandardOutput.ReadToEnd()
    $discardedError = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    $storeExit = $process.ExitCode
    $process.Dispose()
    if ($storeExit -ne 0) { throw "Git Credential Manager could not store authorization (exit $storeExit)." }
    Write-Host "GitLab authorization ready for $Username."
} finally {
    if ($secretPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer) }
    $plainToken = $null
    $gitlabSecret.Dispose()
}
