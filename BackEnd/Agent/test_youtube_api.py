
import os
from googleapiclient.discovery import build
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Get the API key
api_key = os.getenv("YT_API_KEY")

if not api_key:
    # If not in env, prompt the user (for testing) or fail
    print("Error: YOUTUBE_API_KEY or GOOGLE_API_KEY is missing in your .env file.")
    print("Please add it to .env or paste it below for this test run.")
    api_key = input("Enter API Key (or press Enter to exit): ").strip()

if not api_key:
    exit()

def search_videos(query: str, max_results: int = 5):
    """Searches YouTube for videos matching the query."""
    youtube = build('youtube', 'v3', developerKey=api_key)

    print(f"\n--- Searching YouTube for: '{query}' ---")
    
    request = youtube.search().list(
        part="snippet",
        q=query
    )
    
    try:
        response = request.execute()
        
        # Parse and display results
        for item in response.get("items", []):
            video_id = item["id"]["videoId"]
            title = item["snippet"]["title"]
            description = item["snippet"]["description"]
            channel = item["snippet"]["channelTitle"]
            thumbnail = item["snippet"]["thumbnails"]["high"]["url"]
            
            print(f"\n📺 Title: {title}")
            print(f"   ID: {video_id}")
            print(f"   Channel: {channel}")
            print(f"   Thumbnail: {thumbnail}")
            print(f"   Description Snippet: {description[:100]}...") # Truncated
            print(f"   Link: https://www.youtube.com/watch?v={video_id}")
            
    except Exception as e:
        print(f"Error during search: {e}")

if __name__ == "__main__":
    while True:
        q = input("\nEnter search query (or 'q' to quit): ").strip()
        if q == 'q': break
        search_videos(q)
