$ErrorActionPreference = 'Stop'
$robotHome = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$failures = @()
$sources = @{}

foreach ($name in @('backup.sh', 'restore.sh')) {
    $path = Join-Path $robotHome ("scripts/db/" + $name)
    $source = Get-Content -LiteralPath $path -Raw
    $sources[$name] = $source

    if ($source.Contains('MYSQL_ROOT_PASSWORD:-root')) {
        $failures += "$name has an insecure root password fallback."
    }
    if (-not $source.Contains('MYSQL_ROOT_PASSWORD:?')) {
        $failures += "$name does not require an explicit Docker MySQL password."
    }
    if ($source -match '-e\s+MYSQL_PWD\s*=') {
        $failures += "$name places the MySQL password in Docker command-line arguments."
    }
    if (-not $source.Contains('-e MYSQL_PWD ')) {
        $failures += "$name does not inherit MYSQL_PWD from the process environment."
    }
    if (-not $source.Contains('umask 077')) {
        $failures += "$name does not default backup artifacts to owner-only permissions."
    }
    if (-not $source.Contains('chmod 700')) {
        $failures += "$name does not restrict the backup directory."
    }
    if (-not $source.Contains('chmod 600')) {
        $failures += "$name does not restrict backup files."
    }
}

$restoreSource = $sources['restore.sh']
$preBackupFailure = [regex]::Match(
    $restoreSource,
    'if\s+\[\[\s+\$\{PRE_DUMP_EXIT\}\s+-ne\s+0\s+\|\|\s+\$\{PRE_GZIP_EXIT\}\s+-ne\s+0\s+\]\];([\s\S]*?)\r?\n\s*fi'
)
if (-not $preBackupFailure.Success -or $preBackupFailure.Groups[1].Value -notmatch 'exit 1') {
    $failures += 'restore.sh must stop when either pre-restore mysqldump or gzip fails.'
}
if (-not $restoreSource.Contains('gzip -t')) {
    $failures += 'restore.sh does not validate the pre-restore backup gzip stream.'
}
$preRestoreDumps = [regex]::Matches(
    $restoreSource,
    'mysqldump([\s\S]*?)\|\s*gzip\s*>\s*"\$\{PRE_RESTORE_TEMP\}"'
)
if ($preRestoreDumps.Count -ne 2) {
    $failures += "restore.sh should define two complete pre-restore mysqldump pipelines; found $($preRestoreDumps.Count)."
}
if ([regex]::Matches($restoreSource, 'PRE_PIPE_STATUS=\("\$\{PIPESTATUS\[@\]\}"\)').Count -ne 2) {
    $failures += 'restore.sh must capture both commands in each pre-restore backup pipeline.'
}
if (-not $restoreSource.Contains('verify-backup.sh" --allow-empty-data "${PRE_RESTORE_TEMP}"')) {
    $failures += 'restore.sh does not validate the pre-restore SQL snapshot before publishing it.'
}
if (-not $restoreSource.Contains('mv -- "${PRE_RESTORE_MANIFEST_TEMP}" "${PRE_RESTORE_MANIFEST}"')) {
    $failures += 'restore.sh does not publish the pre-restore manifest before the archive.'
}
foreach ($match in $preRestoreDumps) {
    foreach ($option in @('--routines', '--triggers', '--events')) {
        if (-not $match.Groups[1].Value.Contains($option)) {
            $failures += "restore.sh pre-restore backup is missing $option."
        }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output 'PASS: backup credentials, owner-only files, and fail-closed restore safeguards are present.'
