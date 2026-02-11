$sourcePath = "e:\PlateIt"
$destinationZip = "e:\PlateIt\PlateIt_Submission.zip"
$tempDir = "e:\PlateIt_Temp_Export"

# Clean up previous runs
if (Test-Path $destinationZip) { Remove-Item $destinationZip -Force }
if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }

New-Item -ItemType Directory -Path $tempDir | Out-Null
Write-Host "Preparing clean source export..."

# LIST OF ITEMS TO COPY EXPLICITLY 
# (This whitelist approach is safer than blacklisting)
$whitelist = @(
    "app\src",
    "app\build.gradle.kts",
    "app\proguard-rules.pro",
    "BackEnd", 
    "gradle",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradlew",
    "gradlew.bat",
    "gradle.properties", 
    "README.md",
    "GEMINI.md",
    "USER_MANUAL.md",
    ".gitignore"
)

foreach ($item in $whitelist) {
    $src = Join-Path $sourcePath $item
    $dest = Join-Path $tempDir $item

    if (Test-Path $src) {
        if ((Get-Item $src).PSIsContainer) {
            # Copy Directory Structure
            Write-Host "Copying Folder: $item"
            # Exclude nested build artifacts/caches within these folders if any
            # For BackEnd, we need to be careful of venv/__pycache__
            if ($item -eq "BackEnd") {
                 New-Item -ItemType Directory -Path $dest -Force | Out-Null
                 # Copy sub-content excluding garbage
                 Get-ChildItem $src -Recurse | Where-Object { 
                    ($_.FullName -notmatch "__pycache__") -and 
                    ($_.FullName -notmatch "venv") -and 
                    ($_.FullName -notmatch "\.env") -and
                    ($_.FullName -notmatch "\.pytest_cache") -and
                    ($_.FullName -notmatch "\.git")
                 } | Copy-Item -Destination { 
                    $_.FullName.Replace($src, $dest) 
                 } -Force -Container
            }
            else {
                 Copy-Item $src -Destination (Split-Path $dest -Parent) -Recurse -Force
            }
        } else {
            # Copy File
            Write-Host "Copying File: $item"
            Copy-Item $src -Destination $dest -Force
        }
    }
}

Write-Host "Compressing to $destinationZip ..."
Compress-Archive -Path "$tempDir\*" -DestinationPath $destinationZip

# Final Check
$size = (Get-Item $destinationZip).Length / 1MB
Write-Host "------------------------------------------------"
Write-Host "SUCCESS! Created clean submission zip."
Write-Host "Location: $destinationZip"
Write-Host "Final Size: $([math]::Round($size, 2)) MB"
Write-Host "------------------------------------------------"

# Cleanup
Remove-Item $tempDir -Recurse -Force
