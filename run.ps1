param([string]$JavaHome = '')
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'scripts\build.ps1') -JavaHome $JavaHome
. (Join-Path $PSScriptRoot 'scripts\java.ps1')
$java = Find-JavaTool -Name 'java' -JavaHome $JavaHome
& $java -cp (Join-Path $PSScriptRoot 'build\classes') com.game.core.Main
Assert-ExitCode -Operation 'Game'
