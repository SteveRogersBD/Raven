import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage

# Load env
load_dotenv()

# Get Key
api_key = os.getenv("OPEN_API_KEY")

print(f"Testing OpenAI connection with key ending in: ...{api_key[-4:] if api_key else 'None'}")

if not api_key:
    print("❌ Error: OPEN_API_KEY not found in .env")
    exit(1)

try:
    # Initialize Chat Model
    llm = ChatOpenAI(model="gpt-5.2", api_key=api_key)
    
    print("\nSending request to GPT-5.2...")
    
    # Send a simple message
    response = llm.invoke([
        HumanMessage(content="Hello! Verify you are working by telling me a 1-sentence cooking tip.")
    ])
    
    print("\n✅ Success!")
    print(f"Response: {response.content}")
    
except Exception as e:
    print(f"\n❌ Connection Failed: {e}")
