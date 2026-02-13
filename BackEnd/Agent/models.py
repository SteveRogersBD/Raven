
import uuid
from datetime import datetime
from typing import Optional, Dict, List
from sqlmodel import SQLModel, Field, Column, JSON

# 1. Users
class User(SQLModel, table=True):
    id: uuid.UUID = Field(default_factory=uuid.uuid4, primary_key=True)
    email: str = Field(index=True, unique=True)
    full_name: Optional[str] = None
    password: str = Field(index=True)
    username: str = Field(index=True, unique=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
    dp_url: Optional[str] = None
    preferences: List[str] = Field(default=[], sa_column=Column(JSON))


# 2. PantryItems
class PantryItem(SQLModel, table=True):
    __tablename__ = "pantry_items_v2"
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    name: str
    amount: Optional[str] = None
    image_url: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

# 3. Recipes
class Recipe(SQLModel, table=True):
    id: int = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    title: str
    # Detailed data (Ingredients/Steps) stored as JSON for flexibility
    content_json: Dict = Field(default={}, sa_column=Column(JSON)) 
    source_url: Optional[str] = None # Youtube link if applicable
    image_url: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)

# 4. ChatSessions (The "Thread")
class ChatSession(SQLModel, table=True):
    id: str = Field(primary_key=True) # This is the 'thread_id' used by LangGraph
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    title: str = Field(default="New Chat")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

# 5. Messages (The actual content)
class Message(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    session_id: str = Field(foreign_key="chatsession.id", index=True)
    sender: str # "user" or "ai"
    content: str # The text content
    ui_type: Optional[str] = "none"
    recipe_data: Optional[Dict] = Field(default=None, sa_column=Column(JSON))
    ingredient_data: Optional[Dict] = Field(default=None, sa_column=Column(JSON))
    video_data: Optional[Dict] = Field(default=None, sa_column=Column(JSON))
    created_at: datetime = Field(default_factory=datetime.utcnow)

# 6. Video Recommendations (Cache)
class VideoRecommendation(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    video_id: str  # YouTube Video ID
    title: str
    thumbnail_url: Optional[str] = None
    channel_name: Optional[str] = None
    views: Optional[str] = None
    length: Optional[str] = None
    link: str
    created_at: datetime = Field(default_factory=datetime.utcnow)

# 7. Cookbook (Saved Recipes)
class Cookbook(SQLModel, table=True):
    __tablename__ = "cookbook"
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    title: str
    recipe_data: Dict = Field(default={}, sa_column=Column(JSON)) 
    source_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)

# 8. CookingSession (Active Cooking State)
class CookingSession(SQLModel, table=True):
    __tablename__ = "cooking_session"
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    cookbook_id: Optional[int] = Field(default=None, foreign_key="cookbook.id")
    current_step_index: int = Field(default=0)
    is_finished: bool = Field(default=False)
    last_updated: datetime = Field(default_factory=datetime.utcnow)

# 9. ShoppingList
class ShoppingList(SQLModel, table=True):
    __tablename__ = "shopping_lists"
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    title: str
    items: List[Dict] = Field(default=[], sa_column=Column(JSON)) # List of dicts like {"name": "Eggs", "amount": "12", "bought": false}
    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)
