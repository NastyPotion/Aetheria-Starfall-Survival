param([string]$JavaHome = '')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'scripts\java.ps1')
$compiler = Find-JavaTool -Name 'javac' -JavaHome $JavaHome
$classes = Join-Path $PSScriptRoot 'build\classes'
if ([System.IO.Path]::GetFullPath((Split-Path -Parent $classes)) -ne [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'build'))) {
    throw 'The generated classes folder must stay inside this project build folder.'
}
if (Test-Path -LiteralPath $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$sourceFiles = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Filter '*.java' -Recurse |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
$argumentFile = Join-Path $PSScriptRoot 'build\sources.txt'
[System.IO.File]::WriteAllLines($argumentFile, $sourceFiles, [System.Text.UTF8Encoding]::new($false))
& $compiler --release 21 -encoding UTF-8 -Xlint:all -Werror -d $classes ('@' + $argumentFile)
Assert-ExitCode -Operation 'Java compilation'
$resourceRoot = Join-Path $PSScriptRoot 'src'
Get-ChildItem -LiteralPath $resourceRoot -Recurse -File | Where-Object { $_.Extension -ne '.java' } | ForEach-Object {
    $relativeResource = $_.FullName.Substring($resourceRoot.Length + 1)
    $targetResource = Join-Path $classes $relativeResource
    New-Item -ItemType Directory -Path (Split-Path -Parent $targetResource) -Force | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $targetResource -Force
}
Write-Host "Built Java classes and resources in $classes"
