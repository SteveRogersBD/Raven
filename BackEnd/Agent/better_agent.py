import json,os,time,requests,typing_extensions
from typing import Annotated, Literal
TypedDict = typing_extensions.TypedDict
from langgraph.graph import StateGraph, START, END
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage
from pydantic import BaseModel, Field
from dotenv import load_dotenv
import google.generativeai as genai
from langchain_core.tools import tool

# --- Import Reusable Tools ---
from tools import (
    download_video_file,
    extract_video_id,
    get_youtube_transcript,
    get_youtube_description,
    get_ingredient_image_url,
    get_ingredient_image_url_fast,
    find_by_ingredients,
    extract_recipe_from_url,
    google_image_search,
    pexels_image_search,
    get_video_metadata
)





from dotenv import load_dotenv

load_dotenv()

# --- MODEL CONFIGURATION ---
# Orchestrator: Google Gemini 3 (Native multimodal & high reasoning)
# Worker: Fast, efficient (GPT-4o or fallback)
# Vision/Video: Gemini 3 (Native multimodal)

ORCHESTRATOR_MODEL = "gemini-3-flash-preview" 
WORKER_MODEL = "gpt-4o" 
REFINER_MODEL = "gpt-4o-mini"

# Initialize LLMs
try:
    # Main Brain (Orchestrator) - Using Gemini 3 as the heart of PlateIt
    orchestrator_llm = ChatGoogleGenerativeAI(model=ORCHESTRATOR_MODEL, google_api_key=os.getenv("GEMINI_API_KEY"))
except Exception as e:
    print(f"Warning: Could not init {ORCHESTRATOR_MODEL}. Error: {e}")
    # Fallback to GPT-4o if Gemini fails
    orchestrator_llm = ChatOpenAI(model="gpt-4o", api_key=os.getenv("OPEN_API_KEY"))

try:
    # Worker Brain (Fast)
    worker_llm = ChatOpenAI(model=WORKER_MODEL, api_key=os.getenv("OPEN_API_KEY"))
except:
    worker_llm = ChatOpenAI(model="gpt-4o", api_key=os.getenv("OPEN_API_KEY"))

try:
    # Refiner (Prose Polish)
    refiner_llm = ChatOpenAI(model=REFINER_MODEL, api_key=os.getenv("OPEN_API_KEY"))
except:
    refiner_llm = worker_llm





# --- Data Models ---

class Ingredient(BaseModel):
    name: str = Field(description="Name of the ingredient, e.g. 'onions'")
    amount: str = Field(description="Quantity and unit, e.g. '1 cup'")
    imageUrl: str | None = Field(default=None, description="URL of the ingredient image")

class RecipeStep(BaseModel):
    instruction: str = Field(description="The cooking instruction text")
    visual_query: str | None = Field(default=None, description="REQUIRED: A short 3-5 word keyword search query to find an image for this step. Example: 'chopped onions on board'")
    imageUrl: str | None = Field(default=None, description="URL of the step image")

class Recipe(BaseModel):
    name: str = Field(description="Name of the recipe")
    steps: list[RecipeStep] = Field(description="List of cooking steps")
    ingredients: list[Ingredient] = Field(description="List of ingredients")
    total_time: str | None = Field(default=None, description="Total preparation and cooking time, e.g. '45 mins'")
    source: str | None = Field(default=None, description="URL of the original recipe source (e.g. YouTube video, Blog URL)")
    source_image: str | None = Field(default=None, description="URL of the source image/thumbnail")


# Structured Outputs
# We use the Orchestrator for complex recipe generation
recipe_gen_llm = orchestrator_llm

# We use the Worker for final formatting/extraction (it's good at JSON)
recipe_format_llm = worker_llm.with_structured_output(Recipe) if 'Recipe' in globals() else worker_llm

# Re-init structured output now that Recipe is defined
recipe_format_llm = worker_llm.with_structured_output(Recipe)


# --- State Definition ---

