[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath(
    (Split-Path -Parent $PSScriptRoot)).TrimEnd('\')
$textExtensions = @(
    '.java', '.xml', '.md', '.ps1', '.yml', '.yaml', '.json', '.properties'
)
$textNames = @('.gitignore', '.gitattributes', 'LICENSE')
$generatedDirectories = @('build', '.git', '.keys')
$forbiddenArtifactExtensions = @(
    '.apk', '.aab', '.jks', '.keystore', '.p12', '.pfx', '.pem', '.key',
    '.der', '.idsig', '.log', '.db', '.sqlite', '.env'
)
$forbidden = [ordered]@{
    'local Windows profile path' = '(?i)[A-Z]:[\\/]Users[\\/][^\\/\s"'']+'
    'local Unix profile path' = '(?i)(?<![A-Za-z0-9_])/(?:home|Users)/[^/\s"'']+'
    'hard-coded adb device selector' = '(?i)\badb(?:\.exe)?(?:\s+[^\r\n\s]+)*\s+-s\s+[A-Za-z0-9._:-]{6,}'
    'Android identity property dump' = '(?i)\bro\.product\.(?:model|device)\b'
    'embedded private key' = 'BEGIN (?:RSA |EC |OPENSSH |ENCRYPTED )?PRIVATE KEY'
    'credential-bearing Bark URL' = '(?i)https://api\.day\.app/[A-Za-z0-9_-]{16,}'
    'hard-coded Bark Device Key' = '(?i)(?:bark|device)[_ -]?key\s*[:=]\s*["'']?[A-Za-z0-9_-]{16,}'
    'GitHub access token' = '(?i)\b(?:ghp_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{20,})\b'
    'generic bearer token literal' = '(?i)authorization\s*[:=]\s*["'']?bearer\s+[A-Za-z0-9._-]{20,}'
    'account address literal' = '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b'
    'mainland mobile number' = '(?<!\d)1[3-9]\d{9}(?!\d)'
    'Japan mobile number' = '(?<!\d)(?:070|080|090)-?\d{4}-?\d{4}(?!\d)'
    'IMEI-like identifier' = '(?<!\d)\d{15}(?!\d)'
    'legacy secondary-channel code' = '(?i)\b(?:smtp|mime|mail|email)\b|163\.com|邮箱|邮件|网易'
    'legacy private package marker' = '(?i)cnjp'
}

$violations = New-Object System.Collections.Generic.List[string]

function Get-RelativePath {
    param([Parameter(Mandatory)][string]$FullName)
    return $FullName.Substring($projectRoot.Length).TrimStart('\').Replace('\', '/')
}

$files = Get-ChildItem -LiteralPath $projectRoot -Recurse -File -Force |
    Where-Object {
        $relative = Get-RelativePath -FullName $_.FullName
        $segments = $relative -split '/'
        -not ($segments | Where-Object { $_ -eq '.git' })
    }

foreach ($file in $files) {
    $relative = Get-RelativePath -FullName $file.FullName
    $segments = $relative -split '/'
    $inGeneratedDirectory = $segments | Where-Object {
        $generatedDirectories -contains $_
    }

    if (-not $inGeneratedDirectory -and
            $forbiddenArtifactExtensions -contains $file.Extension.ToLowerInvariant()) {
        $violations.Add("${relative}: forbidden release artifact")
    }
    if (-not $inGeneratedDirectory -and
            $relative -match '(?i)(?:smtp|mime|mail|email|163|cnjp)') {
        $violations.Add("${relative}: legacy channel or private marker in filename")
    }
    if ($inGeneratedDirectory -or $relative -eq 'scripts/check-public-tree.ps1') {
        continue
    }
    if ($textExtensions -notcontains $file.Extension.ToLowerInvariant() -and
            $textNames -notcontains $file.Name) {
        continue
    }

    $content = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($entry in $forbidden.GetEnumerator()) {
        if ($content -match $entry.Value) {
            $violations.Add("${relative}: $($entry.Key)")
        }
    }
}

if (Test-Path -LiteralPath (Join-Path $projectRoot '.git')) {
    $git = (Get-Command git -ErrorAction Stop).Source
    $trackedPaths = @(& $git -C $projectRoot ls-files)
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to enumerate tracked Git files.'
    }
    foreach ($trackedPath in $trackedPaths) {
        $normalized = $trackedPath.Replace('\', '/')
        $extension = [System.IO.Path]::GetExtension($normalized).ToLowerInvariant()
        if ($normalized -match '^(?:build|\.keys)/' -or
                $forbiddenArtifactExtensions -contains $extension) {
            $violations.Add("${normalized}: generated or sensitive artifact is tracked")
        }
    }
}

if ($violations.Count -gt 0) {
    $violations | Sort-Object -Unique | ForEach-Object { Write-Error $_ }
    throw 'Public-tree safety check failed.'
}

Write-Output "Public-tree safety check passed ($($files.Count) files reviewed)."
