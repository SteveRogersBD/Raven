$sourcePath = "e:\PlateIt"
$destinationZip = "e:\PlateIt\PlateIt_Submission.zip"
$tempDir = "e:\PlateIt_Temp_Export"

# Cleanup
if (Test-Path $destinationZip) { Remove-Item $destinationZip -Force }
if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }
New-Item -ItemType Directory -Path $tempDir | Out-Null

Write-Host "Creating clean export using Robocopy..."

# We will copy the entire root to temp, excluding garbage folders.
# /E = Copy Subdirectories, including empty ones.
# /XD = Exclude Directories matching these names
# /XF = Exclude Files matching these names

$robocopyArgs = @(
    $sourcePath,
    $tempDir,
    "/E",
    "/XD", ".git", ".gradle", ".idea", "build", "app\build", "__pycache__", "venv", "node_modules", "Checkpoints", ".pytest_cache",
    "/XF", "*.apk", "*.iml", "local.properties", ".DS_Store", "Thumbs.db", ".env", "*.zip", "*.log"
)

# Run Robocopy
# Robocopy returns exit codes that aren't errors (1=files copied), so we ignore the exit code logic of PS
& robocopy @robocopyArgs | Out-Null

Write-Host "Verifying BackEnd contents..."
if (-not (Test-Path "$tempDir\BackEnd\Agent\agent_server.py")) {
    Write-Warning "ALERT: agent_server.py is missing! Checking what happened..."
    Get-ChildItem "$tempDir\BackEnd" -Recurse
} else {
    Write-Host " - agent_server.py found."
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
