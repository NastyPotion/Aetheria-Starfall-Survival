param([string]$JavaHome = '')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1') -JavaHome $JavaHome
. (Join-Path $PSScriptRoot 'java.ps1')
$jarTool = Find-JavaTool -Name 'jar' -JavaHome $JavaHome
$distribution = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Path $distribution -Force | Out-Null
$jarPath = Join-Path $distribution 'AetheriaCrashLandings.jar'
& $jarTool --create --file $jarPath --main-class com.game.core.Main -C (Join-Path $projectRoot 'build\classes') .
Assert-ExitCode -Operation 'JAR packaging'

# Use explicit paths so local notes and generated files never enter a release.
$included = @('src', 'scripts', 'tests', '.settings', '.classpath', '.project', '.gitignore',
    'README.md', 'run.ps1', 'docs\USER_GUIDE.md', 'docs\DEVELOPMENT.md',
    'docs\ART_DIRECTION.md', 'docs\TITLE_ART_PROMPT.txt', 'docs\images')
$sourcePath = Join-Path $distribution 'Aetheria-Starfall-Survival-source.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archiveStream = [System.IO.File]::Open($sourcePath, [System.IO.FileMode]::Create)
$archive = [System.IO.Compression.ZipArchive]::new($archiveStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($relativePath in $included) {
        $item = Get-Item -LiteralPath (Join-Path $projectRoot $relativePath)
        $files = if ($item.PSIsContainer) { Get-ChildItem -LiteralPath $item.FullName -Recurse -File } else { @($item) }
        foreach ($file in $files) {
            $entryName = $file.FullName.Substring($projectRoot.Length + 1).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $file.FullName, $entryName, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    }
} finally {
    $archive.Dispose()
    $archiveStream.Dispose()
}
Write-Host "Game: $jarPath"
Write-Host "Source project: $sourcePath"
