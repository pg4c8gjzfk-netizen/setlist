[CmdletBinding()]
param(
    [string]$MavenCommand = "mvn.cmd",
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$SkipTests
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$maven = Get-Command -Name $MavenCommand -ErrorAction Stop

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $jpackageCommand = Get-Command -Name "jpackage.exe" -ErrorAction Stop
    $jpackage = $jpackageCommand.Source
} else {
    $jpackage = Join-Path $JavaHome "bin\jpackage.exe"
    if (-not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
        throw "jpackage.exe was not found under JAVA_HOME: $jpackage"
    }
    $env:JAVA_HOME = $JavaHome
    $env:Path = (Join-Path $JavaHome "bin") + ";" + $env:Path
}

Push-Location $projectRoot
try {
    $mavenArguments = @("clean", "package")
    if ($SkipTests) {
        $mavenArguments += "-DskipTests"
    }

    & $maven.Source @mavenArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }

    $mainJar = Join-Path $projectRoot "target\setlist-studio.jar"
    $packageInput = Join-Path $projectRoot "target\package-input"
    if (-not (Test-Path -LiteralPath $mainJar -PathType Leaf)) {
        throw "Main JAR was not found: $mainJar"
    }
    if (-not (Test-Path -LiteralPath $packageInput -PathType Container)) {
        throw "Runtime dependencies were not found: $packageInput"
    }
    Copy-Item -LiteralPath $mainJar -Destination $packageInput -Force

    $distributionDirectory = Join-Path $projectRoot "target\dist"
    & $jpackage `
        --type app-image `
        --name SetlistStudio `
        --app-version 1.0.0 `
        --vendor "Setlist Studio" `
        --description "Desktop setlist editor and generator for XLSX workbooks" `
        --input $packageInput `
        --main-jar "setlist-studio.jar" `
        --main-class "jp.ac.u_tokai.cc.javaadvanced.App" `
        --dest $distributionDirectory
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE"
    }

    $appImage = Join-Path $distributionDirectory "SetlistStudio"
    Copy-Item -LiteralPath "README.md" -Destination (Join-Path $appImage "README.md") -Force

    $buildInformation = @(
        "Setlist Studio 1.0.0",
        "Built: $([DateTimeOffset]::Now.ToString('o'))",
        "Java: $(& $jpackage --version)"
    )
    Set-Content `
        -LiteralPath (Join-Path $appImage "build-info.txt") `
        -Value $buildInformation `
        -Encoding UTF8

    $zipFile = Join-Path $distributionDirectory "SetlistStudio-1.0.0-windows-x64.zip"
    Compress-Archive -LiteralPath $appImage -DestinationPath $zipFile -CompressionLevel Optimal

    Write-Host "Windows distribution package created."
    Write-Host $zipFile
} finally {
    Pop-Location
}
