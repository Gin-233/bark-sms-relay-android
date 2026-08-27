[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath(
    (Split-Path -Parent $PSScriptRoot)).TrimEnd('\')
$textExtensions = @(
    '.java', '.xml', '.md', '.txt', '.ps1', '.yml', '.yaml', '.json', '.properties'
)
$textNames = @('.gitignore', '.gitattributes', 'LICENSE')
$generatedDirectories = @('build', '.git', '.keys')
$forbiddenArtifactExtensions = @(
    '.apk', '.aab', '.jks', '.keystore', '.p12', '.pfx', '.pem', '.key',
    '.der', '.cer', '.crt', '.p8', '.idsig', '.log', '.db', '.sqlite', '.env'
)
$sensitiveFileNamePattern = '(?i)(?:^|/)(?:\.env(?!\.(?:example|sample|template)$)(?:\.[^/]+)?|local\.properties|keystore\.properties|google-services\.json|id_(?:rsa|dsa|ecdsa|ed25519)|(?:credentials?|secrets?)\.(?:json|ya?ml|properties|txt))$'
$requiredReleaseFiles = @(
    'LICENSE',
    'LICENSES/Apache-2.0.txt',
    'LICENSES/Bark-MIT.txt',
    'THIRD_PARTY_NOTICES.md'
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
    'credential-bearing URL' = '(?i)\bhttps?://[^/\s:@]+:[^/\s@]+@'
    'generic secret assignment' = '(?i)\b(?:api[_ -]?key|client[_ -]?secret|password|passphrase|access[_ -]?token|refresh[_ -]?token)\b\s*[:=]\s*["''][^"'']{8,}["'']'
    'account address literal' = '(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b'
    'compact phone-number-like literal' = '(?<![#A-Za-z0-9])(?:\+\d{1,3})?\d{7,15}(?![A-Za-z0-9])'
    'formatted phone-number-like literal' = '(?<![#A-Za-z0-9])(?:\+\d{1,3}[ \t.-]?)?(?:\(?\d{2,4}\)?[ \t.-]){1,4}\d{3,4}(?![A-Za-z0-9])'
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
    if (-not $inGeneratedDirectory -and $relative -match $sensitiveFileNamePattern) {
        $violations.Add("${relative}: forbidden sensitive filename")
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

foreach ($requiredFile in $requiredReleaseFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $projectRoot $requiredFile) -PathType Leaf)) {
        $violations.Add("${requiredFile}: required release notice is missing")
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
