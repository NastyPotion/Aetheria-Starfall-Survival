function Find-JavaTool {
    param([Parameter(Mandatory=$true)][string]$Name, [string]$JavaHome)
    $candidateHomes = @($JavaHome, $env:JAVA_HOME,
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'))
    foreach ($candidateHome in $candidateHomes) {
        if ($candidateHome) {
            $candidateTool = Join-Path $candidateHome ('bin\' + $Name + '.exe')
            if (Test-Path -LiteralPath $candidateTool -PathType Leaf) { return $candidateTool }
        }
    }
    $command = Get-Command ($Name + '.exe') -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    throw "Java 21 JDK required. Install a JDK and set JAVA_HOME, or pass -JavaHome 'C:\path\to\jdk-21'."
}

function Assert-ExitCode {
    param([string]$Operation)
    if ($LASTEXITCODE -ne 0) { throw "$Operation failed (exit code $LASTEXITCODE)." }
}
