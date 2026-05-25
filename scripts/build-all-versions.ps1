param(
    [string]$ReportPath = "build/version-build-report.txt",
    [switch]$CleanEach,
    [string]$JavaHome
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-BuildReport {
    param(
        [Parameter(Mandatory = $true)]
        [System.Collections.IEnumerable]$Results,

        [Parameter(Mandatory = $true)]
        [datetime]$StartedAt,

        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $resultList = @($Results)
    $successCount = @($resultList | Where-Object { $_.Status -eq 'SUCCESS' }).Count
    $failureCount = @($resultList | Where-Object { $_.Status -eq 'FAILED' }).Count
    $updatedAt = Get-Date

    $lines = @(
        'TaskManager version build report',
        "Started: $($StartedAt.ToString('o'))",
        "Updated: $($updatedAt.ToString('o'))",
        "Succeeded: $successCount",
        "Failed: $failureCount",
        '',
        'Status | Minecraft | Module | ExitCode | Duration',
        '------ | --------- | ------ | -------- | --------'
    )

    if ($resultList.Count -eq 0) {
        $lines += 'PENDING | - | - | - | -'
    } else {
        foreach ($result in $resultList) {
            $lines += ('{0} | {1} | {2} | {3} | {4}' -f $result.Status, $result.Minecraft, $result.Module, $result.ExitCode, $result.Duration)
        }
    }

    $reportDirectory = Split-Path -Parent $Path
    if ($reportDirectory) {
        New-Item -ItemType Directory -Force -Path $reportDirectory | Out-Null
    }

    Set-Content -Path $Path -Encoding UTF8 -Value $lines
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..'))
$isWindowsHost = [System.IO.Path]::PathSeparator -eq ';'

if ($isWindowsHost) {
    $gradleWrapper = Join-Path $repoRoot 'gradlew.bat'
} else {
    $gradleWrapper = Join-Path $repoRoot 'gradlew'
}

if (-not (Test-Path $gradleWrapper)) {
    throw "Could not find Gradle wrapper at $gradleWrapper"
}

if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome/bin$([System.IO.Path]::PathSeparator)$env:Path"
}

if (-not [System.IO.Path]::IsPathRooted($ReportPath)) {
    $ReportPath = Join-Path $repoRoot $ReportPath
}

$startedAt = Get-Date
$results = New-Object System.Collections.Generic.List[object]

Push-Location $repoRoot
try {
    $moduleLines = & $gradleWrapper '-q' 'printVersionModules' 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to read version modules from Gradle. Exit code: $LASTEXITCODE`n$($moduleLines -join [Environment]::NewLine)"
    }

    $targets = foreach ($line in $moduleLines) {
        $text = "$line".Trim()
        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }

        if ($text -notmatch '^(mc[0-9_]+)\|([0-9][0-9.]*)$') {
            continue
        }

        [pscustomobject]@{
            Module = $Matches[1]
            Minecraft = $Matches[2]
        }
    }

    if (@($targets).Count -eq 0) {
        throw "No version modules were discovered from Gradle output.`n$($moduleLines -join [Environment]::NewLine)"
    }

    Write-BuildReport -Results $results -StartedAt $startedAt -Path $ReportPath

    foreach ($target in $targets) {
        $taskArgs = @()
        if ($CleanEach) {
            $taskArgs += ":$($target.Module):clean"
        }
        $taskArgs += ":$($target.Module):build"
        $taskArgs += '--configure-on-demand'
        $taskArgs += '--console=plain'
        $taskArgs += '--no-daemon'

        $moduleStartedAt = Get-Date
        Write-Host ("[{0}] Building {1} ({2})" -f $moduleStartedAt.ToString('HH:mm:ss'), $target.Minecraft, $target.Module)

        & $gradleWrapper @taskArgs
        $exitCode = $LASTEXITCODE
        $duration = [datetime]::UtcNow - $moduleStartedAt.ToUniversalTime()
        $status = if ($exitCode -eq 0) { 'SUCCESS' } else { 'FAILED' }

        $results.Add([pscustomobject]@{
            Status = $status
            Minecraft = $target.Minecraft
            Module = $target.Module
            ExitCode = $exitCode
            Duration = $duration.ToString('hh\:mm\:ss')
        })

        Write-BuildReport -Results $results -StartedAt $startedAt -Path $ReportPath
    }

    $failedResults = @($results | Where-Object { $_.Status -eq 'FAILED' })
    if ($failedResults.Count -gt 0) {
        Write-Host "Finished with failures. See $ReportPath"
        exit 1
    }

    Write-Host "Finished successfully. See $ReportPath"
}
finally {
    Pop-Location
}