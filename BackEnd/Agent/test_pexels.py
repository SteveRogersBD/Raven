import os
import requests
from dotenv import load_dotenv

# Load key from .env
load_dotenv(r"d:\PlateIt\BackEnd\Agent\.env")

def test_pexels(query):
    api_key = os.getenv("PEXELS_API_KEY")
    if not api_key:
        print("❌ Error: PEXELS_API_KEY not found in .env")
        return

    print(f"🔎 Searching Pexels for: '{query}'...")
    
    url = "https://api.pexels.com/v1/search"
    headers = {
        "Authorization": api_key
    }
    params = {
        "query": query,
        "per_page": 1,
        "orientation": "landscape"
    }

    try:
        response = requests.get(url, headers=headers, params=params)
        response.raise_for_status()
        data = response.json()

        if data.get("photos"):
            photo = data["photos"][0]
            print(f"✅ Found Image!")
            print(f"   ID: {photo['id']}")
            print(f"   Photographer: {photo['photographer']}")
            print(f"   URL (Medium): {photo['src']['medium']}")
        else:
            print("⚠️ No photos found.")

    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    while True:
        q = input("\nEnter search term (or 'exit'): ").strip()
        if q.lower() == 'exit':
            break
        if q:
            test_pexels(q)
