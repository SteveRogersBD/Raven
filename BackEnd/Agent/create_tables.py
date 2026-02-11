from sqlmodel import create_engine, SQLModel
from dotenv import load_dotenv
import os
import models  # Import your models module to register classes

# Load .env
load_dotenv()

# Get DB URL
DATABASE_URL = os.getenv("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(DATABASE_URL, echo=True)

def update_schema():
    print("Creating new tables defined in SQLModel metadata...")
    # SQLModel.metadata.create_all(engine) will create tables ONLY if they don't exist.
    # It will NOT update existing tables (e.g. adding columns).
    SQLModel.metadata.create_all(engine)
    print("Schema update complete.")

if __name__ == "__main__":
    update_schema()
