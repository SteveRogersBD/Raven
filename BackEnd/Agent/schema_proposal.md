# Proposed Database Schema

Based on your request, here are the table definitions for `Cookbook` and `CookingSession`.

## 1. Cookbook Table
Stores extracted recipes for the user.

```python
class Cookbook(SQLModel, table=True):
    __tablename__ = "cookbook"
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    title: str
    
    # Stores the full recipe JSON (ingredients, steps, nutritional info, etc.)
    # This allows flexibility as the recipe format evolves.
    recipe_data: Dict = Field(default={}, sa_column=Column(JSON)) 
    
    source_url: Optional[str] = None
    thumbnail_url: Optional[str] = None
    created_at: datetime = Field(default_factory=datetime.utcnow)
```

## 2. Cooking Session Table
Tracks the user's progress through a recipe.

```python
class CookingSession(SQLModel, table=True):
    __tablename__ = "cooking_session"
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: uuid.UUID = Field(foreign_key="user.id", index=True)
    
    # Links to the saved recipe in the Cookbook
    cookbook_id: Optional[int] = Field(default=None, foreign_key="cookbook.id")
    
    # Tracks progress
    current_step_index: int = Field(default=0) # 0-indexed step number
    is_finished: bool = Field(default=False)
    
    # Timestamps
    last_updated: datetime = Field(default_factory=datetime.utcnow)
    created_at: datetime = Field(default_factory=datetime.utcnow)
```
