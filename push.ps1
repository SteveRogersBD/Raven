# Push script for PlateIt project
# Usage: ./push.ps1 "Your commit message"

$commitMessage = $args[0]

if (-not $commitMessage) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $commitMessage = "Auto-update: $timestamp"
}

Write-Host "--- Starting Auto-Commit ---" -ForegroundColor Cyan

# Check for changes
$status = git status --porcelain
if (-not $status) {
    Write-Host "No changes to commit. Working tree clean." -ForegroundColor Yellow
    exit
}

# Add all changes
Write-Host "Adding changes..." -ForegroundColor Gray
git add .

# Commit
Write-Host "Committing with message: '$commitMessage'" -ForegroundColor Gray
git commit -m "$commitMessage"

# Push
Write-Host "Pushing to origin main..." -ForegroundColor Gray
git push origin main

Write-Host "--- Done! ---" -ForegroundColor Green
