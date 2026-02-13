# PlateIt 🍽️ - The Multimodal AI Culinary Orchestrator

**PlateIt** is a state-of-the-art AI-powered culinary assistant that bridges the gap between digital inspiration (YouTube, TikTok, Instagram) and real-world cooking. Built with **Google Gemini 3 Flash Preview** as its central brain, PlateIt "watches" videos, "sees" your pantry, and guides you through cooking with a professional AI sous-chef in your ear.

---

## ✨ Key Features

- **🎥 Video-to-Recipe Extraction**: Paste any cooking video link (YouTube, TikTok, etc.), and our agent natively understands the video to extract a full, structured recipe.
- **🖼️ Vision-Powered Pantry**: Snap a photo of your fridge; Gemini identifies every ingredient and updates your inventory automatically.
- **🤖 Gemini-Orchestrated Chat**: Real-time culinary guidance using voice, text, or images. Ask substitute advice or "how-to" tips while your hands are messy.
- **🛒 Dynamic Shopping Lists**: Move missing ingredients directly to a categorized shopping list.
- **🍳 Professional Cooking Mode**: Step-by-step instruction with large UI, timers, and integrated AI support.
- **💎 Premium Experience**: Integrated with **RevenueCat** for seamless Pro feature access.

---

## 🛠️ Tech Stack

### Mobile App (Android)
- **Framework**: Native Android (Java / Material 3)
- **Networking**: Retrofit2 + OkHttp3 (180s timeouts for AI processing)
- **Image Processing**: Picasso
- **Monetization**: RevenueCat SDK
- **Animation**: Lottie

### Backend API (The "Brain")
- **Core**: FastAPI (Python 3.12)
- **Orchestration**: LangGraph (Advanced State Machines)
- **Multimodal Engine**: **Google Gemini 3 Flash Preview** (Vision & Video)
- **Worker Logic**: GPT-4o (High-precision structured data)
- **Database**: Supabase (PostgreSQL with SQLModel ORM)
- **Deployment**: Google Cloud Run (Dockerized)

---

## 📂 Project Structure

```
PlateIt/
├── app/                  # Android Application Source (Native Java)
├── BackEnd/              # Python Intelligence Layer
│   └── Agent/            # LangGraph Workflow & FastAPI Server
├── .github/workflows/    # CI/CD Automated Deployment Pipeline
├── HACK.md               # Technical Deep-Dive Documentation
└── README.md             # This Guide
```

---

## 🚀 Android Studio Installation Guide

Follow these steps precisely to test PlateIt on your local machine or device.

### 1. Prerequisites
- **Android Studio Koala (2024.1.1)** or newer.
- **Java 11** (Standard for Android development).
- An Android device or Emulator (API Level 24+).

### 2. Clone the Repository
```bash
git clone https://github.com/SteveRogersBD/PlateIt.git
```

### 3. Open in Android Studio
1. Launch Android Studio.
2. Select **File > Open** and navigate to your `PlateIt` folder.
3. Wait for the **Gradle Sync** to finish. This might take 1–3 minutes as it fetches dependencies like RevenueCat, Room, and Retrofit.

### 4. Configure local.properties
The app requires an API key for legacy search fallbacks.
1. Locate `local.properties` in your root project folder (same level as `app/`).
2. Add the following line:
   ```properties
   SERP_API_KEY=your_serp_api_key_here
   ```
   *Note: If you don't have one, just use a placeholder text, as the AI handles most tasks now.*

### 5. Backend Connection (Optional/Local)
By default, the app is pointed to our **Live Cloud Run API**. To test against a local backend:
1. Open `app/src/main/java/com/example/plateit/api/RetrofitClient.java`.
2. Swap the `BASE_URL` from the production link to your local machine IP (e.g., `http://192.168.1.X:8080/`).

### 6. Run the App
1. Select your device from the target dropdown.
2. Click **Run** (Green Play Button).
3. **Important**: If building for Release, ensure you have configured the signing keys or use the **Debug** variant for testing.

---

## 🧠 Backend Setup

If you wish to host the "Brain" locally:

1. **Environment Config**:
   Navigate to `BackEnd/Agent/` and create a `.env` file (see `tools.py` for required keys).
   ```ini
   GEMINI_API_KEY=your_key
   OPEN_API_KEY=your_key
   DATABASE_URL=your_supabase_url
   SPOONACULAR_API_KEY=your_key
   ```
2. **Launch**:
   ```bash
   pip install -r requirements.txt
   uvicorn agent_server:app --host 0.0.0.0 --port 8080
   ```

---

<p align="center">
  Built for the <b>Google Gemini API Developer Competition</b> 🚀
</p>

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/SteveRogersBD">SteveRogersBD</a>
</p>
