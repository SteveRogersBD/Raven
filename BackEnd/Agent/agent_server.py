from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel
from sqlmodel import Session, select
from typing import Optional, List, Dict, Any
import uuid
import os
import requests
from dotenv import load_dotenv
from schemas_pantry import IngredientSearchRequest, RecipeSummary

load_dotenv()

from better_agent import workflow as recipe_workflow
from database import get_session, create_db_and_tables
from models import User, PantryItem, VideoRecommendation, Cookbook, CookingSession
from tools import search_youtube_videos
import random
from datetime import datetime, timedelta

app = FastAPI()

# Keys are loaded from environment variables (Cloud Run or .env file)
# os.environ["GOOGLE_API_KEY"] and "GEMINI_API_KEY" should be set in the environment.

# --- Auth Models ---
class SignupRequest(BaseModel):
    full_name: Optional[str] = None
    username: str
    email: str
    password: str

class SigninRequest(BaseModel):
    email: str
    password: str

class UpdatePreferencesRequest(BaseModel):
    user_id: uuid.UUID
    preferences: list[str]

class AuthResponse(BaseModel):
    user_id: uuid.UUID
    email: str
    username: str
    message: str

class PantryItemCreate(BaseModel):
    user_id: uuid.UUID
    name: str
    amount: Optional[str] = None
    image_url: Optional[str] = None

# --- Auth Endpoints ---
@app.post("/signup", response_model=AuthResponse)
def signup(request: SignupRequest, session: Session = Depends(get_session)):
    # Check if user exists
    existing_user = session.exec(select(User).where((User.email == request.email) | (User.username == request.username))).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="User with this email or username already exists")
    
    # Create new user (Storing password as plain text as requested)
    new_user = User(
        email=request.email,
        username=request.username,
        full_name=request.full_name,
        password=request.password 
    )
    session.add(new_user)
    session.commit()
    session.refresh(new_user)
    
    return AuthResponse(
        user_id=new_user.id,
        email=new_user.email,
        username=new_user.username,
        message="User created successfully"
    )

@app.post("/signin", response_model=AuthResponse)
def signin(request: SigninRequest, session: Session = Depends(get_session)):
    statement = select(User).where(User.email == request.email)
    user = session.exec(statement).first()
    
    if not user or user.password != request.password:
        raise HTTPException(status_code=401, detail="Invalid credentials")
        
    return AuthResponse(
        user_id=user.id,
        email=user.email,
        username=user.username,
        message="Login successful"
    )

@app.post("/users/preferences")
def update_preferences(request: UpdatePreferencesRequest, session: Session = Depends(get_session)):
    print(f"--- Update Preferences Request ---")
    print(f"User ID: {request.user_id}")
    print(f"Preferences: {request.preferences}")
    
    try:
        user = session.get(User, request.user_id)
        if not user:
            print(f"User not found: {request.user_id}")
            raise HTTPException(status_code=404, detail="User not found")
        
        print(f"Found user: {user.email}")
        user.preferences = request.preferences
        session.add(user)
        session.commit()
        session.refresh(user)
        
        print(f"Preferences updated successfully: {user.preferences}")
        return {"message": "Preferences updated", "preferences": user.preferences}
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error updating preferences: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/users/preferences/{user_id}")
def get_preferences(user_id: uuid.UUID, session: Session = Depends(get_session)):
    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    return {"preferences": user.preferences}


