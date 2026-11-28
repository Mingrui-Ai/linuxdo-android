param(
    [Parameter(Mandatory)][string]$DeviceSerial,
    [string]$Adb = 'adb',
    [int]$AdbPort = 5037
)
$ErrorActionPreference = 'Stop'
$cases = [ordered]@{ poll = 6; busy = 1; body = 1; reset = 1; transport = 3 }
foreach ($group in $cases.Keys) {
    $lines = @(& $Adb -P $AdbPort -s $DeviceSerial shell am instrument -w -r -e group $group org.linuxdo.android.debug.test/localqa.PollChecks)
    $exitCode = $LASTEXITCODE
    $lines
    $passed = @($lines -match '^INSTRUMENTATION_RESULT: \w+=PASS\b').Count
    if ($exitCode -ne 0 -or $passed -ne $cases[$group] -or -not ($lines -match '^INSTRUMENTATION_CODE: -1\s*$')) {
        throw "Instrumentation group '$group' did not pass ($passed/$($cases[$group]))."
    }
}
