[CmdletBinding()]
param(
    [string]$DeviceSerial,
    [uri]$ProxyUrl,
    [switch]$Performance
)

$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot

try {
    $sdkDirectory = $env:ANDROID_HOME
    if (-not $sdkDirectory) { $sdkDirectory = $env:ANDROID_SDK_ROOT }
    if (Test-Path -LiteralPath 'local.properties') {
        foreach ($propertyLine in Get-Content -LiteralPath 'local.properties') {
            if ($propertyLine -match '^\s*sdk\.dir\s*=\s*(.+)$') {
                $sdkDirectory = $Matches[1].Trim().Replace('\\', '\').Replace('\:', ':')
                break
            }
        }
    }
    if (-not $sdkDirectory) {
        throw 'Set sdk.dir in local.properties or set ANDROID_HOME to your Android SDK.'
    }

    $adbPath = Join-Path $sdkDirectory 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adbPath)) {
        throw "ADB was not found at $adbPath. Install Android SDK Platform-Tools first."
    }

    $deviceOutput = & $adbPath devices
    if ($LASTEXITCODE -ne 0) { throw 'Unable to query connected Android devices.' }
    $readyDevices = @($deviceOutput | ForEach-Object {
        if ($_ -match '^(\S+)\s+device\s*$') { $Matches[1] }
    })
    if ($DeviceSerial) {
        if ($readyDevices -notcontains $DeviceSerial) {
            throw "Device $DeviceSerial is not ready. Connect it and authorize USB debugging."
        }
    } elseif ($readyDevices.Count -eq 1) {
        $DeviceSerial = $readyDevices[0]
    } elseif ($readyDevices.Count -eq 0) {
        throw 'No authorized device found. Connect your phone and allow USB debugging.'
    } else {
        throw 'Multiple devices are connected. Select one with -DeviceSerial SERIAL.'
    }

    $gradleArguments = @(':app:assembleDebug', '--console=plain', '--max-workers=4')
    if ($ProxyUrl) {
        if (-not $ProxyUrl.IsAbsoluteUri -or $ProxyUrl.Scheme -ne 'http' -or $ProxyUrl.UserInfo) {
            throw 'ProxyUrl must be an HTTP proxy URL without embedded credentials.'
        }
        $gradleArguments += @(
            "-Dhttp.proxyHost=$($ProxyUrl.Host)",
            "-Dhttp.proxyPort=$($ProxyUrl.Port)",
            "-Dhttps.proxyHost=$($ProxyUrl.Host)",
            "-Dhttps.proxyPort=$($ProxyUrl.Port)"
        )
    }

    if ($Performance) {
        $gradleArguments += '-PlinuxDoPerformanceBuild=true'
    }
    Write-Host 'Building LinuxDo test APK...'
    & '.\gradlew.bat' @gradleArguments
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. Nothing was installed.' }

    Write-Host "Installing on $DeviceSerial. Allow installation on the phone if prompted."
    & $adbPath -s $DeviceSerial install -r 'app\build\outputs\apk\debug\app-debug.apk'
    if ($LASTEXITCODE -ne 0) { throw 'Installation failed. Check the phone for a USB installation prompt.' }

    if ($Performance) {
        Write-Host 'Precompiling the test APK on the device...'
        & $adbPath -s $DeviceSerial shell cmd package compile -m speed -f 'org.linuxdo.android.debug'
        if ($LASTEXITCODE -ne 0) { throw 'The test APK was installed, but ART compilation failed.' }
    }

    $launchOutput = & $adbPath -s $DeviceSerial shell am start -W -n 'org.linuxdo.android.debug/org.linuxdo.android.MainActivity'
    $launchExitCode = $LASTEXITCODE
    $launchOutput | Write-Output
    if ($launchExitCode -ne 0 -or ($launchOutput -match 'Error:|Exception')) {
        throw 'The debug APK was installed, but the app could not be opened automatically.'
    }
    Write-Host 'LinuxDo debug is ready on your phone.'
} finally {
    Pop-Location
}