# --- Video Recommendation Endpoint ---
@app.get("/recommendations/videos/{user_id}")
def get_video_recommendations(user_id: uuid.UUID, session: Session = Depends(get_session)):
    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # 1. Check Cache
    cutoff_time = datetime.utcnow() - timedelta(hours=24)
    cached_videos = session.exec(
        select(VideoRecommendation)
        .where(VideoRecommendation.user_id == user_id)
        .where(VideoRecommendation.created_at > cutoff_time)
    ).all()
    
    # Check if cached videos are missing new fields (views, length)
    is_cache_incomplete = any(v.views is None or v.length is None for v in cached_videos) if cached_videos else False

    if cached_videos and not is_cache_incomplete:
        print(f"DEBUG: Cache HIT for user {user_id}. Returning {len(cached_videos)} videos.")
        return {
            "videos": [
                {
                    "title": v.title,
                    "link": v.link,
                    "thumbnail": v.thumbnail_url,
                    "channel": v.channel_name,
                    "views": v.views,
                    "length": v.length,
                }
                for v in cached_videos
            ]
        }

    # 2. If Cache Miss/Expired/Incomplete -> Fetch New
    if is_cache_incomplete:
        print(f"DEBUG: Cache incomplete (missing fields) for user {user_id}. Refreshing...")
    else:
        print(f"DEBUG: Cache MISS for user {user_id}. Fetching fresh videos.")
    preferences = user.preferences if user.preferences else []
    all_videos = []
    seen_links = set()

    # Strategy: diverse sampling
    if len(preferences) > 3:
        target_prefs = random.sample(preferences, 3)
    else:
        target_prefs = preferences

    # Prepare queries
    queries = []
    if not target_prefs:
        queries.append("chicken recipes")
    else:
        for p in target_prefs:
            queries.append(f"{p} recipes")

    print(f"Fetching videos for topics: {queries}")

    # Fetch and Aggregate
    for q in queries:
        videos = search_youtube_videos(q, limit=5)
        if isinstance(videos, list):
            for v in videos:
                if v.get('link') and v['link'] not in seen_links:
                    all_videos.append(v)
                    seen_links.add(v['link'])
    
    random.shuffle(all_videos)

    # 3. Update Cache
    existing_recs = session.exec(select(VideoRecommendation).where(VideoRecommendation.user_id == user_id)).all()
    for rec in existing_recs:
        session.delete(rec)
    
    # Insert new ones
    for v in all_videos:
        vid_id = ""
        if "v=" in v.get('link', ''):
             vid_id = v['link'].split('v=')[-1].split('&')[0]

        new_rec = VideoRecommendation(
            user_id=user_id,
            video_id=vid_id,
            title=v.get('title', 'Unknown'),
            thumbnail_url=v.get('thumbnail'),
            channel_name=v.get('channel'),
            views=v.get('views'),
            length=v.get('length'),
            link=v.get('link'),
            created_at=datetime.utcnow()
        )
        session.add(new_rec)
    
    session.commit()

    print(f"DEBUG: Sending {len(all_videos)} fresh videos to user {user_id}")
    return {"videos": all_videos}





# --- Pantry Endpoints ---
@app.get("/pantry/{user_id}")
def get_pantry_items(user_id: uuid.UUID, session: Session = Depends(get_session)):
    items = session.exec(select(PantryItem).where(PantryItem.user_id == user_id).order_by(PantryItem.created_at.desc())).all()
    return items

@app.post("/pantry/add")
def add_pantry_item(item: PantryItemCreate, session: Session = Depends(get_session)):
    new_item = PantryItem(
        user_id=item.user_id,
        name=item.name,
        amount=item.amount,
        image_url=item.image_url
    )
    session.add(new_item)
    session.commit()
    session.refresh(new_item)
    return new_item

@app.delete("/pantry/{item_id}")
def delete_pantry_item(item_id: int, session: Session = Depends(get_session)):
    item = session.get(PantryItem, item_id)
    if not item:
        raise HTTPException(status_code=404, detail="Item not found")
    session.delete(item)
    session.commit()
    return {"message": "Item deleted"}

# --- Existing Recipe Extraction ---
class VideoRequest(BaseModel):
    video_url: str

@app.post("/extract_recipe")
def extract_recipe(request: VideoRequest):
    initial_state = {"url": request.video_url}
    try:
        final_state = recipe_workflow.invoke(initial_state)
        return final_state.get('recipe',{})
    except Exception as e:
        print(f"Error executing workflow: {e}")
        raise HTTPException(status_code=500, detail=str(e))

from fastapi import File, UploadFile
import shutil

