package com.example.plateit.api;

import com.example.plateit.requests.ChatRequest;
import com.example.plateit.responses.ChatResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AgentApiService {
        @POST("chat")
        Call<ChatResponse> chat(@Body ChatRequest request);

        @retrofit2.http.GET("recipes/{id}/full")
        retrofit2.Call<com.example.plateit.responses.RecipeResponse> getRecipeDetails(
                        @retrofit2.http.Path("id") int recipeId);

        @retrofit2.http.Multipart
        @retrofit2.http.POST("pantry/scan_image")
        retrofit2.Call<com.example.plateit.responses.PantryScanResponse> scanPantryImage(
                        @retrofit2.http.Part okhttp3.MultipartBody.Part image);

        @retrofit2.http.Multipart
        @retrofit2.http.POST("recipes/identify_dish")
        retrofit2.Call<com.example.plateit.responses.RecipeResponse> identifyDishFromImage(
                        @retrofit2.http.Part okhttp3.MultipartBody.Part image);

        @retrofit2.http.GET("get_ingredient_image")
        retrofit2.Call<com.example.plateit.responses.IngredientImageResponse> getIngredientImage(
                        @retrofit2.http.Query("query") String query);

        @retrofit2.http.GET("pantry/{user_id}")
        retrofit2.Call<java.util.List<com.example.plateit.db.PantryItem>> getPantryItems(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.POST("pantry/add")
        retrofit2.Call<com.example.plateit.db.PantryItem> addPantryItem(
                        @retrofit2.http.Body com.example.plateit.requests.PantryItemCreateRequest request);

        @retrofit2.http.DELETE("pantry/{item_id}")
        retrofit2.Call<Void> deletePantryItem(@retrofit2.http.Path("item_id") int itemId);

        @retrofit2.http.POST("recipes/findByIngredients")
        retrofit2.Call<java.util.List<com.example.plateit.responses.RecipeSummary>> findRecipesByIngredients(
                        @retrofit2.http.Body com.example.plateit.requests.IngredientSearchRequest request);

        @retrofit2.http.GET("recommendations/blogs/{user_id}")
        retrofit2.Call<com.example.plateit.responses.BlogRecommendationResponse> getBlogRecommendations(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.POST("users/preferences")
        retrofit2.Call<Void> updatePreferences(
                        @retrofit2.http.Body com.example.plateit.requests.PreferencesRequest request);

        @retrofit2.http.GET("recommendations/videos/{user_id}")
        retrofit2.Call<com.example.plateit.responses.VideoRecommendationResponse> getRecommendations(
                        @retrofit2.http.Path("user_id") String userId);

        // --- Cookbook ---
        @retrofit2.http.POST("cookbook/add")
        retrofit2.Call<com.example.plateit.responses.CookbookEntry> addToCookbook(
                        @retrofit2.http.Body com.example.plateit.requests.CookbookEntryCreate request);

        @retrofit2.http.GET("cookbook/{user_id}")
        retrofit2.Call<java.util.List<com.example.plateit.responses.CookbookEntry>> getCookbook(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.DELETE("cookbook/{recipe_id}")
        retrofit2.Call<Void> deleteFromCookbook(@retrofit2.http.Path("recipe_id") int recipeId);

        // --- Cooking Session ---
        @retrofit2.http.POST("cooking/start")
        retrofit2.Call<com.example.plateit.responses.CookingSession> startCookingSession(
                        @retrofit2.http.Body com.example.plateit.requests.CookingSessionCreate request);

        @retrofit2.http.POST("cooking/update")
        retrofit2.Call<com.example.plateit.responses.CookingSession> updateCookingProgress(
                        @retrofit2.http.Body com.example.plateit.requests.CookingProgressUpdate request);

        @retrofit2.http.GET("cooking/active/{user_id}")
        retrofit2.Call<com.example.plateit.responses.CookingSession> getActiveCookingSession(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.GET("cooking/sessions/{user_id}")
        retrofit2.Call<java.util.List<com.example.plateit.responses.CookingSession>> getAllCookingSessions(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.GET("chat/sessions/{user_id}")
        retrofit2.Call<java.util.List<com.example.plateit.models.ChatSession>> getChatSessions(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.GET("chat/history/{thread_id}")
        retrofit2.Call<java.util.List<com.example.plateit.responses.ChatHistoryResponse>> getChatHistory(
                        @retrofit2.http.Path("thread_id") String threadId);

        @retrofit2.http.GET("users/stats/{user_id}")
        retrofit2.Call<com.example.plateit.responses.UserStatsResponse> getUserStats(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.GET("users/profile/{user_id}")
        retrofit2.Call<com.example.plateit.responses.AuthResponse> getUserProfile(
                        @retrofit2.http.Path("user_id") String userId);

        // --- Shopping List ---
        @retrofit2.http.GET("shopping_lists/{user_id}")
        retrofit2.Call<java.util.List<com.example.plateit.responses.ShoppingList>> getShoppingLists(
                        @retrofit2.http.Path("user_id") String userId);

        @retrofit2.http.GET("shopping_list/{list_id}")
        retrofit2.Call<com.example.plateit.responses.ShoppingList> getShoppingList(
                        @retrofit2.http.Path("list_id") int listId);

        @retrofit2.http.POST("shopping_lists/add")
        retrofit2.Call<com.example.plateit.responses.ShoppingList> createShoppingList(
                        @retrofit2.http.Body com.example.plateit.requests.ShoppingListCreateRequest request);

        @retrofit2.http.PUT("shopping_lists/{list_id}")
        retrofit2.Call<com.example.plateit.responses.ShoppingList> updateShoppingList(
                        @retrofit2.http.Path("list_id") int listId,
                        @retrofit2.http.Body com.example.plateit.requests.ShoppingListUpdate request);

        @retrofit2.http.DELETE("shopping_lists/{list_id}")
        retrofit2.Call<Void> deleteShoppingList(@retrofit2.http.Path("list_id") int listId);

        @retrofit2.http.POST("shopping_lists/from_recipe")
        retrofit2.Call<com.example.plateit.responses.ShoppingListFromRecipeResponse> createShoppingListFromRecipe(
                        @retrofit2.http.Body java.util.Map<String, Object> request);
}
