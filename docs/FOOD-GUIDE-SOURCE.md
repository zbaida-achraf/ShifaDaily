# Food guide and recipe data — sources and limits

## Leaflet

- **Title:** «من أجل نظام غذائي متوازن» (*For a balanced diet*), patient-education leaflet bearing the logo of
  **Sothema** (Moroccan pharmaceutical company); footnote mentions collaboration with a hospital department in Rabat
  (not fully legible).
- **How obtained:** photo supplied by the project owner on 19 Oct 2026.
- **Transcription:** read from the photo (rotated and enlarged) on 19 Oct 2026. **Not independently verified**
  against an official digital copy or a dietitian.
- **One item omitted:** a starch equivalence of "2 tablespoons of [potato preparation]" was not legible enough.

### What is used in the app

| Leaflet section | App use |
|---|---|
| Green "consume as desired" pictures (vegetables, fish, water, unsweetened tea/coffee) | Tier **As desired** (`pictured = true`) |
| Yellow "limited consumption" pictures (pasta, oil, chicken, cheese, milk, carrots, peas, fruit, nuts/dried fruit) | Tier **Limited** |
| Red "occasionally, small amounts" pictures (cakes, sugar, honey, jam, sweets, ice cream, juices, sodas, dates) | Tier **Occasionally**; never used in curated recipes and not offered as additions |
| Text: starch & legume equivalences, red meat / egg yolk note, bread | Tier **Limited** with `pictured = false` ("inferred from the leaflet text") |
| Meal pattern (breakfast, lunch/dinner, snack), useful information, portion equivalents, 4 tips | Shown verbatim-in-meaning (translated/paraphrased) in **Food guide**, with attribution |

Herbs, spices and salt are not covered by the leaflet and are labelled "Not in guide". Vegetables not pictured
(spinach, broccoli, celery, garlic, turnip) are placed in "As desired" based on the leaflet's general statement
about vegetables and are labelled as inferred.

### Numbers

The only numbers shown are the leaflet's own portion equivalents (≈25 g carbohydrate, written "sugar" in the
leaflet) and its example 100 g grapes ≈ 18 g sugar vs 100 g raisins ≈ 74 g, all **attributed to the leaflet**.
ShifaDaily does **not** compute calories, carbohydrates or any nutrient for recipes, because no validated
food-composition database was integrated. Before adding nutrient values, use a documented source (e.g. a national
food-composition table or USDA FoodData Central), record its version/date, and review with a dietitian.

## Recipes

- 19 recipes written for ShifaDaily (no copying from websites or books), mostly Moroccan home dishes adapted to the
  leaflet's advice: vegetables first, measured starch/legume portions, oven/steam/tagine with ~1 tbsp oil for 4,
  no added sugar.
- Quantities are ordinary household amounts; cooking times are typical, not laboratory-verified.
- Allergen tags follow the 14 major allergen groups at ingredient level. Brand, hidden-ingredient and
  cross-contamination checks are impossible; the app says so.
- No photos are used for recipes, so no image can be mistaken for the actual dish.
- No external recipe API is used (no cost, no data sent, fully offline).
