from sqlmodel import Session, select, create_engine
from models import CookingSession, User
from dotenv import load_dotenv
import os

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")
if DATABASE_URL and DATABASE_URL.startswith("postgres://"):
    DATABASE_URL = DATABASE_URL.replace("postgres://", "postgresql://", 1)

engine = create_engine(DATABASE_URL)

def check_sessions():
    with Session(engine) as session:
        statement = select(CookingSession).order_by(CookingSession.id.desc())
        results = session.exec(statement).all()
        
        print(f"--- Found {len(results)} Total Cooking Sessions ---")
        for sess in results:
            print(f"ID: {sess.id} | User: {sess.user_id} | Updated: {sess.last_updated} | Finished: {sess.is_finished}")

if __name__ == "__main__":
    check_sessions()
