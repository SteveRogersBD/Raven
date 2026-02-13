import os
import requests
import tempfile
from langchain_core.tools import tool
from bs4 import BeautifulSoup
from urllib.parse import urlparse, parse_qs
import time
from duckduckgo_search import DDGS
import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()

@tool
def pexels_image_search(query: str):
    """
    Finds a high-quality image URL for a specific food item or cooking step using Pexels (Free).
    This is the primary tool for visuals like "chopping onions", "boiling water", etc.
    """
    api_key = os.getenv("PEXELS_API_KEY")
    if not api_key:
        return "Error: PEXELS_API_KEY not configured."
        
    url = "https://api.pexels.com/v1/search"
    headers = {
        "Authorization": api_key
    }
    params = {
        "query": query,
        "per_page": 1,
        "orientation": "landscape" # Best for recipe steps
    }
    
    try:
        response = requests.get(url, headers=headers, params=params)
        response.raise_for_status()
        data = response.json()
        
        if data.get("photos"):
            # Return the 'large' or 'medium' size for best balance
            return data["photos"][0]["src"]["medium"]
        else:
            return "No image found."
    except Exception as e:
        return f"Error searching Pexels: {e}"

@tool
def google_search(query: str):
    """
    Performs a general web search using DuckDuckGo.
    Useful for finding cooking tips, food history, or general questions not covered by Spoonacular.
    """
    try:
        results = []
        with DDGS() as ddgs:
            for r in ddgs.text(query, max_results=5):
                title = r.get('title', 'No Title')
                link = r.get('href', 'No Link')
                snippet = r.get('body', 'No Snippet')
                results.append(f"Title: {title}\nLink: {link}\nSnippet: {snippet}")
        
        if not results:
            return "No good search results found."
            
        return "\n\n".join(results)
    except Exception as e:
        return f"Error performing search: {e}"

@tool
def google_image_search(query: str):
    """
    Finds an image URL for a specific food item or dish using DuckDuckGo Images.
    """
    try:
        with DDGS() as ddgs:
            for r in ddgs.images(query, max_results=1):
                return r.get('image')
        return "No image found."
    except Exception as e:
        return f"Error searching images: {e}"

@tool
def serp_google_search(query: str):
    """
    Performs a general web search using Google (via SerpApi).
    LEGACY: Not currently used by the agent default.
    """
    api_key = os.getenv("SERP_API_KEY")
    if not api_key:
        return "Error: SERP_API_KEY not configured."
    
    url = "https://serpapi.com/search"
    params = {
        "engine": "google",
        "q": query,
        "api_key": api_key,
        "num": 5
    }
    
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        
        results = []
        if "organic_results" in data:
            for item in data["organic_results"]:
                title = item.get('title', 'No Title')
                link = item.get('link', 'No Link')
                snippet = item.get('snippet', 'No Snippet')
                results.append(f"Title: {title}\nLink: {link}\nSnippet: {snippet}")
        
        if not results:
            return "No good search results found."
            
        return "\n\n".join(results)
    except Exception as e:
        return f"Error performing search: {e}"

@tool
def serp_google_image_search(query: str):
    """
    Finds an image URL for a specific food item or dish using Google Images (via SerpApi).
    LEGACY: Not currently used by the agent default.
    """
    api_key = os.getenv("SERP_API_KEY")
    if not api_key:
        return "Error: SERP_API_KEY not configured."

    url = "https://serpapi.com/search"
    params = {
        "engine": "google_images",
        "q": query,
        "api_key": api_key,
        "num": 1
    }
    
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        
        if "images_results" in data and len(data["images_results"]) > 0:
            return data["images_results"][0].get("original")
        return "No image found."
    except Exception as e:
        return f"Error searching images: {e}"

# --- Spoonacular Tools ---

def _spoonacular_get(endpoint: str, params: dict):
    """Helper to call Spoonacular API"""
    api_key = os.getenv("SPOONACULAR_API_KEY")
    if not api_key:
        return {"error": "SPOONACULAR_API_KEY not configured."}
    
    base_url = "https://api.spoonacular.com"
    params["apiKey"] = api_key
    
    try:
        response = requests.get(f"{base_url}{endpoint}", params=params)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        return {"error": str(e)}

