$ErrorActionPreference = "Stop"

Write-Host "Building Free Mouse Lock..."

if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat build
} elseif (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle build
} else {
    Write-Host "Gradle is not installed and this zip does not include a wrapper jar."
    Write-Host "Install Gradle 9.2+ or open the project in IntelliJ and let it import Gradle."
    exit 1
}
