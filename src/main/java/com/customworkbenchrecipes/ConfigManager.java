package com.customworkbenchrecipes;

import com.google.gson.*;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    public static List<StoredOverride> overrides = new ArrayList<>();
    public static Map<String, RecipeConfig> recipeIndex = new HashMap<>();

    private static final String DEFAULT_CONFIG = "{\n" +
            "  \"recipes\": [\n" +
            "    {\n" +
            "      \"recipe_id\": \"trajanscore:engine_fabricator/heavy_engine\",\n" +
            "      \"recipe_type\": \"trajanscore:engine_fabricator\",\n" +
            "      \"action\": \"replace\",\n" +
            "      \"ingredients\": [\n" +
            "        { \"item\": \"minecraft:redstone_block\" },\n" +
            "        { \"item\": \"minecraft:iron_block\" },\n" +
            "        { \"item\": \"minecraft:redstone_block\" },\n" +
            "        { \"item\": \"minecraft:piston\" },\n" +
            "        { \"item\": \"minecraft:piston\" },\n" +
            "        { \"item\": \"minecraft:piston\" },\n" +
            "        { \"item\": \"minecraft:iron_ingot\" },\n" +
            "        { \"item\": \"minecraft:iron_ingot\" },\n" +
            "        { \"item\": \"minecraft:iron_ingot\" }\n" +
            "      ],\n" +
            "      \"output\": { \"item\": \"trajanscore:heavy_engine\", \"count\": 1 }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    public static void loadConfig() {
        Path path = Paths.get("config/custom_workbench_recipes.json");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, DEFAULT_CONFIG.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                CustomWorkbenchRecipesMod.LOGGER.info("[CWR] Auto-created default config at: " + path);
            } catch (Exception e) {
                CustomWorkbenchRecipesMod.LOGGER.error("[CWR] Failed to create default config", e);
            }
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(IngredientConfig.class, new IngredientDeserializer())
                    .create();
            ConfigData data = gson.fromJson(reader, ConfigData.class);
            overrides.clear();
            recipeIndex.clear();
            if (data != null && data.recipes != null) {
                for (RecipeConfig rc : data.recipes) {
                    if (rc == null || rc.recipe_id == null) {
                        CustomWorkbenchRecipesMod.LOGGER.warn("[CWR] Invalid recipe: missing recipe_id");
                        continue;
                    }
                    if (rc.action == null) rc.action = "replace";
                    if ("remove".equals(rc.action)) {
                        // Remove action only needs recipe_id
                        StoredOverride so = new StoredOverride(rc);
                        overrides.add(so);
                        recipeIndex.put(rc.recipe_id, rc);
                        continue;
                    }
                    boolean hasShapedData = rc.isShaped();
                    boolean hasFlatIngredients = rc.ingredients != null && !rc.ingredients.isEmpty();
                    if (rc.recipe_type == null || (!hasShapedData && !hasFlatIngredients) || rc.output == null) {
                        CustomWorkbenchRecipesMod.LOGGER.warn("[CWR] Invalid recipe definition: " + rc.recipe_id);
                        continue;
                    }
                    StoredOverride so = new StoredOverride(rc);
                    overrides.add(so);
                    recipeIndex.put(rc.recipe_id, rc);
                }
            }
            CustomWorkbenchRecipesMod.LOGGER.info("[CWR] Loaded overrides: " + overrides.size());
        } catch (Exception e) {
            CustomWorkbenchRecipesMod.LOGGER.error("[CWR] Failed to parse config", e);
        }
    }

    public static void reload() {
        loadConfig();
        RecipeOverrideManager.applyOverrides();
    }

    // Data holder classes
    public static class ConfigData {
        List<RecipeConfig> recipes;
    }

    public static class RecipeConfig {
        public String recipe_id;
        public String recipe_type;      // e.g., "trajanscore:crafter", "trajanscore:engine_fabricator", "minecraft:crafting_shaped"
        public String action;           // "replace", "remove", or "add" (default: "replace")
        public List<IngredientConfig> ingredients;
        public OutputConfig output;

        // Shaped recipe support (minecraft:crafting_shaped)
        public List<String> pattern;
        public Map<String, IngredientConfig> key;

        // Legacy support
        public String workbench_type;
        public ResultConfig result;

        public boolean isShaped() {
            return pattern != null && key != null && !pattern.isEmpty() && !key.isEmpty();
        }

        public OutputConfig getOutput() {
            if (output != null) return output;
            // Legacy fallback
            if (result != null) {
                OutputConfig o = new OutputConfig();
                o.item = result.item;
                o.count = result.count;
                return o;
            }
            return null;
        }
    }

    public static class IngredientConfig {
        public String item;
        public String tag;
        public int count = 1;
    }

    public static class OutputConfig {
        public String item;
        public int count = 1;
    }

    // Legacy support
    public static class ResultConfig {
        public String item;
        public int count = 1;
    }

    public static class StoredOverride {
        public RecipeConfig rc;
        public StoredOverride(RecipeConfig rc) { this.rc = rc; }
    }

    /**
     * Custom deserializer to handle both plain string ingredients ("minecraft:iron_block")
     * and object ingredients ({"item": "minecraft:iron_block", "count": 2})
     */
    private static class IngredientDeserializer implements JsonDeserializer<IngredientConfig> {
        @Override
        public IngredientConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            IngredientConfig ing = new IngredientConfig();
            if (json.isJsonPrimitive()) {
                // Plain string format: "minecraft:iron_block"
                ing.item = json.getAsString();
                ing.count = 1;
            } else if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                if (obj.has("item")) ing.item = obj.get("item").getAsString();
                if (obj.has("tag")) ing.tag = obj.get("tag").getAsString();
                if (obj.has("count")) ing.count = obj.get("count").getAsInt();
                else ing.count = 1;
            }
            return ing;
        }
    }
}
