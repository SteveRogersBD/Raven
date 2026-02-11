
from sqlmodel import create_engine, text
from dotenv import load_dotenv
import os

# Load .env
load_dotenv()

# Get DB URL
DATABASE_URL = os.getenv("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(DATABASE_URL, echo=True)

def update_schema():
    with engine.connect() as connection:
        # Check if column exists
        check_sql = text("SELECT column_name FROM information_schema.columns WHERE table_name='user' AND column_name='preferences';")
        result = connection.execute(check_sql).fetchone()
        
        if not result:
            print("Adding 'preferences' column to 'user' table...")
            # Use JSONB if possible for better performance, but JSON works too.
            # Defaulting to empty array string '[]'::json
            alter_sql = text('ALTER TABLE "user" ADD COLUMN preferences JSON DEFAULT \'[]\'::json;')
            connection.execute(alter_sql)
            connection.commit()
            print("Column added successfully.")
        else:
            print("Column 'preferences' already exists.")

        # Check for VideoRecommendation columns
        for col in ["views", "length"]:
            check_v_sql = text(f"SELECT column_name FROM information_schema.columns WHERE table_name='videorecommendation' AND column_name='{col}';")
            v_result = connection.execute(check_v_sql).fetchone()
            if not v_result:
                print(f"Adding '{col}' column to 'videorecommendation' table...")
                alter_v_sql = text(f'ALTER TABLE "videorecommendation" ADD COLUMN {col} TEXT;')
                connection.execute(alter_v_sql)
                connection.commit()
                print(f"Column '{col}' added successfully.")
            else:
                print(f"Column '{col}' already exists in 'videorecommendation'.")

if __name__ == "__main__":
    update_schema()