class AgentState(TypedDict):
    url: str
    video_id: str
    description: str
    transcript: str
    text_content: str
    video_file_path: str
    image_file_path: str 
    video_thumbnail: str 
    
    # Internal state for passing data between nodes
    ingredients_detected: list[str] 
    dish_description: str
    raw_recipe_text: str 
    
    recipe: Recipe
    enriched_ingredients: list[Ingredient]
    enriched_steps: list[RecipeStep]
    metadata_sufficient: str # "yes" or "no"

# Ensure API Key is available to LangChain
if not os.environ.get("GOOGLE_API_KEY") and os.environ.get("GEMINI_API_KEY"):
    os.environ["GOOGLE_API_KEY"] = os.environ["GEMINI_API_KEY"]


# --- Router Logic ---

def determine_source_type(state: AgentState):
    url = state["url"]
    if not url: return "website"
    lower_url = url.lower()
    
    # 1. YouTube
    if any(x in url for x in ["youtube.com", "youtu.be"]):
        return "youtube"
    
    # 2. Video File & Social Media
    video_exts = ['.mp4', '.mov', '.avi', '.webm']
    social_domains = ['instagram.com', 'tiktok.com', 'facebook.com', 'x.com', 'twitter.com']
    
    if any(lower_url.endswith(ext) for ext in video_exts) or any(x in lower_url for x in social_domains):
        return "video_file"

    # 3. Image File
    image_exts = ['.jpg', '.jpeg', '.png', '.webp', '.heic']
    if any(lower_url.endswith(ext) for ext in image_exts):
        return "image_file"
        
    return "website"

# --- Input Processing Nodes ---

@tool
def node_process_image_file(state: AgentState):
    """Downloads an image file or passes local path."""
    url = state["url"]
    print(f"--- Downloading Image: {url} ---")
    if os.path.exists(url): return {"image_file_path": os.path.abspath(url)}

    try:
        filename = "temp_agent_image.jpg"
        with requests.get(url, stream=True) as r:
            r.raise_for_status()
            with open(filename, 'wb') as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)
        return {"image_file_path": os.path.abspath(filename)}
    except Exception as e:
        print(f"Error downloading image: {e}")
        return {"image_file_path": None}

def node_process_video_file(state: AgentState):
    """Downloads a video file."""
    url = state["url"]
    print(f"--- Downloading Video: {url} ---")
    path = download_video_file.invoke({"url": url, "filename": "temp_agent_video.mp4"})
    if "Error" in path: return {"video_file_path": None}
    return {"video_file_path": path}

def node_scrape_website(state: AgentState):
    """Scrapes text from website."""
    url = state["url"]
    print(f"--- Scraping Website: {url} ---")
    
    response = extract_recipe_from_url.invoke(url)
    
    if isinstance(response, str) and "Error" in response:
         print(f"   -> Spoonacular extraction failed: {response}. Trying raw scrape fallback.")
         # Fallback: Scrape raw text and pass it to the text extractor
         raw_text = scrape_website_text.invoke(url)
         return {"text_content": raw_text or response}

    name = response.get('title', 'Unknown Recipe')
    base_img_url = "https://img.spoonacular.com/ingredients_100x100/"
    source_image = response.get('image', None)
    
    ingredients = []
    for item in response.get('extendedIngredients', []):
        img_file = item.get('image', '')
        full_img_url = f"{base_img_url}{img_file}" if img_file else None
        
        ingredients.append(Ingredient(
            name=item.get('name', 'unknown'), 
            amount=f"{item.get('amount', 0)} {item.get('unit', '')}", 
            imageUrl=full_img_url
        ))

    steps = []
    analyzed_instructions = response.get('analyzedInstructions', [])
    
    # Loop through all instruction sets (some recipes have "For the Sauce", "For the Meat", etc.)
    for instruction_set in analyzed_instructions:
        for step in instruction_set.get('steps', []):
            instr_text = step.get('step', '')
            
            # Heuristic for step image: Try to find the 'main' ingredient or equipment in this step
            step_img = None
            
            # Check ingredients in step
            if step.get('ingredients'):
                ing_img = step['ingredients'][0].get('image')
                if ing_img:
                    step_img = f"https://img.spoonacular.com/ingredients_100x100/{ing_img}"
            
            # Check equipment in step if no ingredient image
            if not step_img and step.get('equipment'):
                eq_img = step['equipment'][0].get('image')
                if eq_img:
                    step_img = f"https://img.spoonacular.com/equipment_100x100/{eq_img}"
            
            steps.append(RecipeStep(
                instruction=instr_text,
                imageUrl=step_img
            ))
            
    # Fallback: if we have NO steps images, use the source_image as a placeholder for the first/last steps if it makes sense?
    # Probably better to leave it to node_enrich_steps which we have in the graph.
            
    total_time = f"{response.get('readyInMinutes', 0)} mins"
    recipe = Recipe(name=name, steps=steps, ingredients=ingredients, total_time=total_time, source=url, source_image=source_image)
    return {"recipe": recipe} 

