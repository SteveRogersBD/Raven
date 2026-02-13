#!/usr/bin/env python3
"""
Script to test and fix YouTube download issues.
Run this to diagnose and update yt-dlp.
"""

import subprocess
import sys
import os
from dotenv import load_dotenv

load_dotenv()

def check_ytdlp_version():
    """Check current yt-dlp version"""
    try:
        result = subprocess.run(['yt-dlp', '--version'], capture_output=True, text=True)
        version = result.stdout.strip()
        print(f"✓ Current yt-dlp version: {version}")
        return version
    except Exception as e:
        print(f"✗ Error checking yt-dlp version: {e}")
        return None

def update_ytdlp():
    """Update yt-dlp to latest version"""
    print("\n📦 Updating yt-dlp...")
    try:
        subprocess.run([sys.executable, '-m', 'pip', 'install', '--upgrade', 'yt-dlp'], check=True)
        print("✓ yt-dlp updated successfully")
        return True
    except Exception as e:
        print(f"✗ Error updating yt-dlp: {e}")
        return False

def test_download(url="https://www.youtube.com/watch?v=aRD6N6Unebg"):
    """Test downloading a YouTube video"""
    print(f"\n🧪 Testing download: {url}")
    
    import yt_dlp
    
    ydl_opts = {
        'skip_download': True,
        'quiet': False,
        'no_warnings': False,
        'nocheckcertificate': True,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        },
        'extractor_args': {
            'youtube': {
                'player_client': ['android', 'web'],
            }
        },
    }
    
    # Check for cookies
    cookies_path = os.path.join(os.path.dirname(__file__), "youtube_cookies.txt")
    if os.path.exists(cookies_path):
        ydl_opts['cookiefile'] = cookies_path
        print(f"✓ Using cookies from: {cookies_path}")
    else:
        print(f"⚠ No cookies file found at: {cookies_path}")
        print("  Consider adding cookies for better reliability")
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if info:
                print(f"✓ Successfully extracted metadata:")
                print(f"  Title: {info.get('title', 'N/A')}")
                print(f"  Duration: {info.get('duration', 'N/A')}s")
                print(f"  Uploader: {info.get('uploader', 'N/A')}")
                return True
    except Exception as e:
        print(f"✗ Download test failed: {e}")
        return False

def check_api_fallback():
    """Check if YouTube API is configured"""
    print("\n🔑 Checking YouTube API configuration...")
    api_key = os.getenv("YT_API_KEY") or os.getenv("GOOGLE_API_KEY")
    if api_key:
        print(f"✓ YouTube API key found (starts with: {api_key[:10]}...)")
        return True
    else:
        print("✗ No YouTube API key found")
        print("  Set YT_API_KEY or GOOGLE_API_KEY in .env for fallback support")
        return False

def main():
    print("=" * 60)
    print("YouTube Download Diagnostic Tool")
    print("=" * 60)
    
    # Check version
    version = check_ytdlp_version()
    
    # Update
    if input("\nUpdate yt-dlp to latest version? (y/n): ").lower() == 'y':
        update_ytdlp()
        check_ytdlp_version()
    
    # Check API
    check_api_fallback()
    
    # Test download
    if input("\nTest video download? (y/n): ").lower() == 'y':
        test_url = input("Enter YouTube URL (or press Enter for default): ").strip()
        if not test_url:
            test_url = "https://www.youtube.com/watch?v=aRD6N6Unebg"
        test_download(test_url)
    
    print("\n" + "=" * 60)
    print("Recommendations:")
    print("=" * 60)
    print("1. Keep yt-dlp updated: pip install --upgrade yt-dlp")
    print("2. Add cookies for reliability (export from browser)")
    print("3. Set YT_API_KEY in .env for API fallback")
    print("4. Consider using YouTube API directly for metadata")
    print("=" * 60)

if __name__ == "__main__":
    main()