@tool
def search_recipes(query: str, cuisine: str = None, diet: str = None, number: int = 5):
    """
    Search for recipes by query, cuisine, and diet.
    Applies logic to find the best matches.
    """
    params = {
        "query": query,
        "number": number,
        "addRecipeInformation": True,
        "instructionsRequired": True
    }
    if cuisine:
        params["cuisine"] = cuisine
    if diet:
        params["diet"] = diet
        
    data = _spoonacular_get("/recipes/complexSearch", params)
    if "error" in data: return data["error"]
    
    results = []
    for r in data.get("results", []):
        source_url = r.get("sourceUrl") or r.get("spoonacularSourceUrl")
        results.append(f"ID: {r['id']} | Title: {r['title']} | Image: {r.get('image')} | Time: {r.get('readyInMinutes')}m | Source: {source_url}")
        
    return "\n".join(results) if results else "No recipes found."

@tool
def search_by_nutrients(min_protein: int = 0, max_calories: int = 1000, number: int = 5):
    """
    Find recipes with specific nutrient requirements.
    """
    params = {
        "minProtein": min_protein,
        "maxCalories": max_calories,
        "number": number,
        "random": True 
    }
    data = _spoonacular_get("/recipes/findByNutrients", params)
    if "error" in data: return data["error"]
    
    results = []
    for r in data:
        results.append(f"ID: {r['id']} | Title: {r['title']} | Image: {r.get('image')} | Cal: {r['calories']} | Protein: {r['protein']}")
    return "\n".join(results) if results else "No recipes found."

@tool
def find_by_ingredients(ingredients: str, number: int = 5):
    """
    Find recipes that use the given ingredients.
    ingredients: Comma-separated list (e.g. "apples, flour, sugar")
    """
    params = {
        "ingredients": ingredients,
        "number": number,
        "ranking": 2, # Minimize missing ingredients
        "ignorePantry": True
    }
    data = _spoonacular_get("/recipes/findByIngredients", params)
    if "error" in data: return data["error"]
    
    results = []
    for r in data:
        missing = [i["name"] for i in r.get("missedIngredients", [])]
        results.append(f"ID: {r['id']} | Title: {r['title']} | Image: {r.get('image')} | Missing: {', '.join(missing)}")
    return "\n".join(results) if results else "No recipes found."

@tool
def get_recipe_information(recipe_id: int):
    """
    Get full details for a specific recipe ID (instructions, ingredients).
    """
    data = _spoonacular_get(f"/recipes/{recipe_id}/information", {"includeNutrition": False})
    if "error" in data: return data["error"]
    
    title = data.get("title")
    servings = data.get("servings")
    ready_in = data.get("readyInMinutes")
    url = data.get("sourceUrl")
    
    ingredients = [f"- {i['original']}" for i in data.get("extendedIngredients", [])]
    
    instructions = data.get("instructions")
    if not instructions and data.get("analyzedInstructions"):
        steps = []
        for step in data["analyzedInstructions"][0]["steps"]:
            steps.append(f"{step['number']}. {step['step']}")
        instructions = "\n".join(steps)
        
    return f"""Title: {title}
Servings: {servings} | Time: {ready_in}m
Source: {url}
Ingredients:
{chr(10).join(ingredients)}
Instructions:
{instructions}"""

@tool
def find_similar_recipes(recipe_id: int, number: int = 3):
    """Find recipes similar to the given ID."""
    data = _spoonacular_get(f"/recipes/{recipe_id}/similar", {"number": number})
    if "error" in data: return data["error"]
    
    results = []
    for r in data:
        results.append(f"ID: {r['id']} | Title: {r['title']} | Image: {r.get('image')}")
    return "\n".join(results) if results else "No similar recipes found."

@tool
def get_random_recipes(tags: str = None, number: int = 3):
    """
    Get random recipes.
    tags: comma separated types (e.g. "vegetarian, dessert")
    """
    params = {"number": number}
    if tags: params["tags"] = tags
    
    data = _spoonacular_get("/recipes/random", params)
    if "error" in data: return data["error"]
    
    results = []
    for r in data.get("recipes", []):
        results.append(f"ID: {r['id']} | Title: {r['title']} | Image: {r.get('image')}")
    return "\n".join(results)

