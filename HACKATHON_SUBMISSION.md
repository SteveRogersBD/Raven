# PlateIt - Hackathon Submission

## Inspiration

We all watch recipe videos and posts online constantly—on YouTube, TikTok, Instagram—but rarely actually cook them. The gap between inspiration and action is huge. We wanted to bridge that gap by making it frictionless to go from watching a video to actually cooking the meal. PlateIt removes the barriers: extract the recipe automatically, check what you're missing, create a shopping list, and then guide you through cooking with live AI assistance.

## What it does

PlateIt transforms recipe videos into actionable cooking experiences:

- **🎥 Automatic Recipe Extraction**: Paste a recipe video link (From Youtube, Tiktok, Twitter, Instagram) or a recipe blog link. Our AI agent find the exact (yes you heard me right!) or recipe from that source.
- **📚 Personal Cookbook**: Save extracted recipes to your digital cookbook for easy access anytime.
- **�  Pantry Management**: Scan your fridge or ingredients with your camera. Gemini identifies what you have and builds your pantry inventory.
- **🔍 Missing Ingredients Check**: When viewing a recipe, instantly see which ingredients you have and which you're missing.
- **🛒 Smart Shopping Lists**: Generate a grocery list from missing ingredients, grouped by category, ready to take to the store.
- **🍳 Guided Cooking Sessions**: Start a cooking session and follow step-by-step instructions with large, readable text.
- **🤖 Live AI Chef Assistant**: Ask questions during cooking—get tips, substitutions, timing advice, or clarifications on any step in real-time.

## How we built it

**Tech Stack:**
- **Mobile**: Android (Java) with Material Design
- **Backend**: FastAPI (Python) with LangGraph orchestration
- **AI Engine**: Google Gemini 1.5/2.0 Flash & Pro for video understanding, vision, and conversational assistance
- **Database**: Supabase (PostgreSQL) for users, recipes, pantry data, and cooking sessions
- **External APIs**: Spoonacular (recipes), SerpApi (YouTube search)
- **Deployment**: Google Cloud Run (Dockerized)

**Core Workflow:**
1. **Video Recipe Extraction**: User pastes a recipe video link or uploads a video file. Gemini 1.5 Pro watches the video and extracts ingredients, quantities, and step-by-step instructions into a structured recipe format.
2. **Recipe Storage**: Extracted recipes are saved to the user's personal cookbook in Supabase.
3. **Pantry Scanning**: User takes a photo of their fridge or ingredients. Gemini's vision model identifies items and populates the pantry inventory.
4. **Ingredient Matching**: When viewing a recipe, the app compares recipe ingredients against the pantry to show what's available and what's missing.
5. **Shopping List Generation**: Missing ingredients are compiled into a categorized shopping list the user can take to the store.
6. **Cooking Session**: User starts cooking a recipe. The app displays one step at a time with large, readable text.
7. **Live AI Assistance**: During cooking, the user can ask the Chef Agent questions about the current step, get substitution suggestions, timing advice, or clarifications—all in real-time context.

## Challenges we ran into

- **Video Recipe Extraction**: Extracting precise, structured recipe data from unstructured video content required careful prompt engineering with Gemini to ensure accuracy and completeness.
- **Ingredient Normalization**: Matching user pantry items (which can be vague or colloquial) to recipe ingredients required fuzzy matching and semantic understanding.
- **Real-Time Context in Cooking**: Maintaining conversation context during a cooking session so the AI assistant understands which step the user is on and can provide relevant help.
- **Pantry Photo Accuracy**: Ensuring Gemini's vision model correctly identifies ingredients in cluttered, real-world fridge photos with varying lighting and angles.
- **API Coordination**: Orchestrating multiple API calls (Gemini for video/vision, Supabase for storage, SerpApi for search) while keeping the app responsive.
- **Time Constraints**: Building a full-stack application in 5 days meant prioritizing the core watch-to-cook workflow over secondary features.

## Accomplishments that we're proud of

- **Seamless Video-to-Recipe Pipeline**: Successfully built an end-to-end workflow that takes a recipe video and converts it into a structured, cookable recipe in seconds.
- **Accurate Pantry Scanning**: Gemini's vision model reliably identifies multiple ingredients from a single photo, even in cluttered, real-world conditions.
- **Context-Aware Cooking Assistance**: The Chef Agent understands which step the user is on and provides relevant, actionable help during cooking sessions.
- **Complete User Journey**: From watching a video to cooking with live AI help—the entire flow is functional and intuitive.
- **Scalable Backend**: Built a production-ready FastAPI server on Google Cloud Run that handles video processing, recipe storage, and real-time chat.
- **Polished Android UI**: Created a smooth, Material Design interface that makes the watch-to-cook journey feel natural and effortless.

## What we learned

- **Gemini's Video Understanding is Game-Changing**: Gemini 1.5 Pro's ability to process entire videos and extract structured information opens up possibilities for automating recipe extraction at scale.
- **Multimodal AI Solves Real Problems**: Combining vision (pantry scanning) and video (recipe extraction) with conversational AI creates a genuinely useful product.
- **User-Centric Problem Definition**: The core insight—people watch recipes but don't cook them—led to a focused feature set that directly addresses that friction point.
- **LangGraph for Stateful Conversations**: Using LangGraph to manage cooking session state made it easy to keep the AI assistant context-aware throughout the cooking process.
- **Rapid Iteration Wins**: Focusing on the core watch-to-cook workflow first allowed us to deliver a working product quickly and iterate based on real usage patterns.

## What's next for PlateIt

- **Voice-Guided Cooking**: Add voice input/output so users can ask questions and receive guidance hands-free while cooking.
- **Ingredient Substitution Database**: Build a comprehensive, AI-powered substitution engine for common ingredients based on dietary restrictions and allergies.
- **Meal Planning**: Help users plan weekly meals based on available ingredients and dietary preferences.
- **Social Sharing**: Allow users to share recipes, cooking tips, and pantry discoveries with friends and the PlateIt community.
- **Offline Mode**: Cache recipes and enable basic cooking guidance without internet connectivity.
- **Nutritional Insights**: Integrate nutritional data to help users make healthier choices and track macros.
- **Community Recipes**: Enable users to upload and share their own extracted recipes with the PlateIt community.
- **Wearable Integration**: Extend to smartwatches for hands-free cooking guidance and step navigation.
