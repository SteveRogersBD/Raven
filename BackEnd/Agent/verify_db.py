import os
from sqlalchemy import create_engine, text
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Get the database URL
database_url = os.getenv("DATABASE_URL")

print(f"Testing connection to: {database_url}")

try:
    # Create the engine
    engine = create_engine(database_url)
    
    # Try to connect and run a simple query
    with engine.connect() as connection:
        result = connection.execute(text("SELECT 1"))
        print("\n✅ Connection Successful!")
        for row in result:
            print(f"Test Query Result: {row[0]}")
            
except Exception as e:
    print("\n❌ Connection Failed!")
    print(f"Error: {e}")
