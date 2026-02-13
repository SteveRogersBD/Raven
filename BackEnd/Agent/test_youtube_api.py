#!/usr/bin/env python3
"""
Test YouTube Data API v3 as a fallback for video metadata.
This is more reliable than yt-dlp for getting basic video info.
"""

import os
from dotenv import load_dotenv

load_dotenv()

def test_youtube_api():
    """Test YouTube Data API"""
    print("=" * 60)
    print("YouTube Data API Test")
    print("=" * 60)
    
    api_key = os.getenv("YT_API_KEY") or os.getenv("GOOGLE_API_KEY")
    if not api_key:
        print("\n✗ No API key found!")
        print("  Set YT_API_KEY or GOOGLE_API_KEY in .env")
        return False
    
    print(f"\n✓ API Key found: {api_key[:20]}...")
    
    try:
        from googleapiclient.discovery import build
        print("✓ google-api-python-client installed")
    except ImportError:
        print("✗ google-api-python-client not installed")
        print("  Run: pip install google-api-python-client")
        return False
    
    # Test video
    video_id = "aRD6N6Unebg"
    print(f"\nTesting with video ID: {video_id}")
    print(f"URL: https://www.youtube.com/watch?v={video_id}")
    print("\n" + "-" * 60)
    
    try:
        youtube = build('youtube', 'v3', developerKey=api_key)
        
        # Get video details
        request = youtube.videos().list(
            part="snippet,contentDetails,statistics",
            id=video_id
        )
        response = request.execute()
        
        if not response.get("items"):
            print("\n✗ No video found with that ID")
            print("  The video might be private, deleted, or region-locked")
            return False
        
        item = response["items"][0]
        snippet = item["snippet"]
        stats = item.get("statistics", {})
        details = item.get("contentDetails", {})
        
        print("\n" + "=" * 60)
        print("✅ SUCCESS!")
        print("=" * 60)
        print(f"Title: {snippet.get('title', 'N/A')}")
        print(f"Channel: {snippet.get('channelTitle', 'N/A')}")
        print(f"Published: {snippet.get('publishedAt', 'N/A')}")
        print(f"Duration: {details.get('duration', 'N/A')}")
        print(f"Views: {int(stats.get('viewCount', 0)):,}" if stats.get('viewCount') else "Views: N/A")
        print(f"Likes: {int(stats.get('likeCount', 0)):,}" if stats.get('likeCount') else "Likes: N/A")
        
        # Thumbnail
        thumbs = snippet.get('thumbnails', {})
        best_thumb = (
            thumbs.get('maxres', {}).get('url') or
            thumbs.get('high', {}).get('url') or
            thumbs.get('medium', {}).get('url')
        )
        print(f"Thumbnail: {best_thumb}")
        
        # Description
        desc = snippet.get('description', '')
        print(f"\nDescription ({len(desc)} chars):")
        print(desc[:300] + "..." if len(desc) > 300 else desc)
        
        print("\n🎉 YouTube Data API is working!")
        print("\nThis API can be used as a reliable fallback when yt-dlp fails.")
        return True
        
    except Exception as e:
        print(f"\n✗ API request failed: {e}")
        return False

def test_transcript_api():
    """Test youtube-transcript-api for getting transcripts"""
    print("\n" + "=" * 60)
    print("YouTube Transcript API Test")
    print("=" * 60)
    
    try:
        from youtube_transcript_api import YouTubeTranscriptApi
        print("\n✓ youtube-transcript-api installed")
    except ImportError:
        print("\n✗ youtube-transcript-api not installed")
        print("  Run: pip install youtube-transcript-api")
        return False
    
    video_id = "aRD6N6Unebg"
    print(f"\nTesting transcript for video ID: {video_id}")
    print("-" * 60)
    
    try:
        transcript_list = YouTubeTranscriptApi.get_transcript(video_id)
        transcript_text = " ".join([t['text'] for t in transcript_list])
        
        print("\n✅ SUCCESS!")
        print(f"Transcript length: {len(transcript_text)} chars")
        print(f"Number of segments: {len(transcript_list)}")
        print(f"\nFirst 300 chars:")
        print(transcript_text[:300] + "...")
        
        print("\n🎉 Transcript API is working!")
        return True
        
    except Exception as e:
        print(f"\n✗ Transcript extraction failed: {e}")
        print("  The video might not have captions/subtitles available")
        return False

if __name__ == "__main__":
    api_ok = test_youtube_api()
    transcript_ok = test_transcript_api()
    
    print("\n" + "=" * 60)
    print("Summary")
    print("=" * 60)
    print(f"YouTube Data API: {'✅ Working' if api_ok else '❌ Failed'}")
    print(f"Transcript API: {'✅ Working' if transcript_ok else '❌ Failed'}")
    
    if api_ok:
        print("\n💡 Recommendation:")
        print("   Use YouTube Data API + Transcript API instead of yt-dlp")
        print("   for recipe extraction. It's more reliable and doesn't")
        print("   require cookies or video downloads.")
    
    print("=" * 60)