@app.post("/extract_recipe_image")
def extract_recipe_image(file: UploadFile = File(...)):
    try:
        # Save the uploaded file temporarily
        temp_filename = f"temp_upload_{file.filename}"
        with open(temp_filename, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        abs_path = os.path.abspath(temp_filename)
        
        # Pass the local file path as the URL
        initial_state = {"url": abs_path}
        
        # Invoke agent
        final_state = recipe_workflow.invoke(initial_state)
        
        # Clean up is done by agent usually, but we can verify later
        
        return final_state.get('recipe', {})
    except Exception as e:
         print(f"Error processing image: {e}")
         raise HTTPException(status_code=500, detail=str(e))

# --- New Cooking Chat ---
from chef_agent import graph as chef_workflow
from langchain_core.messages import HumanMessage
from typing import Dict, Any

# --- New Cooking Chat ---
class ChatRequest(BaseModel):
    message: str
    thread_id: str
    recipe: Optional[Dict[str, Any]] = None # Full recipe object, optional for general chat
    current_step: int # 0-indexed step
    image_data: Optional[str] = None # Base64 encoded image

@app.post("/chat")
def chat_endpoint(request: ChatRequest):
    print(f"--- Chat Request: {request.message} (Step {request.current_step}) ---")
    
    # 1. Construct State
    # Convert dict back to Recipe object logic is handled by Pydantic inside the node usually,
    # but since our AgentState expects a 'Recipe' object (Pydantic model) and we get a Dict,
    # we might need to rely on the node to handle it or convert it here.
    # The chef_node currently does: recipe = state.get("recipe")
    # We should pass the dict, and let's ensure chef_node handles dict access OR convert it here.
    # For simplicity, we pass the dict and if our chef_agent expects a Pydantic object, we convert it there 
    # OR we convert it here.
    # Let's convert it here for type safety if better_agent is available.
    
    from better_agent import Recipe
    recipe_obj = None
    if request.recipe:
        try:
            recipe_obj = Recipe(**request.recipe)
        except Exception as e:
            print(f"Warning: Could not parse recipe object: {e}")
            recipe_obj = None

    initial_state = {
        "messages": [HumanMessage(content=request.message)],
        "recipe": recipe_obj,
        "current_step": request.current_step,
        "image_data": request.image_data
    }
    
    # 2. Invoke Chef Agent
    try:
        final_state = chef_workflow.invoke(initial_state)
        
        # 3. Extract Response
        # The waiter node puts the JSON string in the last message's content
        last_message = final_state["messages"][-1]
        response_json_str = last_message.content
        
        # We assume it's valid JSON because Waiter guarantees it (mostly)
        import json
        response_data = json.loads(response_json_str)
        
        return response_data
        
    except Exception as e:
        print(f"Chat Error: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

# --- Recipe Details Endpoint ---
@app.get("/recipes/{recipe_id}/full")
def get_full_recipe_details(recipe_id: int):
    """
    Fetches full recipe details from Spoonacular and maps to App's RecipeResponse format.
    """
    import os
    import requests
    
    api_key = os.getenv("SPOONACULAR_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="API Key missing")
        
    url = f"https://api.spoonacular.com/recipes/{recipe_id}/information"
    params = {"apiKey": api_key}
    
    try:
        resp = requests.get(url, params=params)
        resp.raise_for_status()
        data = resp.json()
        
        # Map to App Format
        # 1. Ingredients
        ingredients = []
        for ing in data.get("extendedIngredients", []):
            amount = f"{ing.get('amount', '')} {ing.get('unit', '')}".strip()
            ingredients.append({
                "name": ing.get("original", ing.get("name")),
                "amount": amount,
                "imageUrl": f"https://img.spoonacular.com/ingredients_100x100/{ing.get('image', '')}"
            })
            
        # 2. Steps
        steps = []
        if data.get("analyzedInstructions"):
            for step in data["analyzedInstructions"][0].get("steps", []):
                steps.append({
                    "instruction": step.get("step"),
                    "visual_query": None,
                    "imageUrl": None
                })
        else:
            # Fallback to splitting instructions string
            instr = data.get("instructions", "")
            if instr:
                # Remove HTML tags if any
                import re
                clean_instr = re.sub('<[^<]+?>', '', instr)
                steps = [{
                    "instruction": s.strip(),
                    "visual_query": None,
                    "imageUrl": None
                } for s in clean_instr.split('.') if s.strip()]
                
        return {
            "name": data.get("title"),
            "total_time": str(data.get("readyInMinutes", 0)) + " mins",
            "ingredients": ingredients,
            "steps": steps
        }
        
    except Exception as e:
         print(f"Error fetching recipe {recipe_id}: {e}")
         raise HTTPException(status_code=500, detail=str(e))

# --- Pantry Extraction Endpoint ---
# --- Pantry Extraction Endpoint (Image) ---
@app.post("/pantry/scan_image")
async def scan_pantry_image(file: UploadFile = File(...)):
    """
    Analyzes an image file (Multipart) and returns a list of pantry items.
    """
    import google.generativeai as genai
    import json
    
    print(f"--- Pantry Scan Request (File: {file.filename}) ---")
    
    try:
        # 1. Save locally
        temp_filename = f"temp_pantry_{uuid.uuid4()}.jpg"
        with open(temp_filename, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # 2. Configure Gemini
        api_key = os.environ.get("GOOGLE_API_KEY")
        genai.configure(api_key=api_key)
        
        # 3. Upload to Gemini
        print("Uploading to Gemini...")
        sample_file = genai.upload_file(path=temp_filename, display_name="Pantry Image")
        
        # 4. Prompt
        # User requested "Gemini 3", using gemini-3-flash-preview as the vision workhorse
        model = genai.GenerativeModel('gemini-3-flash-preview')
        
        prompt = """
        Analyze this image and identify all food items visible.
        Return ONLY a JSON array of objects with 'name' and 'amount' fields.
        Example:
        [
            {"name": "Milk", "amount": "1 Gallon"},
            {"name": "Eggs", "amount": "12 count"}
        ]
        If implicit, estimate the amount. If unsure, use "1".
        Do not include Markdown formatting (```json ... ```). Just the raw JSON string.
        """
        
        print("Generating content...")
        response = model.generate_content([sample_file, prompt])
        
        # 5. Cleanup
        try:
            genai.delete_file(sample_file.name)
            os.remove(temp_filename)
        except Exception as e:
            print(f"Cleanup warning: {e}")

        # 6. Parse
        content = response.text.replace("```json", "").replace("```", "").strip()
        items = json.loads(content)
        
        # 7. Enrich (Simple Loop)
        for item in items:
            item["image_url"] = _get_image_for_item(item.get("name", ""))
            
        return {"items": items}

    except Exception as e:
        print(f"Pantry Scan Error: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

# --- Dish Identification Endpoint ---
@app.post("/recipes/identify_dish")
async def identify_dish_from_image(file: UploadFile = File(...)):
    """
    Analyzes a dish image and returns a full recipe.
    """
    import google.generativeai as genai
    import json
    
    print(f"--- Dish Analysis Request (File: {file.filename}) ---")
    
    try:
        # 1. Save locally
        temp_filename = f"temp_dish_{uuid.uuid4()}.jpg"
        with open(temp_filename, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
            
        # 2. Configure Gemini
        api_key = os.environ.get("GOOGLE_API_KEY")
        genai.configure(api_key=api_key)
        
        # 3. Upload
        print("Uploading to Gemini...")
        sample_file = genai.upload_file(path=temp_filename, display_name="Dish Image")
        
        # 4. Prompt
        # User requested "Gemini 3", using gemini-3-flash-preview
        model = genai.GenerativeModel('gemini-3-flash-preview')
        
        prompt = """
        You are an expert Chef. The user has uploaded a photo of a finished dish.
        1. Identify the dish.
        2. Create an authentic, detailed recipe for it.
        
        Return the result as a strictly formatted JSON object matching this schema:
        {
            "name": "Recipe Name",
            "total_time": "e.g. 45 mins",
            "ingredients": [
                {"name": "Ingredient 1", "amount": "Quantity", "imageUrl": null}
            ],
            "steps": [
                {"instruction": "Step 1...", "visual_query": "search term for step 1", "imageUrl": null}
            ]
        }
        Do not include Markdown formatting. Just the raw JSON.
        """
        
        print("Generating content...")
        response = model.generate_content([sample_file, prompt])
        
        # 5. Cleanup
        try:
            genai.delete_file(sample_file.name)
            os.remove(temp_filename)
        except Exception as e:
            print(f"Cleanup warning: {e}")
            
        # 6. Parse
        content = response.text.replace("```json", "").replace("```", "").strip()
        recipe_data = json.loads(content)
        
        # 7. Enrich Ingredients (Optional but nice)
        if "ingredients" in recipe_data:
            for ing in recipe_data["ingredients"]:
                 ing["imageUrl"] = _get_image_for_item(ing.get("name", ""))
        
        return recipe_data

    except Exception as e:
        print(f"Dish Analysis Error: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

# --- Pantry Recipe Search ---
@app.post("/recipes/findByIngredients", response_model=List[RecipeSummary])
def find_recipes_by_ingredients(request: IngredientSearchRequest):
    """
    Find recipes that use the given ingredients.
    """
    api_key = os.getenv("SPOONACULAR_API_KEY")
    if not api_key:
        raise HTTPException(status_code=500, detail="SPOONACULAR_API_KEY not configured")

    if not request.ingredients:
        return []

    print(f"--- Recipe Search Request: {request.ingredients} ---")
    url = "https://api.spoonacular.com/recipes/findByIngredients"
    params = {
        "ingredients": ",".join(request.ingredients),
        "number": request.number,
        "ranking": 2, # Minimize missing ingredients
        "ignorePantry": True,
        "apiKey": api_key
    }
    print(f"Calling Spoonacular: {url} with params: {params}")

    try:
        resp = requests.get(url, params=params)
        print(f"Spoonacular Status: {resp.status_code}")
        print(f"Spoonacular Body: {resp.text}")
        
        resp.raise_for_status()
        data = resp.json()

        # Map to our model
        results = []
        for item in data:
            results.append(RecipeSummary(
                id=item.get("id"),
                title=item.get("title"),
                image=item.get("image"),
                usedIngredientCount=item.get("usedIngredientCount", 0),
                missedIngredientCount=item.get("missedIngredientCount", 0),
                likes=item.get("likes", 0)
            ))
        
        return results

    except Exception as e:
        print(f"Error finding recipes: {e}")
        raise HTTPException(status_code=500, detail=str(e))

def _get_image_for_item(item_name: str) -> str:
    """
    Tries to find an image URL for the given item name.
    1. Fast Guess (Standard Pattern) - FREE
    2. Spoonacular API Search - FALLBACK
    """
    import requests
    import os
    
    if not item_name: return ""

    # 1. Fast Guess: Try the standard Spoonacular CDN pattern (FREE)
    # Most items follow 'name-with-hyphens.jpg'
    formatted_name = item_name.lower().strip().replace(" ", "-")
    fast_url = f"https://img.spoonacular.com/ingredients_250x250/{formatted_name}.jpg"
    
    try:
        # Check if the image actually exists without downloading it
        head_resp = requests.head(fast_url, timeout=2)
        if head_resp.status_code == 200:
            print(f"DEBUG: Fast image guess succeeded for '{item_name}'")
            return fast_url
    except Exception:
        pass

    # 2. Fallback: Spoonacular API Search (Uses credits)
    spoon_key = os.getenv("SPOONACULAR_API_KEY")
    if spoon_key:
        try:
            print(f"DEBUG: Fast guess failed. Falling back to Spoonacular API for '{item_name}'")
            url = f"https://api.spoonacular.com/food/ingredients/search"
            params = {
                "query": item_name,
                "apiKey": spoon_key,
                "number": 1
            }
            resp = requests.get(url, params=params)
            if resp.status_code == 200:
                data = resp.json()
                if data.get("results"):
                    image_file = data["results"][0]["image"]
                    return f"https://img.spoonacular.com/ingredients_250x250/{image_file}"
        except Exception as e:
            print(f"Spoonacular image fetch error: {e}")

    return ""

@app.get("/get_ingredient_image")
def get_ingredient_image_endpoint(query: str):
    url = _get_image_for_item(query)
    return {"image_url": url}

# --- Cookbook Endpoints ---
class CookbookEntryCreate(BaseModel):
    user_id: uuid.UUID
    title: str
    recipe_data: Dict[str, Any]
    source_url: Optional[str] = None
    thumbnail_url: Optional[str] = None

@app.post("/cookbook/add")
def add_to_cookbook(entry: CookbookEntryCreate, session: Session = Depends(get_session)):
    print(f"--- Adding to Cookbook: {entry.title} for user {entry.user_id} ---")
    
    # Check if duplicate (optional but good practice)
    # existing = session.exec(select(Cookbook).where(Cookbook.user_id == entry.user_id).where(Cookbook.title == entry.title)).first()
    # if existing:
    #      return existing
         
    new_entry = Cookbook(
        user_id=entry.user_id,
        title=entry.title,
        recipe_data=entry.recipe_data,
        source_url=entry.source_url,
        thumbnail_url=entry.thumbnail_url
    )
    session.add(new_entry)
    session.commit()
    session.refresh(new_entry)
    print(f"DEBUG: Added to cookbook: {new_entry.title} (ID: {new_entry.id})")
    return new_entry

@app.get("/cookbook/{user_id}")
def get_cookbook(user_id: uuid.UUID, session: Session = Depends(get_session)):
    recipes = session.exec(select(Cookbook).where(Cookbook.user_id == user_id).order_by(Cookbook.created_at.desc())).all()
    return recipes

@app.delete("/cookbook/{recipe_id}")
def delete_from_cookbook(recipe_id: int, session: Session = Depends(get_session)):
    recipe = session.get(Cookbook, recipe_id)
    if not recipe:
        raise HTTPException(status_code=404, detail="Recipe not found")
    session.delete(recipe)
    session.commit()
    print(f"DEBUG: Deleted cookbook entry: {recipe_id}")
    return {"message": "Recipe deleted"}

# --- Cooking Session Endpoints ---
class CookingSessionCreate(BaseModel):
    user_id: uuid.UUID
    cookbook_id: Optional[int] = None
    # Use cookbook_id preferably, but if not saved in cookbook, we can't easily link it yet in this schema 
    # unless we add 'recipe_snapshot' to session. For now, assume we track sessions for Saved recipes 
    # OR we just track step index for a transient session if we allow null cookbook_id.

@app.post("/cooking/start")
def start_cooking_session(data: CookingSessionCreate, session: Session = Depends(get_session)):
    # Check if active session exists
    active_session = session.exec(
        select(CookingSession)
        .where(CookingSession.user_id == data.user_id)
        .where(CookingSession.is_finished == False)
    ).first()
    
    if active_session:
        # If exists, maybe we just return it or close it?
        # Let's return the existing one if it matches the recipe, otherwise close it and start new?
        # For simplicity: Close old, start new.
        active_session.is_finished = True
        session.add(active_session)
        session.commit()
    
    new_session = CookingSession(
        user_id=data.user_id,
        cookbook_id=data.cookbook_id,
        current_step_index=0,
        is_finished=False
    )
    session.add(new_session)
    session.commit()
    session.refresh(new_session)
    print(f"DEBUG: Started new cooking session: ID={new_session.id}, User={new_session.user_id}, CookbookID={new_session.cookbook_id}")
    return new_session

class CookingProgressUpdate(BaseModel):
    session_id: int
    current_step_index: int
    is_finished: bool

@app.post("/cooking/update")
def update_cooking_progress(data: CookingProgressUpdate, session: Session = Depends(get_session)):
    sess_obj = session.get(CookingSession, data.session_id)
    if not sess_obj:
        raise HTTPException(status_code=404, detail="Session not found")
    
    sess_obj.current_step_index = data.current_step_index
    sess_obj.is_finished = data.is_finished
    sess_obj.last_updated = datetime.utcnow()
    
    session.add(sess_obj)
    session.commit()
    session.refresh(sess_obj)
    print(f"DEBUG: Updated session {data.session_id} -> Step: {data.current_step_index}, Finished: {data.is_finished}")
    return sess_obj

@app.get("/cooking/active/{user_id}")
def get_active_cooking_session(user_id: uuid.UUID, session: Session = Depends(get_session)):
    active_session = session.exec(
        select(CookingSession)
        .where(CookingSession.user_id == user_id)
        .where(CookingSession.is_finished == False)
    ).first()
    
    if not active_session:
        return None
        
    return active_session

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8080))
    uvicorn.run(app, host="0.0.0.0", port=port)