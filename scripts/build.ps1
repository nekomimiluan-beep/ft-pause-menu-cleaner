param(
    [string]$MinecraftDir = $(if ($env:MINECRAFT_HOME) { $env:MINECRAFT_HOME } else { Join-Path $env:APPDATA ".minecraft" }),
    [string]$Version = "1.2.11"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$MinecraftDir = (Resolve-Path $MinecraftDir).Path
$ClassesDir = Join-Path $Root "build\classes"
$OutputDir = Join-Path $Root "build\libs"
$JarPath = Join-Path $OutputDir "ft-pause-menu-cleaner-neoforge-1.21.1-$Version.jar"

function Resolve-JavaTool {
    param([string]$Name)

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$Name.exe"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "Cannot find $Name. Install JDK 21 or set JAVA_HOME."
}

function Add-JarsFrom {
    param(
        [System.Collections.Generic.List[string]]$Target,
        [string]$Path,
        [switch]$Recurse
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    $options = @{
        LiteralPath = $Path
        Filter = "*.jar"
        File = $true
    }
    if ($Recurse) {
        $options.Recurse = $true
    }

    Get-ChildItem @options | ForEach-Object {
        $Target.Add($_.FullName)
    }
}

$javac = Resolve-JavaTool "javac"
$jar = Resolve-JavaTool "jar"

$jars = [System.Collections.Generic.List[string]]::new()
Add-JarsFrom $jars (Join-Path $MinecraftDir "libraries") -Recurse
Add-JarsFrom $jars (Join-Path $MinecraftDir "versions") -Recurse
Add-JarsFrom $jars (Join-Path $MinecraftDir "mods")
Add-JarsFrom $jars (Join-Path $MinecraftDir "mods\1.21.1") -Recurse

if ($jars.Count -eq 0) {
    throw "No Minecraft/NeoForge dependency jars found under $MinecraftDir."
}

$sources = Get-ChildItem -LiteralPath (Join-Path $Root "src\main\java") -Filter "*.java" -Recurse -File |
    ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found."
}

if (Test-Path -LiteralPath $ClassesDir) {
    Remove-Item -LiteralPath $ClassesDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $ClassesDir, $OutputDir | Out-Null

$classpath = [string]::Join([System.IO.Path]::PathSeparator, $jars)
& $javac -encoding UTF-8 -classpath $classpath -d $ClassesDir $sources

$resources = Join-Path $Root "src\main\resources"
if (Test-Path -LiteralPath $resources) {
    Copy-Item -LiteralPath (Join-Path $resources "*") -Destination $ClassesDir -Recurse -Force
}

if (Test-Path -LiteralPath $JarPath) {
    Remove-Item -LiteralPath $JarPath -Force
}
& $jar --create --file $JarPath -C $ClassesDir .

Write-Host "Built: $JarPath"
