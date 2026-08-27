[CmdletBinding()]
param(
    [switch]$SkipTests,
    [string]$SdkRoot,
    [string]$Keystore,
    [string]$StorePassword,
    [string]$KeyAlias,
    [string]$KeyPassword
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Assert-NativeSuccess {
    param(
        [Parameter(Mandatory)][string]$Step,
        [Parameter(Mandatory)][int]$ExitCode
    )
    if ($ExitCode -ne 0) {
        throw "$Step failed with exit code $ExitCode."
    }
}

function Resolve-ProjectPath {
    param([Parameter(Mandatory)][string]$Path)
    $candidate = $Path
    if (-not [System.IO.Path]::IsPathRooted($candidate)) {
        $candidate = Join-Path $script:projectRoot $candidate
    }
    return [System.IO.Path]::GetFullPath($candidate)
}

$projectRoot = [System.IO.Path]::GetFullPath(
    (Split-Path -Parent $MyInvocation.MyCommand.Path)).TrimEnd('\')

$sdkCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($SdkRoot)) {
    $sdkCandidates += $SdkRoot
}
if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
    $sdkCandidates += $env:ANDROID_SDK_ROOT
}
if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
    $sdkCandidates += $env:ANDROID_HOME
}
if (-not [string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    $sdkCandidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
}

$resolvedSdk = $null
foreach ($candidate in $sdkCandidates) {
    $candidatePath = if ([System.IO.Path]::IsPathRooted($candidate)) {
        [System.IO.Path]::GetFullPath($candidate)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $projectRoot $candidate))
    }
    if (Test-Path -LiteralPath (Join-Path $candidatePath 'platforms\android-35\android.jar')) {
        $resolvedSdk = $candidatePath
        break
    }
}
if ($null -eq $resolvedSdk) {
    throw 'Android SDK platform 35 was not found. Set ANDROID_SDK_ROOT or use -SdkRoot.'
}

$androidJar = Join-Path $resolvedSdk 'platforms\android-35\android.jar'
$buildToolsRoot = Join-Path $resolvedSdk 'build-tools'
$toolsets = @()
if (Test-Path -LiteralPath $buildToolsRoot) {
    foreach ($versionDirectory in Get-ChildItem -LiteralPath $buildToolsRoot -Directory) {
        foreach ($candidate in @(
                $versionDirectory.FullName,
                (Join-Path $versionDirectory.FullName 'android-16'))) {
            if ((Test-Path -LiteralPath (Join-Path $candidate 'aapt.exe')) -and
                    (Test-Path -LiteralPath (Join-Path $candidate 'd8.bat')) -and
                    (Test-Path -LiteralPath (Join-Path $candidate 'zipalign.exe')) -and
                    (Test-Path -LiteralPath (Join-Path $candidate 'apksigner.bat'))) {
                $toolsets += [pscustomobject]@{
                    Version = $versionDirectory.Name
                    Path = $candidate
                }
            }
        }
    }
}
$selectedTools = $toolsets | Sort-Object Version -Descending | Select-Object -First 1
if ($null -eq $selectedTools) {
    throw 'No usable Windows Android build-tools installation was found.'
}

$aapt = Join-Path $selectedTools.Path 'aapt.exe'
$d8 = Join-Path $selectedTools.Path 'd8.bat'
$zipalign = Join-Path $selectedTools.Path 'zipalign.exe'
$apksigner = Join-Path $selectedTools.Path 'apksigner.bat'
$javac = (Get-Command javac -ErrorAction Stop).Source
$java = (Get-Command java -ErrorAction Stop).Source
$keytool = (Get-Command keytool -ErrorAction Stop).Source

$manifestPath = Join-Path $projectRoot 'AndroidManifest.xml'
$manifest = Get-Content -LiteralPath $manifestPath -Raw
$versionMatch = [regex]::Match($manifest, 'android:versionName="([^"]+)"')
if (-not $versionMatch.Success) {
    throw 'AndroidManifest.xml does not contain android:versionName.'
}
$versionName = $versionMatch.Groups[1].Value