@tool
def extract_recipe_from_url(url: str):
    """Extract recipe data from a website URL."""
    data = _spoonacular_get("/recipes/extract", {"url": url})
    
    # Fallback for image if Spoonacular misses it but it's in Open Graph
    if isinstance(data, dict) and not data.get("image"):
        try:
            headers = {'User-Agent': 'Mozilla/5.0'}
            resp = requests.get(url, headers=headers, timeout=5)
            if resp.status_code == 200:
                soup = BeautifulSoup(resp.text, 'html.parser')
                # Try Open Graph
                og_image = soup.find("meta", property="og:image")
                if og_image:
                    data["image"] = og_image.get("content")
                else:
                    # Try Twitter
                    tw_image = soup.find("meta", name="twitter:image")
                    if tw_image:
                        data["image"] = tw_image.get("content")
        except:
            pass

    if "error" in data: return data["error"]
    return data

@tool
def search_ingredients(query: str, number: int = 5):
    """Search for an ingredient to get its ID."""
    data = _spoonacular_get("/food/ingredients/search", {"query": query, "number": number})
    if "error" in data: return data["error"]
    
    results = []
    for r in data.get("results", []):
        results.append(f"ID: {r['id']} | Name: {r['name']}")
    return "\n".join(results)

@tool
def get_ingredient_information(ingredient_id: int):
    """Get nutritional info for an ingredient ID."""
    data = _spoonacular_get(f"/food/ingredients/{ingredient_id}/information", {"amount": 100, "unit": "grams"})
    if "error" in data: return data["error"]
    
    name = data.get("name")
    nutrition = data.get("nutrition", {}).get("nutrients", [])
    
    key_nutrients = []
    for n in nutrition:
        if n["name"] in ["Calories", "Fat", "Protein", "Carbohydrates"]:
            key_nutrients.append(f"{n['name']}: {n['amount']}{n['unit']}")
            
    return f"Ingredient: {name}\nNutrition (per 100g):\n" + "\n".join(key_nutrients)

@tool
def create_recipe_card(recipe_id: int):
    """
    Get a URL to an image card for the recipe.
    Do NOT call this unless the user specifically asks for a visual card.
    """
    data = _spoonacular_get(f"/recipes/{recipe_id}/card", {})
    if "error" in data: return data["error"]
    
    return data.get("url", "No card URL returned.")

# --- Content Extraction Tools ---

@tool
def scrape_website_text(url: str):
    """
    Scrapes the text content from a given website URL.
    Useful for extracting recipes or articles from blogs/websites.
    """
    try:
        headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }
        response = requests.get(url, headers=headers)
        response.raise_for_status()
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # Remove script and style elements
        for script in soup(["script", "style", "nav", "footer"]):
            script.decompose()
            
        text = soup.get_text()
        
        # Clean up text
        lines = (line.strip() for line in text.splitlines())
        chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
        text = '\n'.join(chunk for chunk in chunks if chunk)
        
        return text
        
    except Exception as e:
        return f"Error scraping website: {e}"

