#!/usr/bin/env python3
"""
Quick test script for the problematic YouTube video.
"""

import os
import sys
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import the tools
from tools import get_video_metadata, download_video_file

def test_metadata():
    """Test metadata extraction"""
    print("=" * 60)
    print("Testing Metadata Extraction")
    print("=" * 60)
    
    url = "https://www.youtube.com/watch?v=aRD6N6Unebg"
    print(f"\nURL: {url}")
    print("\n--- Checking Video Metadata: {url} ---")
    
    metadata = get_video_metadata(url)
    
    if metadata:
        if "error" in metadata:
            print(f"\n❌ ERROR: {metadata['error']}")
            if "guide" in metadata:
                print(f"   Guide: {metadata['guide']}")
            return False
        else:
            print(f"\n✅ SUCCESS!")
            print(f"   Title: {metadata.get('title', 'N/A')}")
            print(f"   Video ID: {metadata.get('video_id', 'N/A')}")
            print(f"   Thumbnail: {metadata.get('thumbnail', 'N/A')[:80]}...")
            print(f"   Description Length: {len(metadata.get('description', ''))} chars")
            print(f"   Transcript Length: {len(metadata.get('transcript', ''))} chars")
            if metadata.get('is_api_fallback'):
                print(f"   ⚠️  Used API Fallback (yt-dlp failed)")
            return True
    else:
        print("\n❌ FAILED: No metadata returned")
        return False

def test_download():
    """Test video download"""
    print("\n" + "=" * 60)
    print("Testing Video Download")
    print("=" * 60)
    
    url = "https://www.youtube.com/watch?v=aRD6N6Unebg"
    print(f"\nURL: {url}")
    print("\n--- Downloading Video: {url} ---")
    
    result = download_video_file(url, "test_recipe_video.mp4")
    
    if result and not result.startswith("Error"):
        print(f"\n✅ SUCCESS!")
        print(f"   File: {result}")
        if os.path.exists(result):
            size = os.path.getsize(result)
            print(f"   Size: {size:,} bytes ({size / 1024 / 1024:.2f} MB)")
            
            # Clean up test file
            try:
                os.remove(result)
                print(f"   ✓ Test file cleaned up")
            except:
                pass
            return True
        else:
            print(f"\n❌ File doesn't exist: {result}")
            return False
    else:
        print(f"\n❌ FAILED: {result}")
        return False

def main():
    print("\n🧪 YouTube Download Test Suite")
    print("Testing with: https://www.youtube.com/watch?v=aRD6N6Unebg\n")
    
    # Check environment
    print("Environment Check:")
    print(f"  YT_API_KEY: {'✓ Set' if os.getenv('YT_API_KEY') else '✗ Missing'}")
    print(f"  YOUTUBE_COOKIES: {'✓ Set' if os.getenv('YOUTUBE_COOKIES') else '✗ Missing'}")
    print()
    
    # Run tests
    metadata_ok = test_metadata()
    
    if metadata_ok:
        print("\n" + "─" * 60)
        download_ok = test_download()
    else:
        print("\n⚠️  Skipping download test due to metadata failure")
        download_ok = False
    
    # Summary
    print("\n" + "=" * 60)
    print("Test Summary")
    print("=" * 60)
    print(f"  Metadata Extraction: {'✅ PASS' if metadata_ok else '❌ FAIL'}")
    print(f"  Video Download: {'✅ PASS' if download_ok else '❌ FAIL'}")
    print("=" * 60)
    
    if metadata_ok and download_ok:
        print("\n🎉 All tests passed! YouTube integration is working.")
        return 0
    elif metadata_ok:
        print("\n⚠️  Metadata works but download failed.")
        print("   This might be okay if you only need transcripts/metadata.")
        return 1
    else:
        print("\n❌ Tests failed. Check the errors above.")
        print("\nTroubleshooting:")
        print("  1. Update yt-dlp: pip install --upgrade yt-dlp")
        print("  2. Verify cookies are set in .env")
        print("  3. Check YT_API_KEY is valid")
        print("  4. See YOUTUBE_FIX_GUIDE.md for more help")
        return 1

if __name__ == "__main__":
    sys.exit(main())