$buildDir = Join-Path $projectRoot 'build'
$resolvedBuild = [System.IO.Path]::GetFullPath($buildDir).TrimEnd('\')
if (-not $resolvedBuild.StartsWith(
        $projectRoot + '\', [System.StringComparison]::OrdinalIgnoreCase) -or
        (Split-Path -Leaf $resolvedBuild) -ne 'build') {
    throw "Refusing to clean unexpected build path: $resolvedBuild"
}
if (Test-Path -LiteralPath $resolvedBuild) {
    Remove-Item -LiteralPath $resolvedBuild -Recurse -Force
}

$tempRoot = [System.IO.Path]::GetFullPath(
    [System.IO.Path]::GetTempPath()).TrimEnd('\')
$stageDir = Join-Path $tempRoot ('sms-relay-' + [guid]::NewGuid().ToString('N'))
$genDir = Join-Path $stageDir 'gen'
$classesDir = Join-Path $stageDir 'classes'
$dexDir = Join-Path $stageDir 'dex'
$hostClasses = Join-Path $stageDir 'host-classes'
$stageProject = Join-Path $stageDir 'project'
$stageAndroidJar = Join-Path $stageDir 'android.jar'
$outputDir = Join-Path $resolvedBuild 'outputs'

try {
    foreach ($directory in @(
            $genDir, $classesDir, $dexDir, $hostClasses, $stageProject, $outputDir)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    Copy-Item -LiteralPath $manifestPath -Destination $stageProject
    Copy-Item -LiteralPath (Join-Path $projectRoot 'res') `
        -Destination $stageProject -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot 'src') `
        -Destination $stageProject -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot 'host-test') `
        -Destination $stageProject -Recurse
    $rawResourceDirectory = Join-Path $stageProject 'res\raw'
    New-Item -ItemType Directory -Path $rawResourceDirectory -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'THIRD_PARTY_NOTICES.md') `
        -Destination (Join-Path $rawResourceDirectory 'third_party_notices.txt')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSE') `
        -Destination (Join-Path $rawResourceDirectory 'project_mit.txt')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSES\Apache-2.0.txt') `
        -Destination (Join-Path $rawResourceDirectory 'apache_2_0.txt')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSES\Bark-MIT.txt') `
        -Destination (Join-Path $rawResourceDirectory 'bark_mit.txt')
    Copy-Item -LiteralPath $androidJar -Destination $stageAndroidJar

    if (-not $SkipTests) {
        $hostSources = @(
            (Join-Path $stageProject 'src\com\local\smsrelay\CryptoUtils.java'),
            (Join-Path $stageProject 'src\com\local\smsrelay\RetryPolicy.java'),
            (Join-Path $stageProject 'src\com\local\smsrelay\MessageFingerprint.java'),
            (Join-Path $stageProject 'src\com\local\smsrelay\ChannelConfigValidator.java'),
            (Join-Path $stageProject 'src\com\local\smsrelay\SchedulePolicy.java'),
            (Join-Path $stageProject 'host-test\com\local\smsrelay\HostTests.java')
        )
        & $javac -encoding UTF-8 -source 8 -target 8 -d $hostClasses $hostSources
        Assert-NativeSuccess -Step 'Host test compilation' -ExitCode $LASTEXITCODE
        & $java -cp $hostClasses 'com.local.smsrelay.HostTests'
        Assert-NativeSuccess -Step 'Host tests' -ExitCode $LASTEXITCODE
    }

    & $aapt package -f -m -J $genDir `
        -M (Join-Path $stageProject 'AndroidManifest.xml') `
        -S (Join-Path $stageProject 'res') -I $stageAndroidJar
    Assert-NativeSuccess -Step 'aapt resource generation' -ExitCode $LASTEXITCODE

    $androidSources = @(
        Get-ChildItem -LiteralPath (Join-Path $stageProject 'src') `
            -Recurse -Filter '*.java' | ForEach-Object FullName
        Get-ChildItem -LiteralPath $genDir `
            -Recurse -Filter '*.java' | ForEach-Object FullName
    )
    & $javac -encoding UTF-8 -source 8 -target 8 `
        -classpath $stageAndroidJar -d $classesDir $androidSources
    Assert-NativeSuccess -Step 'Android Java compilation' -ExitCode $LASTEXITCODE

    $classFiles = @(
        Get-ChildItem -LiteralPath $classesDir -Recurse -Filter '*.class' |
            ForEach-Object FullName
    )
    & $d8 --lib $stageAndroidJar --min-api 26 --output $dexDir $classFiles
    Assert-NativeSuccess -Step 'D8' -ExitCode $LASTEXITCODE

    $unsignedApk = Join-Path $stageDir 'sms-relay-unsigned.apk'
    $alignedApk = Join-Path $stageDir 'sms-relay-aligned.apk'
    $stageSignedApk = Join-Path $stageDir "sms-relay-$versionName.apk"
    $signedApk = Join-Path $outputDir "sms-relay-$versionName.apk"
    & $aapt package -f -M (Join-Path $stageProject 'AndroidManifest.xml') `
        -S (Join-Path $stageProject 'res') -I $stageAndroidJar -F $unsignedApk
    Assert-NativeSuccess -Step 'aapt APK packaging' -ExitCode $LASTEXITCODE

    Push-Location $dexDir
    try {
        & $aapt add $unsignedApk 'classes.dex'
        Assert-NativeSuccess -Step 'Adding classes.dex' -ExitCode $LASTEXITCODE
    } finally {
        Pop-Location
    }
    & $zipalign -f 4 $unsignedApk $alignedApk
    Assert-NativeSuccess -Step 'zipalign' -ExitCode $LASTEXITCODE

    $keystoreWasSpecified = -not [string]::IsNullOrWhiteSpace($Keystore)
    if (-not $keystoreWasSpecified -and
            -not [string]::IsNullOrWhiteSpace($env:SMS_RELAY_KEYSTORE)) {
        $Keystore = $env:SMS_RELAY_KEYSTORE
        $keystoreWasSpecified = $true
    }
    if (-not $keystoreWasSpecified) {
        $Keystore = Join-Path $projectRoot '.keys\sms-relay-debug.jks'
    }
    $Keystore = Resolve-ProjectPath $Keystore

    if ([string]::IsNullOrWhiteSpace($StorePassword)) {
        $StorePassword = if (-not [string]::IsNullOrWhiteSpace(
                $env:SMS_RELAY_STORE_PASSWORD)) {
            $env:SMS_RELAY_STORE_PASSWORD
        } else {
            'android'
        }
    }
    if ([string]::IsNullOrWhiteSpace($KeyAlias)) {
        $KeyAlias = if (-not [string]::IsNullOrWhiteSpace($env:SMS_RELAY_KEY_ALIAS)) {
            $env:SMS_RELAY_KEY_ALIAS
        } else {
            'androiddebugkey'
        }
    }
    if ([string]::IsNullOrWhiteSpace($KeyPassword)) {
        $KeyPassword = if (-not [string]::IsNullOrWhiteSpace(
                $env:SMS_RELAY_KEY_PASSWORD)) {
            $env:SMS_RELAY_KEY_PASSWORD
        } else {
            $StorePassword
        }
    }

    if (-not (Test-Path -LiteralPath $Keystore)) {
        if ($keystoreWasSpecified) {
            throw "The specified keystore does not exist: $Keystore"
        }
        New-Item -ItemType Directory -Path (Split-Path -Parent $Keystore) `
            -Force | Out-Null
        & $keytool -genkeypair -noprompt -keystore $Keystore `
            -storepass $StorePassword -alias $KeyAlias -keypass $KeyPassword `
            -keyalg RSA -keysize 2048 -validity 10000 `
            -dname 'CN=SMS Relay Debug,OU=Local Development'
        Assert-NativeSuccess -Step 'Debug signing key generation' -ExitCode $LASTEXITCODE
    }

    & $apksigner sign --ks $Keystore --ks-pass "pass:$StorePassword" `
        --key-pass "pass:$KeyPassword" --ks-key-alias $KeyAlias `
        --out $stageSignedApk $alignedApk
    Assert-NativeSuccess -Step 'APK signing' -ExitCode $LASTEXITCODE

    & $apksigner verify --verbose --print-certs $stageSignedApk
    Assert-NativeSuccess -Step 'APK signature verification' -ExitCode $LASTEXITCODE
    & $aapt dump badging $stageSignedApk | Select-Object -First 8
    Assert-NativeSuccess -Step 'APK manifest verification' -ExitCode $LASTEXITCODE
    $apkEntries = @(& $aapt list $stageSignedApk)
    Assert-NativeSuccess -Step 'APK resource listing' -ExitCode $LASTEXITCODE
    foreach ($requiredEntry in @(
            'res/raw/project_mit.txt',
            'res/raw/apache_2_0.txt',
            'res/raw/bark_mit.txt',
            'res/raw/third_party_notices.txt')) {
        if ($apkEntries -notcontains $requiredEntry) {
            throw "APK is missing required license resource: $requiredEntry"
        }
    }
    Copy-Item -LiteralPath $stageSignedApk -Destination $signedApk
    Get-FileHash -LiteralPath $signedApk -Algorithm SHA256 |
        Select-Object Path, Algorithm, Hash
} finally {
    if (Test-Path -LiteralPath $stageDir) {
        $resolvedStage = [System.IO.Path]::GetFullPath($stageDir).TrimEnd('\')
        if ($resolvedStage.StartsWith(
                $tempRoot + '\', [System.StringComparison]::OrdinalIgnoreCase) -and
                (Split-Path -Leaf $resolvedStage).StartsWith('sms-relay-')) {
            try {
                Remove-Item -LiteralPath $resolvedStage -Recurse -Force
            } catch {
                Write-Warning "Unable to remove temporary build directory: $resolvedStage"
            }
        }
    }
}
