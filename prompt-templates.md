# FriendlyMeals – Prompt Templates

Below you find all the [prompt templates](https://firebase.google.com/docs/ai-logic/server-prompt-templates/get-started) that you need to create in your Firebase project before you can run FriendlyMeals.

Please note that the model names below might not be the most up to date ones. Refer to the [Firebase AI Logic documentation](https://firebase.google.com/docs/ai-logic/models) to check the latest available models.

Don't forget to test your templates in the Firebase console before you use them in your apps. [Read the docs](https://firebase.google.com/docs/ai-logic/server-prompt-templates/get-started?api=dev#test-template) to learn how to test your templates properly.

## Generate Ingredients Template

This prompt template is used when the user takes a photo of their fridge or cupboard to identify the ingredients in it.

**Configuration (frontmatter)**

    model: "gemini-3.5-flash-lite"

**Prompt and (optional) system instructions**

    Please analyze this image: {{media type="mimeType" data="imageData"}}, and list all visible food ingredients. 
    Output ONLY a comma-separated list of ingredients. Do not include any introductory text, headers, or concluding remarks. 
    Provide the raw list only. Be specific with measurements where possible.

## Generate Recipe Template

This prompt template is used when the user generates a recipe from a list of ingredients and notes.

**Configuration (frontmatter)**

    model: "gemini-3.5-flash-lite"
    input:
      schema:
        ingredients: "string, the ingredients list"
        notes?: "string, cuisine and dietary notes"
    output:
      format: json
      schema:
        title: string
        instructions: string
        ingredients(array, ingredients for recipe): string
        prepTime: string
        cookTime: string
        servings: string
        tags(array, relevant tags for recipe): string

**Prompt and (optional) system instructions**

    {{role "user"}}
    Create a detailed recipe based on these ingredients: {{ingredients}}.
            
    Format requirements:
     - 'instructions': Provide the cooking steps as a clear list of instructions separated by newlines. Use bold formatting on the step numbers. Use Markdown.
     - 'ingredients': List all necessary items, including quantities.
     - 'prepTime', 'cookTime', 'servings': Short strings (e.g., "15 mins").
     - 'tags': Generate a list of 3-5 relevant category tags (e.g., "Healthy", "Vegan", "Gluten-Free", "Dessert", "Quick").

    {{#if notes}}
    IMPORTANT CUISINE AND DIETARY NOTES: {{notes}}
    {{/if}}

## Generate Recipe Photo Template

This prompt template is used when the user generates a recipe photo from the recipe title.

**Configuration (frontmatter)**

    model: "gemini-3.1-flash-image"
    input:
      schema:
        recipeTitle: "string"

**Prompt and (optional) system instructions**

    A professional food photography shot of this recipe: {{recipeTitle}}. 
    Style: High-end food photography, restaurant-quality plating, soft natural lighting, on a clean background, showing the complete plated dish.

## Scan Meal Template

This prompt template is used when the user scans a meal to extract the nutritional facts.

**Configuration (frontmatter)**

    model: "gemini-3.5-flash-lite"
    output:
      format: json
      schema:
        protein: string
        fat: string
        carbs: string
        sugar: string
        ingredients(array, ingredients in the meal): string

**Prompt and (optional) system instructions**

    Analyze this image of a meal: {{media type="mimeType" data="imageData"}}, and estimate the nutritional content.
    Return the result in JSON format matching the schema:
    - protein, fat, carbs, sugar (strings with units, e.g., '20g')
    - ingredients (list of strings)

## Find Stores Template

This prompt template is used when the user needs to find stores that sell the ingredients they need.

**Configuration (frontmatter)**

    model: "gemini-3.5-flash-lite"
    tools:
      - googleMaps
    input:
      schema:
        ingredients: "string, the ingredients in the shopping list"
        dayOfWeek: "string, current day of the week"
        currentTime: "string, current time of the day"

**Prompt and (optional) system instructions**

    What are the nearest grocery stores or markets near me that stock these ingredients: {{ingredients}}? 
    For each place, tell me their business hours, if it's open right now on {{dayOfWeek}} at {{currentTime}}, and if it's closing in less than 30 minutes. 
    Tell me what the parking situation is like at each place: is there a dedicated lot or should I look for street parking? 
    Tell the Map URL so I can open it in Google Maps. 
    Format your response strictly as a JSON object with a "stores" array containing store objects. 
    Each store object must have these EXACT keys: "name": string, "address": string, "distance": string, "openNow": boolean, "closingSoon": boolean, "hasParking": boolean, "parkingDetails": string, "mapUrl": string. 
    Do NOT include markdown code block formatting (like ```json). Output only the raw JSON string.

