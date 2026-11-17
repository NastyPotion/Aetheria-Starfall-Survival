param([string]$JavaHome = '')
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1') -JavaHome $JavaHome
. (Join-Path $PSScriptRoot 'scripts\java.ps1')
$jarTool = Find-JavaTool -Name 'jar' -JavaHome $JavaHome
$distribution = Join-Path $PSScriptRoot 'dist'
New-Item -ItemType Directory -Path $distribution -Force | Out-Null
$jarPath = Join-Path $distribution 'AetheriaCrashLandings.jar'
& $jarTool --create --file $jarPath --main-class com.game.core.Main -C (Join-Path $PSScriptRoot 'build\classes') .
Assert-ExitCode -Operation 'JAR packaging'
$included = @('src', 'res', 'docs', 'scripts', 'tests', 'tools', '.settings', '.classpath', '.project',
    '.gitignore', 'README.md', 'AGENTS.md', 'build.ps1', 'run.ps1', 'test.ps1', 'package.ps1') |
    ForEach-Object { Join-Path $PSScriptRoot $_ }
if (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'output\pdf')) {
    $included += Join-Path $PSScriptRoot 'output'
}
$submissionPath = Join-Path $distribution 'AetheriaCrashLandings-submission.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archiveStream = [System.IO.File]::Open($submissionPath, [System.IO.FileMode]::Create)
$archive = [System.IO.Compression.ZipArchive]::new($archiveStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($includedPath in $included) {
        $includedItem = Get-Item -LiteralPath $includedPath
        $files = if ($includedItem.PSIsContainer) { Get-ChildItem -LiteralPath $includedPath -Recurse -File } else { @($includedItem) }
        foreach ($file in $files) {
            $entryName = $file.FullName.Substring($PSScriptRoot.Length + 1).Replace('\', '/')
            if ($entryName -match '(^|/)__pycache__/' -or $file.Extension -match '^\.py[cod]$') { continue }
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $entryName, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
    [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $jarPath, 'AetheriaCrashLandings.jar', [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
} finally {
    $archive.Dispose()
    $archiveStream.Dispose()
}
Write-Host "Runnable JAR: $jarPath"
Write-Host "Submission archive: $(Join-Path $distribution 'AetheriaCrashLandings-submission.zip')"
