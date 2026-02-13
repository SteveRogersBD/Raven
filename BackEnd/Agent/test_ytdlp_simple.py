#!/usr/bin/env python3
"""
Simple standalone test for yt-dlp with YouTube cookies.
No dependencies on tools.py
"""

import os
import tempfile
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

def test_ytdlp():
    """Test yt-dlp with the problematic video"""
    print("=" * 60)
    print("Simple yt-dlp Test")
    print("=" * 60)
    
    try:
        import yt_dlp
        print(f"\n✓ yt-dlp version: {yt_dlp.version.__version__}")
    except Exception as e:
        print(f"\n✗ Failed to import yt-dlp: {e}")
        return False
    
    # Check cookies
    cookies_env = os.getenv("YOUTUBE_COOKIES")
    print(f"✓ YOUTUBE_COOKIES: {'Set' if cookies_env else 'Not set'}")
    
    if cookies_env:
        print(f"  Cookie length: {len(cookies_env)} chars")
    
    # Write cookies to temp file
    cookies_path = os.path.join(tempfile.gettempdir(), "youtube_cookies.txt")
    if cookies_env:
        try:
            with open(cookies_path, "w", encoding='utf-8') as f:
                f.write(cookies_env.strip())
            print(f"✓ Cookies written to: {cookies_path}")
        except Exception as e:
            print(f"✗ Failed to write cookies: {e}")
            cookies_path = None
    else:
        cookies_path = None
    
    # Test URL
    url = "https://www.youtube.com/watch?v=aRD6N6Unebg"
    print(f"\nTesting URL: {url}")
    print("\n" + "-" * 60)
    
    # Configure yt-dlp
    ydl_opts = {
        'skip_download': True,
        'quiet': False,
        'no_warnings': False,
        'nocheckcertificate': True,
        'socket_timeout': 60,
        'retries': 3,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
            'Accept-Language': 'en-US,en;q=0.9',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Sec-Ch-Ua': '"Google Chrome";v="131", "Chromium";v="131", "Not_A Brand";v="24"',
            'Sec-Ch-Ua-Mobile': '?0',
            'Sec-Ch-Ua-Platform': '"Windows"',
        },
        'extractor_args': {
            'youtube': {
                'player_client': ['web'],
                'player_skip': ['configs', 'webpage'],
            }
        },
    }
    
    if cookies_path and os.path.exists(cookies_path):
        ydl_opts['cookiefile'] = cookies_path
        print(f"Using cookies from: {cookies_path}\n")
    
    # Try extraction
    try:
        print("Attempting to extract video metadata...\n")
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
            if info:
                print("\n" + "=" * 60)
                print("✅ SUCCESS!")
                print("=" * 60)
                print(f"Title: {info.get('title', 'N/A')}")
                print(f"Video ID: {info.get('id', 'N/A')}")
                print(f"Duration: {info.get('duration', 'N/A')} seconds")
                print(f"Uploader: {info.get('uploader', 'N/A')}")
                print(f"View Count: {info.get('view_count', 'N/A'):,}")
                print(f"Thumbnail: {info.get('thumbnail', 'N/A')[:80]}...")
                
                desc = info.get('description', '')
                print(f"Description: {desc[:200]}..." if len(desc) > 200 else f"Description: {desc}")
                
                print("\n🎉 yt-dlp is working correctly with YouTube!")
                return True
            else:
                print("\n✗ No info returned")
                return False
                
    except Exception as e:
        print("\n" + "=" * 60)
        print("❌ FAILED")
        print("=" * 60)
        print(f"Error: {e}")
        print("\nThis is the same error you were seeing before.")
        print("\nNext steps:")
        print("1. Update yt-dlp: pip install --upgrade yt-dlp")
        print("2. Verify cookies are fresh (re-export from browser)")
        print("3. Try using YouTube API fallback instead")
        return False

if __name__ == "__main__":
    success = test_ytdlp()
    exit(0 if success else 1)
