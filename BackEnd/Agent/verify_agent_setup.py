import os
import sys
from dotenv import load_dotenv

# Load env variables
load_dotenv()

print("--- Testing Agent Initialization ---")

try:
    from better_agent import workflow, ORCHESTRATOR_MODEL, WORKER_MODEL
    print(f"✅ Agent loaded successfully.")
    print(f"   Orchestrator (Brain): {ORCHESTRATOR_MODEL}")
    print(f"   Worker (Formatter): {WORKER_MODEL}")
    
    # Test URL - A simple text-based recipe page
    test_url = "https://smittenkitchen.com/2026/01/simple-crispy-pan-pizza/"
    
    print(f"\n--- 🚀 Running LIVE Test with URL: {test_url} ---")
    
    result = workflow.invoke({"url": test_url})
    
    if result.get('recipe'):
        r = result['recipe']
        print(f"\n✅ SUCCESSFULLY EXTRACTED RECIPE!")
        print(f"   Name: {r.name}")
        print(f"   Steps: {len(r.steps)}")
        print(f"   Ingredients: {len(r.ingredients)}")
        print(f"   Source Image: {r.source_image}")
        print("\nJSON Preview:")
        print(r.model_dump_json(indent=2)[:500] + "...")
    else:
        print("\n❌ Failed to extract recipe.")
        if "text_content" in result:
             print(f"Text Content Retrieved: {result['text_content'][:200]}...")
    
except ImportError as e:
    print(f"❌ Import Error: {e}")
except Exception as e:
    print(f"❌ Execution Error: {e}")
    import traceback
    traceback.print_exc()