def node_check_video_metadata(state: AgentState):
    """Checks if video description has enough info to skip download."""
    url = state["url"]
    print(f"--- 🔍 Checking Video Metadata: {url} ---")
    
    meta = get_video_metadata.invoke(url)
    if not meta:
        return {"metadata_sufficient": "no"}
        
    description = meta.get("description", "")
    title = meta.get("title", "")
    full_text = f"Title: {title}\nDescription: {description}"
    
    # Extract metadata we need for the UI/Database
    results = {"description": description}
    
    # 1. Use metadata thumbnail if available (works for TikTok, Instagram, X)
    if meta.get("thumbnail"):
        results["video_thumbnail"] = meta["thumbnail"]
    
    # 2. Fallback/Priority for YouTube to ensure high quality
    if any(x in url.lower() for x in ["youtube.com", "youtu.be"]):
        vid_id = extract_video_id.invoke(url)
        if vid_id:
            results["video_id"] = vid_id
            # Metadata thumbnail might be low res on YT, so we prefer the mqdefault
            if not results.get("video_thumbnail"):
                results["video_thumbnail"] = f"https://img.youtube.com/vi/{vid_id}/mqdefault.jpg"
    
    # If we have a transcript from the fallback, use it!
    transcript = meta.get("transcript", "")
    full_text = f"Title: {title}\nDescription: {description}"
    if transcript:
        full_text += f"\n\nTranscript:\n{transcript}"
    
    # Heuristic: If we spent the effort to get a transcript, it's likely sufficient for text extraction
    # unless it's extremely short.
    if len(description) < 100 and not transcript:
        results["metadata_sufficient"] = "no"
        return results

    prompt = f"""
    Analyze this video information (Description and potentially Transcript). 
    Does it contain enough information to reconstruct a FULL recipe with:
    1. Ingredients with quantities?
    2. Cooking instructions?
    
    Content:
    {full_text}
    
    Return JSON: {{"is_complete": true/false}}
    """
    
    try:
        response = worker_llm.invoke([HumanMessage(content=prompt)])
        content = response.content.replace("```json", "").replace("```", "").strip()
        result = json.loads(content)
        
        if result.get("is_complete"):
            print("   ✅ Full recipe found in text/transcript! Skipping video download.")
            results.update({
                "metadata_sufficient": "yes", 
                "description": description, 
                "transcript": transcript,
                "text_content": full_text
            })
        else:
            print("   ❌ Text info incomplete. Proceeding to video download.")
            results["metadata_sufficient"] = "no"
            results["transcript"] = transcript # Keep it anyway
            
    except Exception as e:
        print(f"Error checking metadata: {e}")
        results["metadata_sufficient"] = "no"
        results["transcript"] = transcript
        
    return results
 

    
def node_get_video_metadata(state: AgentState):
    """Fetches video data using yt-dlp (consolidated tool)."""
    url = state["url"]
    print(f"--- Fetching Video Metadata: {url} ---")
    
    # Use the consolidated tool which has our cookie/bot bypasses
    meta = get_video_metadata.invoke(url)
    
    if not meta or not meta.get("title"):
        # Fallback to ID extraction if metadata fails
        video_id = extract_video_id.invoke(url)
        return {"video_id": video_id}
        
    return {
        "video_id": meta.get("video_id"), 
        "transcript": meta.get("transcript", ""), 
        "description": meta.get("description", ""), 
        "video_thumbnail": meta.get("thumbnail")
    }


