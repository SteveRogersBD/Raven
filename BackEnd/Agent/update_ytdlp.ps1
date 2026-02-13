# PowerShell script to update yt-dlp and test YouTube downloads

Write-Host "=" -NoNewline -ForegroundColor Cyan
Write-Host ("=" * 59) -ForegroundColor Cyan
Write-Host "YouTube Download Fix Script" -ForegroundColor Yellow
Write-Host "=" -NoNewline -ForegroundColor Cyan
Write-Host ("=" * 59) -ForegroundColor Cyan
Write-Host ""

# Check if virtual environment exists
if (Test-Path ".venv\Scripts\Activate.ps1") {
    Write-Host "[1/4] Activating virtual environment..." -ForegroundColor Green
    & .venv\Scripts\Activate.ps1
} else {
    Write-Host "[!] No virtual environment found at .venv" -ForegroundColor Yellow
    Write-Host "    Continuing with global Python..." -ForegroundColor Yellow
}

# Update yt-dlp
Write-Host ""
Write-Host "[2/4] Updating yt-dlp to latest version..." -ForegroundColor Green
python -m pip install --upgrade yt-dlp

# Check version
Write-Host ""
Write-Host "[3/4] Checking yt-dlp version..." -ForegroundColor Green
yt-dlp --version

# Run diagnostic
Write-Host ""
Write-Host "[4/4] Running diagnostic script..." -ForegroundColor Green
Write-Host ""
python fix_youtube_download.py

Write-Host ""
Write-Host "=" -NoNewline -ForegroundColor Cyan
Write-Host ("=" * 59) -ForegroundColor Cyan
Write-Host "Done! Check the output above for any issues." -ForegroundColor Yellow
Write-Host "=" -NoNewline -ForegroundColor Cyan
Write-Host ("=" * 59) -ForegroundColor Cyan
