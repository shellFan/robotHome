param(
    [Parameter(Mandatory = $true)]
    [string] $ServerJar,
    [Parameter(Mandatory = $true)]
    [string] $CollectorJar,
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string] $Environment,
    [Parameter(Mandatory = $true)]
    [string] $OutputPath,
    [string] $DeployTime = 'UNKNOWN'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Read-JarProperties {
    param([string] $Path, [string] $EntryName)

    $archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Path).Path)
    try {
        $entry = $archive.GetEntry($EntryName)
        if ($null -eq $entry) {
            throw "JAR metadata entry missing: $EntryName ($Path)"
        }
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try {
            $values = @{}
            while (($line = $reader.ReadLine()) -ne $null) {
                if ($line -match '^([^#!][^=]*?)=(.*)$') {
                    $values[$matches[1].Trim()] = $matches[2].Trim() -replace '\\:', ':'
                }
            }
            return $values
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $archive.Dispose()
    }
}

function Get-ArtifactManifest {
    param([string] $Name, [string] $Path)

    $resolvedPath = (Resolve-Path -LiteralPath $Path).Path
    $git = Read-JarProperties -Path $resolvedPath -EntryName 'BOOT-INF/classes/git.properties'
    $build = Read-JarProperties -Path $resolvedPath -EntryName 'META-INF/build-info.properties'
    [ordered]@{
        artifact = $Name
        file = [System.IO.Path]::GetFileName($resolvedPath)
        sha256 = (Get-FileHash -LiteralPath $resolvedPath -Algorithm SHA256).Hash.ToLowerInvariant()
        gitSha = if ($git['git.commit.id.full']) { $git['git.commit.id.full'] } else { 'UNKNOWN' }
        gitBranch = if ($git['git.branch']) { $git['git.branch'] } else { 'UNKNOWN' }
        gitDirty = if ($git.ContainsKey('git.dirty')) { $git['git.dirty'] } else { 'UNKNOWN' }
        buildTime = if ($build['build.time']) { $build['build.time'] } else { 'UNKNOWN' }
        artifactVersion = if ($build['build.version']) { $build['build.version'] } else { 'UNKNOWN' }
    }
}

$manifest = [ordered]@{
    environment = $Environment
    deployTime = $DeployTime
    generatedAt = [DateTime]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
    artifacts = @(
        (Get-ArtifactManifest -Name 'server' -Path $ServerJar),
        (Get-ArtifactManifest -Name 'collector' -Path $CollectorJar)
    )
}

$outputFile = [System.IO.Path]::GetFullPath($OutputPath)
$outputDirectory = [System.IO.Path]::GetDirectoryName($outputFile)
if (-not [System.IO.Directory]::Exists($outputDirectory)) {
    [System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
}
$manifest | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $outputFile -Encoding UTF8
Write-Output "Manifest written: $outputFile"