@tool
def download_video_file(url: str, filename: str = "temp_video_recipe.mp4"):
    """
    Downloads a video file from a URL using yt-dlp (supports YouTube, Instagram, TikTok, etc.)
    or direct HTTP download.
    Returns the absolute path of the downloaded file.
    """
    import yt_dlp
    
    # Absolute path for the output
    abs_filename = os.path.abspath(filename)
    
    # 1. Try yt-dlp first (handles most social media + direct links often)
    ydl_opts = {
        'outtmpl': abs_filename,
        'format': 'best[ext=mp4]/best', 
        'quiet': True,
        'overwrites': True,
        'no_warnings': False,
        'js_runtimes': {'node': {}},
        'remote_components': ['ejs:github'], # FIXED: Must be a list
        'nocheckcertificate': True,
        'extractor_args': {
            'youtube': {
                'player_client': ['web', 'tv'],
            }
        },
    }
    
    # Check for cookies file or environment variable
    # We use a temp directory for Cloud Run compatibility as the rest of the FS is read-only
    cookies_path = os.path.join(tempfile.gettempdir(), "youtube_cookies.txt")
    
    # If YOUTUBE_COOKIES env var is provided, write it to a temp file
    if os.getenv("YOUTUBE_COOKIES"):
        try:
            with open(cookies_path, "w") as f:
                f.write(os.getenv("YOUTUBE_COOKIES"))
        except Exception as e:
            print(f" -> Error writing cookies.txt from env var: {e}")

    if os.path.exists(cookies_path):
        ydl_opts['cookiefile'] = cookies_path

    
    print(f" -> Attempting download with yt-dlp: {url}")
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])
            
        # Verify file exists and is not empty
        if os.path.exists(abs_filename) and os.path.getsize(abs_filename) > 0:
            print(f" -> Download successful: {abs_filename} ({os.path.getsize(abs_filename)} bytes)")
            return abs_filename
        else:
            print(f" -> yt-dlp created an empty file or failed.")
            if os.path.exists(abs_filename): os.remove(abs_filename)
    except Exception as e:
        print(f" -> yt-dlp failed: {e}. Falling back to requests.")
        if os.path.exists(abs_filename): os.remove(abs_filename)
        
    # 2. Fallback to direct request (for simple file servers)
    try:
        with requests.get(url, stream=True) as r:
            r.raise_for_status()
            with open(filename, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192): 
                    f.write(chunk)
        return abs_filename
    except Exception as e:
        return f"Error downloading video: {e}"

# --- YouTube Tools ---

@tool
def extract_video_id(url: str):
    """
    Extracts the YouTube Video ID from a given URL.
    Supports standard watch URLs, Shorts, and Share links.
    """
    if "watch" in url:
        parsed_url = urlparse(url)
        if 'v' in parse_qs(parsed_url.query):
            return parse_qs(parsed_url.query)['v'][0]
    elif "shorts" in url:
        return url.split("shorts/")[1].split("?")[0]
    elif "youtu.be" in url:
        return url.split("/")[-1].split("?")[0]
    
    return ""

@tool
def get_youtube_transcript(video_id: str):
    """
    Fetches the transcript of a YouTube video using the youtube-transcript-api.
    """
    from youtube_transcript_api import YouTubeTranscriptApi
    try:
        transcript_list = YouTubeTranscriptApi.get_transcript(video_id)
        return "\n".join([t["text"] for t in transcript_list])
    except Exception as e:
        return f"Error fetching transcript: {e}"

@tool
def get_youtube_description(video_id: str):
    """
    Fetches the description of a YouTube video using the official Google YouTube Data API.
    """
    from googleapiclient.discovery import build
    api_key = os.getenv("YT_API_KEY") or os.getenv("GOOGLE_API_KEY")
    if not api_key:
        return "Error: YT_API_KEY or GOOGLE_API_KEY not set."

    try:
        youtube = build('youtube', 'v3', developerKey=api_key)
        request = youtube.videos().list(part="snippet", id=video_id)
        response = request.execute()
        
        if response.get("items"):
            return response["items"][0]["snippet"].get("description", "No description found.")
        return "No video found with that ID."
    except Exception as e:
        return f"Error fetching description: {e}"

@tool
def serp_youtube_transcript(video_id: str):
    """
    Fetches the transcript of a YouTube video using SerpApi.
    LEGACY: Not currently used by the agent default.
    """
    api_key = os.getenv("SERP_API_KEY")
    if not api_key:
        return "Error: SERP_API_KEY not set."
        
    url = "https://serpapi.com/search"
    params = {
        "engine": "youtube_video_transcript",
        "v": video_id,
        "api_key": api_key,
    }
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        if "transcript" in data:
            transcripts = [t["snippet"] for t in data["transcript"]]
            return "\n".join(transcripts)
        return "No transcript found."
    except Exception as e:
        return f"Error fetching transcript: {e}"

