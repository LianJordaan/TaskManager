<#
.SYNOPSIS
    Uploads TaskManager build artifacts to Modrinth as separate versions
    (one Modrinth version per Minecraft version).

.DESCRIPTION
    Reads the Modrinth token from the repo .env file (MODRINTH_TOKEN), resolves
    the project id from its slug, locates the built jar for each requested
    Minecraft version and publishes it with `POST /v2/version`.

    Each Minecraft version becomes its own Modrinth version, numbered
    "<mod_version>+mc<minecraft>" (e.g. 1.0.0+mc1.20.1), matching the version
    string baked into fabric.mod.json by the Gradle build.

.PARAMETER McVersion
    A single Minecraft version to upload, e.g. "1.20.1". Ignored when -All is set.

.PARAMETER All
    Upload every Minecraft version defined in build.gradle's version matrix.

.PARAMETER VersionType
    release | beta | alpha. Defaults to release.

.PARAMETER Changelog
    Changelog text for the uploaded version(s).

.PARAMETER ChangelogFile
    Path to a file whose contents are used as the changelog (overrides -Changelog).

.PARAMETER Build
    Run the Gradle build for each target before uploading.

.PARAMETER Featured
    Mark the uploaded version(s) as featured on the project page.

.PARAMETER NoFabricApiDependency
    Do not declare the required dependency on Fabric API.

.PARAMETER DryRun
    Print exactly what would be sent (metadata + jar path) without calling the API.

.PARAMETER ProjectId
    Project slug or id. Defaults to "task-manager".

.EXAMPLE
    .\scripts\upload-modrinth.ps1 -McVersion 1.20.1 -DryRun

.EXAMPLE
    .\scripts\upload-modrinth.ps1 -McVersion 1.21.11 -Build

.EXAMPLE
    .\scripts\upload-modrinth.ps1 -All
