from duckduckgo_search import DDGS
import json

def search_images():
    print("Welcome to the DuckDuckGo Image Search Tester!")
    print("Type 'exit' to quit.\n")
    
    ddgs = DDGS()

    while True:
        query = input("Enter search term (e.g. 'chopping onions'): ").strip()
        
        if query.lower() == 'exit':
            break
            
        if not query:
            continue

        print(f"\nSearching for '{query}'...")
        
        try:
            # max_results=1 ensures we get the top result quickly
            results = list(ddgs.images(query, max_results=1))
            
            if results:
                image_url = results[0]['image']
                print(f"✅ Found Image URL:\n{image_url}\n")
            else:
                print("❌ No images found.\n")
                
        except Exception as e:
            print(f"❌ Error: {e}\n")

if __name__ == "__main__":
    search_images()
