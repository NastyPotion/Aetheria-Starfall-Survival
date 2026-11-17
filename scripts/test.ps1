param([string]$JavaHome = '')
$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1') -JavaHome $JavaHome
$projectRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'java.ps1')
$compiler = Find-JavaTool -Name 'javac' -JavaHome $JavaHome
$java = Find-JavaTool -Name 'java' -JavaHome $JavaHome
$classes = Join-Path $projectRoot 'build\classes'
$testClasses = Join-Path $projectRoot 'build\test-classes'
New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'tests') -Filter '*.java' -Recurse |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
$argumentFile = Join-Path $projectRoot 'build\test-sources.txt'
[System.IO.File]::WriteAllLines($argumentFile, $testSources, [System.Text.UTF8Encoding]::new($false))
& $compiler --release 21 -encoding UTF-8 -Xlint:all -Werror -cp $classes -d $testClasses ('@' + $argumentFile)
Assert-ExitCode -Operation 'Test compilation'
Push-Location $projectRoot
try {
    foreach ($suite in @('PersistenceTest', 'CampaignTest', 'SurvivalCombatTest', 'SurvivalUITest', 'FeedbackTest')) {
        & $java '-Djava.awt.headless=true' -cp ($classes + ';' + $testClasses) ('com.game.tests.' + $suite)
        Assert-ExitCode -Operation $suite
    }
} finally { Pop-Location }