# --- Extraction Logic Nodes ---

def node_extract_text_from_video(state: AgentState):
    """Extracts raw recipe text from video file using Gemini (Native Video)."""
    video_path = state.get("video_file_path")
    if not video_path or not os.path.exists(video_path): return {}
    
    print("--- 🎥 Extracting Text from Video (Gemini 3) ---")
    
    api_key = os.getenv("GOOGLE_API_KEY")
    genai.configure(api_key=api_key)
    
    try:
        video_file = genai.upload_file(path=video_path)
        while video_file.state.name == "PROCESSING":
            print(f"   Waiting for video processing...")
            time.sleep(2)
            video_file = genai.get_file(video_file.name)

        if video_file.state.name == "FAILED": return {}

        # Use Gemini for Video Understanding
        model = genai.GenerativeModel('gemini-3-flash-preview') 
        prompt = "You are an expert chef. Watch this video and write down the full recipe with a clear name, ingredients with amounts, instructions, and an estimated total_time (e.g. '30 mins')."
        result = model.generate_content([video_file, prompt])
        
        genai.delete_file(video_file.name)
        if os.path.exists(video_path): os.remove(video_path)
        
        return {"raw_recipe_text": result.text}
    except Exception as e:
        print(f"Video extraction error: {e}")
        return {}