@tool
def serp_youtube_description(video_id: str):
    """
    Fetches the description of a YouTube video using SerpApi.
    LEGACY: Not currently used by the agent default.
    """
    api_key = os.getenv("SERP_API_KEY")
    if not api_key:
        return "Error: SERP_API_KEY not set."

    url = "https://serpapi.com/search"
    params = {
        "engine": "youtube_video",
        "v": video_id,
        "api_key": api_key,
    }
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        return data.get("description", {}).get("content", "No description found.")
    except Exception as e:
        return f"Error fetching description: {e}"

@tool
def get_ingredient_image_url_fast(ingredient_name: str):
    """
    Generates the Spoonacular image URL for a given ingredient name.
    Uses the pattern: https://img.spoonacular.com/ingredients_{size}/{name}.jpg
    """
    # Clean up name: lowercase and replace spaces with hyphens
    formatted_name = ingredient_name.lower().strip().replace(" ", "-")
    
    # Base URL for ingredients (using 100x100 as default)
    return f"https://img.spoonacular.com/ingredients_100x100/{formatted_name}.jpg"



@tool
def get_ingredient_image_url(ingredient_name: str):
    """
    Fetches the image URL for a given ingredient name using Spoonacular.
    """
    api_key = os.getenv("SPOONACULAR_API_KEY")
    if not api_key:
        return None
    
    url = "https://api.spoonacular.com/food/ingredients/search"
    params = {
        "query": ingredient_name,
        "apiKey": api_key,
        "number": 1
    }
    
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        if data.get("results"):
            # Spoonacular base URL for ingredients
            image_server_base = "https://img.spoonacular.com/ingredients_100x100/"
            return f"{image_server_base}{data['results'][0]['image']}"
    except Exception:
        pass
    
    return None

def _parse_yt_duration(duration_str):
    """
    Parses YouTube's ISO 8601 duration (e.g., PT1M23S) into a readable format (1:23).
    """
    import re
    # Simple regex to extract H, M, S
    pattern = re.compile(r'PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?')
    match = pattern.match(duration_str)
    if not match: return "0:00"
    
    h, m, s = match.groups()
    h = int(h) if h else 0
    m = int(m) if m else 0
    s = int(s) if s else 0
    
    if h > 0:
        return f"{h}:{m:02d}:{s:02d}"
    return f"{m}:{s:02d}"

def _format_views(view_count):
    """
    Formats large numbers into strings like 1.2M or 45K.
    """
    try:
        num = int(view_count)
    except:
        return "N/A"
        
    if num >= 1_000_000:
        return f"{num/1_000_000:.1f}M".replace(".0", "")
    if num >= 1_000:
        return f"{num/1_000:.1f}K".replace(".0", "")
    return str(num)

