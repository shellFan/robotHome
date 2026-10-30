$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.IO.Compression.FileSystem

$scriptPath = Join-Path (Split-Path $PSScriptRoot -Parent) 'New-DeploymentManifest.ps1'
$tempRoot = Join-Path $env:TEMP ('robot-home-manifest-test-' + [Guid]::NewGuid().ToString('N'))
[void][System.IO.Directory]::CreateDirectory($tempRoot)

function Add-ZipTextEntry {
    param(
        [System.IO.Compression.ZipArchive] $Archive,
        [string] $Name,
        [string] $Text
    )

    $entry = $Archive.CreateEntry($Name)
    $writer = [System.IO.StreamWriter]::new($entry.Open(), [System.Text.Encoding]::UTF8)
    try {
        $writer.Write($Text)
    }
    finally {
        $writer.Dispose()
    }
}

function New-TestJar {
    param(
        [string] $Path,
        [string] $Dirty
    )

    $archive = [System.IO.Compression.ZipFile]::Open($Path, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        Add-ZipTextEntry -Archive $archive -Name 'BOOT-INF/classes/git.properties' -Text @"
git.commit.id.full=0123456789abcdef0123456789abcdef01234567
git.branch=codex/phase16-content-quality
git.dirty=$Dirty
"@
        Add-ZipTextEntry -Archive $archive -Name 'META-INF/build-info.properties' -Text @'
build.time=2026-10-29T00:00:00Z
build.version=1.0.0
'@
    }
    finally {
        $archive.Dispose()
    }
}

try {
    $serverJar = Join-Path $tempRoot 'server.jar'
    $collectorJar = Join-Path $tempRoot 'collector.jar'
    $manifestPath = Join-Path $tempRoot 'manifest.json'
    New-TestJar -Path $serverJar -Dirty 'true'
    New-TestJar -Path $collectorJar -Dirty 'false'

    & $scriptPath -ServerJar $serverJar -CollectorJar $collectorJar `
        -Environment 'test' -OutputPath $manifestPath -DeployTime 'NOT_DEPLOYED'
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json

    if ($manifest.artifacts[0].gitDirty -ne 'true') {
        throw 'Server artifact dirty state was not included in the deployment manifest.'
    }
    if ($manifest.artifacts[1].gitDirty -ne 'false') {
        throw 'Collector artifact clean state was not included in the deployment manifest.'
    }

    Write-Output 'PASS: manifest preserves dirty and clean Git working-tree metadata.'
}
finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}