def node_analyze_image_type(state: AgentState):
    """Decides if image is 'ingredients' or 'dish' using GPT-5.2 (Vision)."""
    image_path = state.get("image_file_path")
    if not image_path: return {}
    
    print(f"--- 🖼️ Analyzing Image Type ({ORCHESTRATOR_MODEL}) ---")
    
    # TODO: Implement OpenAI Vision API here if switching completely
    # For now, sticking to Gemini for Image Upload to avoid base64 complexity in this turn
    # Switching strictly text nodes to OpenAI first as requested "vision... gpt 5.2"
    
    # Actually, let's use the Orchestrator (GPT-5.2) if it supports image input in LangChain
    # LangChain ChatOpenAI supports images via HumanMessage content list
    
    import base64
    def encode_image(image_path):
        with open(image_path, "rb") as image_file:
            return base64.b64encode(image_file.read()).decode('utf-8')
            
    base64_image = encode_image(image_path)
    
    prompt = """Analyze this image. 
    1. Is this a picture of raw ingredients?
    2. Or is it a finished, cooked dish?
    Return JSON: {"type": "ingredients" or "dish", "content": "list or description"}"""
    
    msg = HumanMessage(content=[
        {"type": "text", "text": prompt},
        {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{base64_image}"}}
    ])
    
    try:
        response = orchestrator_llm.invoke([msg])
        text_clean = response.content.replace("```json", "").replace("```", "")
        analysis = json.loads(text_clean)
        
        if analysis["type"] == "ingredients":
            raw_content = analysis["content"]
            items = [item.strip() for item in raw_content.split(',')]
            return {"ingredients_detected": items, "dish_description": ""}
        else:
            return {"ingredients_detected": [], "dish_description": analysis["content"]}
            
    except Exception as e:
        print(f"Image analysis error: {e}")
        return {"dish_description": "Unknown dish"}


def node_recipe_from_ingredients(state: AgentState):
    """Logic for Ingredients -> Recipe (Orchestrator)."""
    ingredients = state.get("ingredients_detected", [])
    if not ingredients: return {}
    
    print(f"--- 🥕 Processing Ingredients ({ORCHESTRATOR_MODEL}) ---")
    ing_str = ", ".join(ingredients)
    search_results = find_by_ingredients.invoke(ing_str)
    
    if search_results and "No recipes found" not in search_results:
        context = f"Ingredients available: {ing_str}.\n\nPotential Recipes Found:\n{search_results}"
        prompt = "Using the available ingredients and valid matches, create a full detailed recipe for the best match. Include a name, ingredients, steps, and a 'total_time' estimate. IMPORTANT: For every step, you MUST generate a 'visual_query' string (3-5 words) that describes the action for an image search (e.g. 'whisking eggs', 'chopping carrots')."
    else:
        context = f"Ingredients available: {ing_str}."
        prompt = "Create a creative and delicious recipe using ONLY these ingredients (and basic pantry items). Include a name, ingredients, steps, and a 'total_time' estimate. IMPORTANT: For every step, you MUST generate a 'visual_query' string (3-5 words) that describes the action for an image search (e.g. 'sauteing onions', 'boiling pasta')."
        
    result = orchestrator_llm.invoke([
        SystemMessage(content="You are an expert chef."),
        HumanMessage(content=f"{context}\n\n{prompt}")
    ])
    
    return {"raw_recipe_text": result.content}


def node_recipe_from_dish_image(state: AgentState):
    """Logic for Dish Image Description -> Recipe (Orchestrator)."""
    description = state.get("dish_description")
    if not description: return {}
    
    print(f"--- 🍲 Processing Dish Description ({ORCHESTRATOR_MODEL}) ---")
    
    result = orchestrator_llm.invoke([
        SystemMessage(content="You are an expert chef."),
        HumanMessage(content=f"The user provided an image of: {description}. Provide a complete, authentic recipe for this dish including name, total_time estimate, ingredients and steps. IMPORTANT: For every step, you MUST generate a 'visual_query' string (3-5 words) that describes the action for an image search (e.g. 'simmering soup', 'grating cheese').")
    ])
    
    return {"raw_recipe_text": result.content}


def node_extract_from_text(state: AgentState):
    """Standard extraction for text/transcript (Orchestrator)."""
    transcript = state.get("transcript")
    description = state.get("description")
    text_content = state.get("text_content")

    if text_content:
        content = f"Content: {text_content}"
    elif transcript or description:
        content = f"Transcript: {transcript or 'None'}\nDescription: {description or 'None'}"
    
    if not content: return {}
    
    print(f"--- 📄 Processing Text Content ({ORCHESTRATOR_MODEL}) ---")
    
    result = orchestrator_llm.invoke([
        SystemMessage(content="You are an expert chef."),
        HumanMessage(content=f"Based on: {content}. Create a detailed recipe with a clear name, total_time estimate, ingredients and steps. IMPORTANT: For every step, you MUST generate a 'visual_query' string (3-5 words) that describes the action for an image search (e.g. 'baking bread', 'chopping herbs').")
    ])
    return {"raw_recipe_text": result.content}


def node_format_recipe(state: AgentState):
    """Final formatting to strict JSON (Worker)."""
    raw_text = state.get("raw_recipe_text")
    if not raw_text: return {"recipe": None}
    
    print(f"--- ✨ Formatting Recipe ({WORKER_MODEL}) ---")
    
    try:
        response = recipe_format_llm.invoke([
            SystemMessage(content="Extract the recipe data into the specific JSON format required."),
            HumanMessage(content=raw_text)
        ])
        
        # Inject metadata
        source_url = state.get("url")
        if source_url and not response.source: response.source = source_url
        source_image = state.get("video_thumbnail")
        if source_image and not response.source_image: response.source_image = source_image
             
        return {"recipe": response}
    except Exception as e:
        print(f"Formatting error: {e}")
        return {}
        
def enrich_ingredients(state: AgentState):
    """Enriches ingredients with images."""
    recipe = state.get('recipe')
    if not recipe: return {}
    
    print("--- 🎨 Enriching Ingredients ---")
    updated = []
    
    for ing in recipe.ingredients:
        # Check if we already have a valid http image
        current_img = ing.imageUrl
        if not current_img or "http" not in current_img:
            # 1. Try Fast Heuristic first (Free, Instant)
            url = get_ingredient_image_url_fast.invoke(ing.name)
            
            # 2. VALIDATION: Check if the heuristic URL actually works
            is_valid = False
            if url and "http" in url:
                try:
                    # Use a short timeout to keep it snappy
                    head_resp = requests.head(url, timeout=1.5)
                    if head_resp.status_code == 200:
                        is_valid = True
                except:
                    pass
            
            # 3. Fallback to API if heuristic is broken (404, timeout, etc.)
            if not is_valid:
                print(f"   -> Heuristic image miss for '{ing.name}', using API fallback...")
                url = get_ingredient_image_url.invoke(ing.name)
            
            if url and "Error" in url: url = None
            
            updated.append(Ingredient(name=ing.name, amount=ing.amount, imageUrl=url))
        else:
             updated.append(ing)
        
    return {"enriched_ingredients": updated}


def node_enrich_steps(state: AgentState):
    """Enriches recipe steps with images (Worker)."""
    recipe = state.get('recipe')
    if not recipe or not recipe.steps: return {}

    print(f"--- 📸 Enriching Steps ({WORKER_MODEL} for keywords) ---")
    
    updated_steps = []
    
    for step in recipe.steps:
        new_step = step.model_copy()
        
        # Only fetch if missing
        if not new_step.imageUrl:
            query_to_use = new_step.visual_query if new_step.visual_query else new_step.instruction
            
            if query_to_use:
                # Ground query for Pexels
                pexels_query = f"{query_to_use} cooking food"
                image_url = None
                
                try:
                    # 1. Try Pexels (Free)
                    image_url = pexels_image_search.invoke(pexels_query)
                    
                    # 2. Fallback to Google (Paid) if Pexels fails or returns "No image found."
                    if not image_url or "Error" in image_url or "No image" in image_url:
                        print(f"   -> Pexels miss for '{query_to_use}', checking Google...")
                        image_url = google_image_search.invoke(query_to_use)
                except Exception:
                    pass
                
                # Assign if valid
                if image_url and "Error" not in image_url and "No image" not in image_url:
                    new_step.imageUrl = image_url
        
        updated_steps.append(new_step)
        
    return {"enriched_steps": updated_steps}

def node_polish_recipe(state: AgentState):
    """Polishes recipe prose (capitalization, punctuation) using the refiner model."""
    recipe = state.get("recipe")
    if not recipe: return {}
    
    print(f"--- ✍️ Polishing Recipe Prose ({REFINER_MODEL}) ---")
    
    # We send the whole recipe as JSON to the refiner and ask it to fix just the text strings
    recipe_json = recipe.model_dump_json()
    
    prompt = f"""
    You are a professional food editor. Polish the text in this recipe JSON.
    1. Capitalize the first letter of every sentence and every ingredient name.
    2. Ensure proper punctuation (periods at the end of instructions).
    3. Maintain a professional, clear tone.
    4. DO NOT change the structure of the JSON.
    5. DO NOT change measurements, ingredients, or the total_time value, just the formatting of prose.
    
    Recipe JSON:
    {recipe_json}
    
    Return ONLY the polished version as a raw JSON object. No Markdown.
    """
    
    try:
        response = refiner_llm.invoke([HumanMessage(content=prompt)])
        content = response.content.replace("```json", "").replace("```", "").strip()
        data = json.loads(content)
        return {"recipe": Recipe(**data)}
    except Exception as e:
        print(f"Polishing error: {e}")
        return {}


def node_pre_enrichment(state: AgentState):
    return {}

def node_merge_enrichment(state: AgentState):
    """Merges enriched data."""
    recipe = state.get('recipe')
    if not recipe: return {}
    
    updates = {}
    if state.get('enriched_ingredients'): updates['ingredients'] = state.get('enriched_ingredients')
    if state.get('enriched_steps'): updates['steps'] = state.get('enriched_steps')
        
    if updates: return {"recipe": recipe.model_copy(update=updates)}
    return {}


# --- Graph Construction ---

graph = StateGraph(AgentState)

# 1. Processing Nodes
graph.add_node("process_video_file", node_process_video_file)
graph.add_node("process_image_file", node_process_image_file)
graph.add_node("scrape_website", node_scrape_website)
graph.add_node("get_video_metadata", node_get_video_metadata)
graph.add_node("check_video_metadata", node_check_video_metadata)

# 2. Logic Nodes
graph.add_node("extract_text_from_video", node_extract_text_from_video)
graph.add_node("analyze_image_type", node_analyze_image_type)
graph.add_node("recipe_from_ingredients", node_recipe_from_ingredients)
graph.add_node("recipe_from_dish_image", node_recipe_from_dish_image)
graph.add_node("extract_from_text", node_extract_from_text)

# 3. Formatting & Polishing Nodes
graph.add_node("format_recipe", node_format_recipe)
graph.add_node("polish_recipe", node_polish_recipe) # New Node!
graph.add_node("enrich_ingredients", enrich_ingredients)
graph.add_node("enrich_steps", node_enrich_steps)
graph.add_node("merge_enrichment", node_merge_enrichment)
graph.add_node("pre_enrichment", node_pre_enrichment)

# --- Edges ---

def route_input(state):
    return determine_source_type(state)

def route_image_logic(state):
    if state.get("ingredients_detected"): return "ingredients"
    return "dish"

def route_scrape_logic(state):
    if state.get("recipe"): return "formatted"
    return "raw_text"

def route_video_download_completion(state: AgentState):
    """Routes based on whether the video was actually downloaded."""
    if state.get("video_file_path") and os.path.exists(state["video_file_path"]):
        return "multimodal"
    return "metadata_fallback"

graph.add_conditional_edges(START, route_input, {
    "youtube": "check_video_metadata",
    "video_file": "check_video_metadata",
    "image_file": "process_image_file",
    "website": "scrape_website"
})

def route_video_metadata(state):
    if state.get("metadata_sufficient") == "yes": return "extract_text"
    return "download_video"

graph.add_conditional_edges("check_video_metadata", route_video_metadata, {
    "extract_text": "extract_from_text",
    "download_video": "process_video_file"
})
graph.add_edge("get_video_metadata", "extract_from_text")

graph.add_conditional_edges("process_video_file", route_video_download_completion, {
    "multimodal": "extract_text_from_video",
    "metadata_fallback": "get_video_metadata"
})

graph.add_conditional_edges("scrape_website", route_scrape_logic, {
    "formatted": "polish_recipe", # Route through refiner
    "raw_text": "extract_from_text"
})

# process_video_file is now a conditional predecessor
graph.add_edge("process_image_file", "analyze_image_type")

graph.add_conditional_edges("analyze_image_type", route_image_logic, {
    "ingredients": "recipe_from_ingredients",
    "dish": "recipe_from_dish_image"
})

graph.add_edge("extract_from_text", "format_recipe")
graph.add_edge("extract_text_from_video", "format_recipe")
graph.add_edge("recipe_from_ingredients", "format_recipe")
graph.add_edge("recipe_from_dish_image", "format_recipe")

# All formatted recipes go through the polish_recipe node
graph.add_edge("format_recipe", "polish_recipe")
graph.add_edge("polish_recipe", "pre_enrichment")

graph.add_edge("pre_enrichment", "enrich_ingredients")
graph.add_edge("pre_enrichment", "enrich_steps")
graph.add_edge("enrich_ingredients", "merge_enrichment")
graph.add_edge("enrich_steps", "merge_enrichment")
graph.add_edge("merge_enrichment", END)

workflow = graph.compile()

if __name__ == "__main__":
    print(f"\n=== PlateIt Agent | Orchestrator: {ORCHESTRATOR_MODEL} | Worker: {WORKER_MODEL} ===")
    while True:
        url = input("\nEnter URL (or 'q'): ").strip()
        if url == 'q': break
        try:
            res = workflow.invoke({"url": url})
            if res.get('recipe'):
                r = res['recipe']
                print(f"\nSuccessfully extracted: {r.name}")
                print(r)
            else:
                print("Failed.")
        except Exception as e:
            print(f"Error: {e}")