#>
[CmdletBinding(DefaultParameterSetName = 'Single')]
param(
    [Parameter(ParameterSetName = 'Single', Position = 0)]
    [string]$McVersion,

    [Parameter(ParameterSetName = 'All')]
    [switch]$All,

    [ValidateSet('release', 'beta', 'alpha')]
    [string]$VersionType = 'release',

    [string]$Changelog = 'Initial 1.0.0 release of TaskManager.',

    [string]$ChangelogFile,

    [switch]$Build,

    [switch]$Featured,

    [switch]$NoFabricApiDependency,

    [switch]$DryRun,

    [string]$ProjectId = 'task-manager'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# Modrinth project id for Fabric API.
$FabricApiProjectId = 'P7dR8mSH'
$ApiBase = 'https://api.modrinth.com/v2'
$UserAgent = 'LianJordaan/TaskManager-release-script'

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..'))

function Read-DotEnv {
    param([string]$Path)
    $values = @{}
    if (-not (Test-Path $Path)) { return $values }
    foreach ($line in Get-Content -Path $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
        $idx = $trimmed.IndexOf('=')
        if ($idx -lt 1) { continue }
        $key = $trimmed.Substring(0, $idx).Trim()
        $val = $trimmed.Substring($idx + 1).Trim().Trim('"').Trim("'")
        $values[$key] = $val
    }
    return $values
}

function Get-ModVersion {
    $propsPath = Join-Path $repoRoot 'gradle.properties'
    foreach ($line in Get-Content -Path $propsPath) {
        if ($line -match '^\s*mod_version\s*=\s*(.+?)\s*$') { return $Matches[1] }
    }
    throw "Could not read mod_version from $propsPath"
}

function Get-AllMcVersions {
    $buildGradle = Join-Path $repoRoot 'build.gradle'
    $content = Get-Content -Path $buildGradle -Raw
    $matchList = [regex]::Matches($content, "minecraft:\s*'([^']+)'")
    $versions = foreach ($m in $matchList) { $m.Groups[1].Value }
    return @($versions)
}

function Get-ModuleName {
    param([string]$Mc)
    return 'mc' + ($Mc -replace '\.', '_')
}

function Invoke-Build {
    param([string]$Module)
    $wrapper = if ([System.IO.Path]::PathSeparator -eq ';') {
        Join-Path $repoRoot 'gradlew.bat'
    } else {
        Join-Path $repoRoot 'gradlew'
    }
    if (-not (Test-Path $wrapper)) { throw "Gradle wrapper not found at $wrapper" }
    Write-Host "  Building :$Module:build ..." -ForegroundColor Cyan
    Push-Location $repoRoot
    try {
        & $wrapper ":$Module:build" '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed for :$Module (exit $LASTEXITCODE)" }
    } finally {
        Pop-Location
    }
}

function Resolve-ProjectId {
    param([string]$SlugOrId, [string]$Token)
    $headers = @{ Authorization = $Token; 'User-Agent' = $UserAgent }
    $project = Invoke-RestMethod -Method Get -Uri "$ApiBase/project/$SlugOrId" -Headers $headers
    return $project.id
}

# ---------------------------------------------------------------------------

$modVersion = Get-ModVersion

if ($ChangelogFile) {
    if (-not (Test-Path $ChangelogFile)) { throw "Changelog file not found: $ChangelogFile" }
    $Changelog = Get-Content -Path $ChangelogFile -Raw
}

# Determine target Minecraft versions.
$allMc = Get-AllMcVersions
if ($All) {
    $targets = $allMc
} else {
    if (-not $McVersion) {
        throw "Specify a Minecraft version with -McVersion <ver>, or use -All. Known: $($allMc -join ', ')"
    }
    if ($allMc -notcontains $McVersion) {
        throw "Unknown Minecraft version '$McVersion'. Known: $($allMc -join ', ')"
    }
    $targets = @($McVersion)
}

# Load the token (not required for dry runs).
$dotenv = Read-DotEnv -Path (Join-Path $repoRoot '.env')
$token = if ($dotenv.ContainsKey('MODRINTH_TOKEN')) { $dotenv['MODRINTH_TOKEN'] } else { '' }
if (-not $DryRun -and [string]::IsNullOrWhiteSpace($token)) {
    throw "MODRINTH_TOKEN is empty. Add it to $(Join-Path $repoRoot '.env')."
}

# Resolve the real project id from the slug (validates token + project).
# Draft projects can't be read via the slug, and a create-versions-only token
# can't read projects either, so fall back to using the supplied value as-is.
$resolvedProjectId = $ProjectId
if (-not $DryRun) {
    if ($ProjectId -match '^[A-Za-z0-9]{8}$') {
        Write-Host "Using project id '$ProjectId' directly." -ForegroundColor DarkGray
    } else {
        try {
            $resolvedProjectId = Resolve-ProjectId -SlugOrId $ProjectId -Token $token
            Write-Host "Project '$ProjectId' resolved to id '$resolvedProjectId'." -ForegroundColor DarkGray
        } catch {
            Write-Host "Could not resolve '$ProjectId' via API ($($_.Exception.Message))." -ForegroundColor Yellow
            Write-Host "Using '$ProjectId' as-is. For a draft project, pass the 8-char Project ID via -ProjectId <id>." -ForegroundColor Yellow
            $resolvedProjectId = $ProjectId
        }
    }
}

$curlExe = (Get-Command curl.exe -ErrorAction Stop).Source

$dependencies = @()
if (-not $NoFabricApiDependency) {
    $dependencies += @{ project_id = $FabricApiProjectId; dependency_type = 'required' }
}

Write-Host ""
Write-Host "TaskManager -> Modrinth ($($targets.Count) version(s), mod_version=$modVersion, type=$VersionType)" -ForegroundColor Green
if ($DryRun) { Write-Host "DRY RUN - nothing will be uploaded." -ForegroundColor Yellow }
Write-Host ""

$results = New-Object System.Collections.Generic.List[object]
$tempFiles = New-Object System.Collections.Generic.List[string]

try {
    foreach ($mc in $targets) {
        $module = Get-ModuleName -Mc $mc
        Write-Host "=== Minecraft $mc ($module) ===" -ForegroundColor White

        if ($Build) { Invoke-Build -Module $module }

        $jarPath = Join-Path $repoRoot "versions/$mc/build/libs/taskmanager-$mc-$modVersion.jar"
        $jarExists = Test-Path $jarPath

        $data = [ordered]@{
            name           = "TaskManager $modVersion for Minecraft $mc"
            version_number = "$modVersion+mc$mc"
            changelog      = $Changelog
            dependencies   = $dependencies
            game_versions  = @($mc)
            version_type   = $VersionType
            loaders        = @('fabric')
            featured       = [bool]$Featured
            project_id     = $resolvedProjectId
            file_parts     = @('file')
            primary_file   = 'file'
        }
        $json = $data | ConvertTo-Json -Depth 6

        if (-not $jarExists) {
            Write-Host "  Jar not found: $jarPath" -ForegroundColor Red
            Write-Host "  Build it first (./gradlew :${module}:build) or pass -Build." -ForegroundColor Red
            $results.Add([pscustomobject]@{ Minecraft = $mc; Status = 'NO_JAR'; Detail = $jarPath })
            if ($DryRun) { Write-Host "  Would send:`n$json`n" -ForegroundColor DarkGray }
            continue
        }

        if ($DryRun) {
            Write-Host "  Jar:  $jarPath"
            Write-Host "  Data:`n$json`n" -ForegroundColor DarkGray
            $results.Add([pscustomobject]@{ Minecraft = $mc; Status = 'DRY_RUN'; Detail = "$modVersion+mc$mc" })
            continue
        }

        # Write JSON metadata to a UTF-8 (no BOM) temp file for curl's -F data field.
        $dataFile = [System.IO.Path]::GetTempFileName()
        $tempFiles.Add($dataFile)
        [System.IO.File]::WriteAllText($dataFile, $json, (New-Object System.Text.UTF8Encoding($false)))

        # Keep the token out of the visible command line via a curl --config file.
        $authFile = [System.IO.Path]::GetTempFileName()
        $tempFiles.Add($authFile)
        [System.IO.File]::WriteAllText($authFile, "header = `"Authorization: $token`"`n", (New-Object System.Text.UTF8Encoding($false)))

        $curlArgs = @(
            '--silent', '--show-error',
            '--config', $authFile,
            '-A', $UserAgent,
            '-w', "`n%{http_code}",
            '-X', 'POST',
            '-F', "data=@$dataFile;type=application/json",
            '-F', "file=@$jarPath;type=application/java-archive",
            "$ApiBase/version"
        )

        Write-Host "  Uploading $jarPath ..." -ForegroundColor Cyan
        $raw = @(& $curlExe @curlArgs)
        $curlExit = $LASTEXITCODE

        $httpCode = $raw[-1]
        $body = if ($raw.Count -gt 1) { ($raw[0..($raw.Count - 2)] -join "`n") } else { '' }

        if ($curlExit -eq 0 -and $httpCode -match '^20[01]$') {
            $versionId = ''
            try { $versionId = (ConvertFrom-Json $body).id } catch {}
            Write-Host "  OK ($httpCode) version id: $versionId" -ForegroundColor Green
            $results.Add([pscustomobject]@{ Minecraft = $mc; Status = 'UPLOADED'; Detail = $versionId })
        } else {
            Write-Host "  FAILED (curl exit $curlExit, http $httpCode)" -ForegroundColor Red
            Write-Host "  $body" -ForegroundColor Red
            $results.Add([pscustomobject]@{ Minecraft = $mc; Status = 'FAILED'; Detail = "http $httpCode" })
        }
        Write-Host ""
    }
}
finally {
    foreach ($f in $tempFiles) {
        if (Test-Path $f) { Remove-Item -Path $f -Force -ErrorAction SilentlyContinue }
    }
}

Write-Host "----- Summary -----" -ForegroundColor White
$results | Format-Table -AutoSize | Out-String | Write-Host

$failed = @($results | Where-Object { $_.Status -in @('FAILED', 'NO_JAR') })
if ($failed.Count -gt 0) { exit 1 }
