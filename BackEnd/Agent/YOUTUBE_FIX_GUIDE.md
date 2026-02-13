# YouTube Download Fix Guide

## Problem
YouTube is blocking yt-dlp with errors like:
- `Failed to extract any player response`
- `Failed to parse JSON`
- `Unable to extract yt initial data`

## Solutions (in order of priority)

### 1. Update yt-dlp (CRITICAL)

YouTube frequently changes their API. Always keep yt-dlp updated:

```bash
cd BackEnd/Agent
pip install --upgrade yt-dlp
```

Or update requirements.txt:
```
yt-dlp>=2025.02.01
```

### 2. Add Browser Cookies (RECOMMENDED)

This is the most reliable solution for persistent access:

#### Step 1: Export Cookies from Browser
1. Install a cookie export extension:
   - Chrome/Edge: "Get cookies.txt LOCALLY"
   - Firefox: "cookies.txt"
2. Go to YouTube.com while logged in
3. Export cookies to `youtube_cookies.txt` format

#### Step 2: Add to Your Project
Save the cookies file as `BackEnd/Agent/youtube_cookies.txt` or set as environment variable:

```env
YOUTUBE_COOKIES=# Netscape HTTP Cookie File
# This is a generated file! Do not edit.
.youtube.com	TRUE	/	TRUE	1234567890	CONSENT	YES+1
.youtube.com	TRUE	/	TRUE	1234567890	VISITOR_INFO1_LIVE	abcdef123456
# ... more cookies
```

### 3. Use YouTube API Fallback (ALREADY IMPLEMENTED)

Your code already falls back to YouTube Data API v3 when yt-dlp fails. Make sure you have:

```env
YT_API_KEY=your_youtube_api_key_here
```

Get an API key from: https://console.cloud.google.com/apis/credentials

### 4. Test Your Setup

Run the diagnostic script:

```bash
cd BackEnd/Agent
python fix_youtube_download.py
```

This will:
- Check your yt-dlp version
- Offer to update it
- Test downloading from YouTube
- Verify API fallback configuration

## What Changed in tools.py

Updated both `download_video_file` and `get_video_metadata` with:

1. **Better User-Agent**: Updated to Chrome 131 (latest)
2. **Improved Headers**: Added Sec-Ch-Ua headers for better bot detection bypass
3. **Optimized Extractor Args**: 
   - Removed 'ios' client (often blocked)
   - Changed `player_skip` from `['web_initial']` to `['webpage']`
   - Added `skip: ['hls', 'dash']` for faster extraction
4. **Retry Logic**: Added `retries: 3` and `fragment_retries: 3`
5. **Removed Deprecated Options**: Removed `js_runtimes` and `remote_components`

## Testing

Test with your problematic video:

```python
from tools import get_video_metadata, download_video_file

# Test metadata extraction
metadata = get_video_metadata("https://www.youtube.com/watch?v=aRD6N6Unebg")
print(metadata)

# Test download
filepath = download_video_file("https://www.youtube.com/watch?v=aRD6N6Unebg")
print(f"Downloaded to: {filepath}")
```

## If Still Failing

1. **Check if video is restricted**: Age-restricted or region-locked videos need cookies
2. **Try a different video**: Test with a simple, public video first
3. **Use API-only mode**: Modify code to skip yt-dlp and use YouTube API directly
4. **Check IP blocking**: Some cloud providers (GCP, AWS) have IPs blocked by YouTube

## Alternative: Use YouTube API Directly

For recipe extraction, you might not need the video file at all. Use:

```python
# Get transcript only (no download needed)
from tools import get_youtube_transcript, extract_video_id

video_id = extract_video_id(url)
transcript = get_youtube_transcript(video_id)
```

This uses `youtube-transcript-api` which is more reliable than yt-dlp for text extraction.

## Environment Variables Summary

```env
# Required for API fallback
YT_API_KEY=your_youtube_api_key

# Optional but recommended for reliability
YOUTUBE_COOKIES=<paste cookies.txt content here>

# Other APIs (already configured)
SPOONACULAR_API_KEY=your_key
PEXELS_API_KEY=your_key
GEMINI_API_KEY=your_key
```

## Quick Fix Commands

```bash
# Update yt-dlp
pip install --upgrade yt-dlp

# Test the fix
python fix_youtube_download.py

# Restart your server
# (if running in Docker, rebuild the container)
```