def search_youtube_videos(query: str, limit: int = 5):
    """
    Searches YouTube using the Official YouTube Data API v3.
    Fetches snippets, statistics (views), and contentDetails (duration).
    Ensures a mix of shorts and longer videos.
    """
    from googleapiclient.discovery import build
    
    api_key = os.getenv("YT_API_KEY") or os.getenv("GOOGLE_API_KEY")
    if not api_key:
        print("Error: YT_API_KEY or GOOGLE_API_KEY not configured.")
        return []

    try:
        youtube = build('youtube', 'v3', developerKey=api_key)
        
        # 1. Search for a larger pool to allow for mixing (fetch e.g. 15)
        search_request = youtube.search().list(
            q=query,
            part='id',
            type='video',
            maxResults=limit * 3 
        )
        search_response = search_request.execute()
        
        video_ids = [item['id']['videoId'] for item in search_response.get('items', [])]
        if not video_ids:
            return []

        # 2. Fetch full details (statistics, duration, snippet) for these IDs
        details_request = youtube.videos().list(
            id=','.join(video_ids),
            part='snippet,statistics,contentDetails'
        )
        details_response = details_request.execute()
        
        all_fetched_videos = []
        for item in details_response.get('items', []):
            snippet = item['snippet']
            stats = item.get('statistics', {})
            details = item.get('contentDetails', {})
            duration_str = details.get('duration', 'PT0S')
            
            # Simple duration check: PT1M or PT5M etc.
            # We treat < 60s as a "Short" (heuristic)
            is_short = False
            if 'M' not in duration_str and 'H' not in duration_str:
                # Only seconds, probably a short
                is_short = True
            
            # Thumbnail Logic
            thumbs = snippet.get('thumbnails', {})
            best_thumb = (
                thumbs.get('maxres', {}).get('url') or 
                thumbs.get('standard', {}).get('url') or 
                thumbs.get('high', {}).get('url') or 
                thumbs.get('default', {}).get('url')
            )

            all_fetched_videos.append({
                "title": snippet['title'],
                "link": f"https://www.youtube.com/watch?v={item['id']}",
                "thumbnail": best_thumb,
                "channel": snippet['channelTitle'],
                "views": _format_views(stats.get('viewCount', 0)),
                "length": _parse_yt_duration(duration_str),
                "is_short": is_short
            })
            
        # 3. Balance Mix Logic
        shorts = [v for v in all_fetched_videos if v['is_short']]
        longs = [v for v in all_fetched_videos if not v['is_short']]
        
        # We want approx 50/50 or at least some of each
        mixed_videos = []
        
        # Take some from each
        target_longs = max(1, limit // 2)
        target_shorts = limit - target_longs
        
        mixed_videos.extend(longs[:target_longs])
        mixed_videos.extend(shorts[:target_shorts])
        
        # Fill up if one list was too short
        remaining_needed = limit - len(mixed_videos)
        if remaining_needed > 0:
            already_added_links = {v['link'] for v in mixed_videos}
            for v in all_fetched_videos:
                if v['link'] not in already_added_links:
                    mixed_videos.append(v)
                    already_added_links.add(v['link'])
                if len(mixed_videos) >= limit:
                    break
        
        # Shuffle final list for variety
        import random
        random.shuffle(mixed_videos)
        
        return mixed_videos[:limit]

    except Exception as e:
        print(f"Error searching YouTube API for '{query}': {e}")
        return []

@tool
def search_youtube(query: str, limit: int = 5):
    """
    Search specifically for YouTube videos to get video links and thumbnails.
    """
    videos = search_youtube_videos(query, limit)
    if not videos: return "No videos found."
    
    results = []
    for v in videos:
        results.append(f"Title: {v['title']}\nLink: {v['link']}\nThumbnail: {v['thumbnail']}\nChannel: {v['channel']}")
        
    return "\n\n".join(results)

def search_google_blogs(query: str, limit: int = 5):
    """
    Searches Google via SerpAPI for blog posts/articles.
    Returns a unified list of blog objects, prioritizing recipe cards.
    """
    api_key = os.getenv("SERP_API_KEY")
    if not api_key:
        print("Error: SERP_API_KEY not configured.")
        return []

    url = "https://serpapi.com/search"
    params = {
        "engine": "google",
        "q": query,
        "api_key": api_key,
        "num": limit
    }
    
    try:
        response = requests.get(url, params=params)
        response.raise_for_status()
        data = response.json()
        
        blogs = []
        
        # 1. Prioritize "recipes_results" (Rich Cards)
        if "recipes_results" in data:
            for item in data["recipes_results"]:
                # Construct a snippet from ingredients or time
                snippet = ""
                if "ingredients" in item:
                    snippet = f"Ingredients: {', '.join(item['ingredients'][:3])}..."
                elif "total_time" in item:
                     snippet = f"Time: {item['total_time']}"
                     
                blogs.append({
                    "title": item.get("title"),
                    "link": item.get("link"),
                    "thumbnail": item.get("thumbnail"), 
                    "source": item.get("source"),
                    "snippet": snippet
                })

        # 2. Append "organic_results" (Standard Links)
        if "organic_results" in data:
            for item in data["organic_results"]:
                
                # Check for "recipe" intent in title or snippet to filter out generic articles
                # (Unless we already have very few results)
                title = item.get("title", "").lower()
                snippet = item.get("snippet", "").lower()
                
                # Loose filter if lists are short
                if len(blogs) > 10:
                     if "recipe" not in title and "how to cook" not in title and "dish" not in title:
                        continue

                # Robust image extraction
                thumbnail = item.get("thumbnail") 
                
                # Check pagemap for cse_image / cse_thumbnail (Best source for blog images)
                if not thumbnail and "pagemap" in item:
                    pagemap = item["pagemap"]
                    
                    # Try cse_image first
                    cse_images = pagemap.get("cse_image")
                    if cse_images and isinstance(cse_images, list) and len(cse_images) > 0:
                        thumbnail = cse_images[0].get("src")
                    
                    # Try cse_thumbnail second
                    if not thumbnail:
                        cse_thumbs = pagemap.get("cse_thumbnail")
                        if cse_thumbs and isinstance(cse_thumbs, list) and len(cse_thumbs) > 0:
                            thumbnail = cse_thumbs[0].get("src")
                            
                    # Try metatags og:image
                    if not thumbnail:
                        metatags = pagemap.get("metatags")
                        if metatags and isinstance(metatags, list) and len(metatags) > 0:
                            thumbnail = metatags[0].get("og:image")

                blogs.append({
                    "title": item.get("title"),
                    "link": item.get("link"),
                    "snippet": item.get("snippet"),
                    "source": item.get("source"),
                    "thumbnail": thumbnail
                })
                
        # Deduplicate by link
        seen_links = set()
        unique_blogs = []
        for b in blogs:
            if b['link'] and b['link'] not in seen_links:
                unique_blogs.append(b)
                seen_links.add(b['link'])
                
        return unique_blogs[:limit]
        
    except Exception as e:
        print(f"Error searching Google for '{query}': {e}")
        return []

@tool
def get_video_metadata(url: str):
    """
    Extracts comprehensive metadata (title, description, transcript, thumbnail) 
    from a video URL using yt-dlp with Cloud Run bypasses.
    """
    import yt_dlp
    
    # Absolute path check for cookies (Cloud Run /tmp/ or local temp is writable)
    cookies_path = os.path.join(tempfile.gettempdir(), "youtube_cookies.txt")
    if os.getenv("YOUTUBE_COOKIES"):
        try:
            # We only write if it's missing or if we want to ensure it's fresh
            with open(cookies_path, "w", encoding='utf-8') as f:
                # Ensure we strip leading/trailing whitespace which happens during copy-paste
                cookie_data = os.getenv("YOUTUBE_COOKIES").strip()
                f.write(cookie_data)
        except Exception as e:
            print(f" -> Warning: Could not write cookies to {cookies_path}: {e}")

    ydl_opts = {
        'skip_download': True,
        'quiet': True,
        'ignoreerrors': True,
        'no_warnings': True,
        'extract_flat': False, 
        'nocheckcertificate': True,
        'socket_timeout': 30,
        'js_runtimes': {'node': {}}, 
        'remote_components': ['ejs:github'],
        'writesubtitles': True,
        'allsubtitles': False,
        'subtitleslangs': ['en', '.*'],
        'extractor_args': {
            'youtube': {
                'player_client': ['web', 'tv'],
            }
        },
    }
    
    if os.path.exists(cookies_path):
        ydl_opts['cookiefile'] = cookies_path
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            if not info:
                return None
                
            title = info.get('title', '')
            description = info.get('description', '')
            thumbnail = info.get('thumbnail', '')
            transcript = ""
            
            return {
                "title": title, 
                "description": description, 
                "thumbnail": thumbnail,
                "transcript": transcript,
                "video_id": info.get('id', '')
            }
    except Exception as e:
        error_msg = str(e)
        if "confirm you're not a bot" in error_msg or "Sign in" in error_msg:
            print("--- 🚨 YOUTUBE BOT BLOCK DETECTED ---")
            return {
                "error": "YouTube is blocking our Cloud Run IP. You MUST provide cookies to fix this.",
                "guide": "1. Export cookies from a logged-in browser using 'Get cookies.txt LOCALLY'. 2. Add the content to GitHub Secrets as 'YOUTUBE_COOKIES'. 3. Redeploy."
            }
        print(f"Error extracting metadata with yt-dlp: {e}")
        return None

