$ErrorActionPreference = "Stop"

$sourceFiles = Get-ChildItem -Recurse -Path "src\main\java" -Filter "*.java" | ForEach-Object { $_.FullName }

if (-not (Test-Path "out")) {
    New-Item -ItemType Directory -Path "out" | Out-Null
}

javac -d out $sourceFiles
java -cp out com.naik.NaikApp
