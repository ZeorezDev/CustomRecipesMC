package com.customworkbenchrecipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeOverrideManager {
    private static final Logger LOGGER = CustomWorkbenchRecipesMod.LOGGER;

    public static void applyOverrides() {
        List<ConfigManager.StoredOverride> overrideList = ConfigManager.overrides;
        if (overrideList == null || overrideList.isEmpty()) {
            LOGGER.info("[CWR] No recipe overrides to apply");
            return;
        }

        RecipeManager mgr = RecipeLoader.getRecipeManager();
        if (mgr == null) {
            LOGGER.warn("[CWR] RecipeManager not available - cannot apply overrides");
            return;
        }

        // Get internal recipe map via reflection (only the recipes type-map is needed)
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipeMaps = getRecipeMap(mgr);

        if (recipeMaps == null) {
            LOGGER.error("[CWR] Failed to access RecipeManager internals via reflection");
            return;
        }

        // Make maps mutable (they are ImmutableMaps by default)
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> mutableRecipeMaps = new HashMap<>();
        for (Map.Entry<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> entry : recipeMaps.entrySet()) {
            mutableRecipeMaps.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        setRecipeMap(mgr, mutableRecipeMaps);

        int applied = 0;
        for (ConfigManager.StoredOverride so : overrideList) {
            if (so == null || so.rc == null) continue;
            ConfigManager.RecipeConfig rc = so.rc;
            ResourceLocation recipeId = new ResourceLocation(rc.recipe_id);

            try {
                if ("remove".equals(rc.action)) {
                    if (removeRecipe(mutableRecipeMaps, recipeId)) {
                        LOGGER.info("[CWR] Removed recipe: {}", recipeId);
                        applied++;
                    } else {
                        LOGGER.warn("[CWR] Recipe not found for removal: {}", recipeId);
                    }
                } else {
                    // "replace" or "add"
                    Recipe<?> newRecipe = buildRecipe(rc, recipeId);
                    if (newRecipe != null) {
                        removeRecipe(mutableRecipeMaps, recipeId);
                        addRecipe(mutableRecipeMaps, recipeId, newRecipe);
                        LOGGER.info("[CWR] {} recipe: {} (type: {})", rc.action, recipeId, rc.recipe_type);
                        applied++;
                    } else {
                        LOGGER.error("[CWR] Failed to build recipe: {}", recipeId);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[CWR] Error applying override for recipe: {}", recipeId, e);
            }
        }
        LOGGER.info("[CWR] Applied {}/{} recipe overrides", applied, overrideList.size());
    }

    /**
     * Builds a Recipe instance from config by constructing a JsonObject and
     * passing it to the appropriate RecipeSerializer.fromJson().
     */
    @SuppressWarnings("unchecked")
    private static Recipe<?> buildRecipe(ConfigManager.RecipeConfig rc, ResourceLocation recipeId) {
        ResourceLocation serializerId = new ResourceLocation(rc.recipe_type);

        // In Forge 1.20.1, recipe type and serializer often share the same ID
        RecipeSerializer<?> serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(serializerId);
        if (serializer == null) {
            LOGGER.error("[CWR] No serializer found for type: {}", serializerId);
            LOGGER.info("[CWR] Available serializers:");
            for (ResourceLocation id : ForgeRegistries.RECIPE_SERIALIZERS.getKeys()) {
                if (id.getNamespace().equals(serializerId.getNamespace())) {
                    LOGGER.info("[CWR]   - {}", id);
                }
            }
            return null;
        }

        JsonObject json = new JsonObject();
        json.addProperty("type", rc.recipe_type);

        if (rc.isShaped()) {
            // Build JSON for minecraft:crafting_shaped format:
            // { "type": "minecraft:crafting_shaped", "pattern": [...], "key": {...}, "result": {...} }
            JsonArray patternArray = new JsonArray();
            for (String row : rc.pattern) {
                patternArray.add(row);
            }
            json.add("pattern", patternArray);

            JsonObject keyObj = new JsonObject();
            for (Map.Entry<String, ConfigManager.IngredientConfig> entry : rc.key.entrySet()) {
                JsonObject ingObj = new JsonObject();
                ConfigManager.IngredientConfig ing = entry.getValue();
                if (ing.item != null) {
                    ingObj.addProperty("item", ing.item);
                } else if (ing.tag != null) {
                    ingObj.addProperty("tag", ing.tag);
                }
                keyObj.add(entry.getKey(), ingObj);
            }
            json.add("key", keyObj);

            // Shaped recipes use "result" not "output"
            ConfigManager.OutputConfig output = rc.getOutput();
            if (output != null) {
                JsonObject resultObj = new JsonObject();
                resultObj.addProperty("item", output.item);
                if (output.count > 1) {
                    resultObj.addProperty("count", output.count);
                }
                json.add("result", resultObj);
            }
        } else {
            // Build JSON matching the Trajan's Core recipe format:
            // { "type": "...", "ingredients": [ {"item": "..."} ], "output": {"item": "..."} }
            JsonArray ingredientsArray = new JsonArray();
            for (ConfigManager.IngredientConfig ing : rc.ingredients) {
                JsonObject ingObj = new JsonObject();
                if (ing.item != null) {
                    ingObj.addProperty("item", ing.item);
                } else if (ing.tag != null) {
                    ingObj.addProperty("tag", ing.tag);
                }
                ingredientsArray.add(ingObj);
            }
            json.add("ingredients", ingredientsArray);

            // Build output
            ConfigManager.OutputConfig output = rc.getOutput();
            if (output != null) {
                JsonObject outputObj = new JsonObject();
                outputObj.addProperty("item", output.item);
                if (output.count > 1) {
                    outputObj.addProperty("count", output.count);
                }
                json.add("output", outputObj);
            }
        }

        try {
            return serializer.fromJson(recipeId, json);
        } catch (Exception e) {
            LOGGER.error("[CWR] Serializer.fromJson failed for {}: {}", recipeId, e.getMessage());
            return null;
        }
    }

    private static boolean removeRecipe(
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipeMaps,
            ResourceLocation recipeId) {
        boolean removed = false;
        for (Map<ResourceLocation, Recipe<?>> typeMap : recipeMaps.values()) {
            if (typeMap.remove(recipeId) != null) {
                removed = true;
            }
        }
        return removed;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addRecipe(
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipeMaps,
            ResourceLocation recipeId,
            Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();
        recipeMaps.computeIfAbsent(type, k -> new HashMap<>()).put(recipeId, recipe);
    }

    // --- Reflection helpers ---

    @SuppressWarnings("unchecked")
    private static Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> getRecipeMap(RecipeManager mgr) {
        try {
            // f_44007_ is the SRG name for "recipes" field in RecipeManager
            Field field = ObfuscationReflectionHelper.findField(RecipeManager.class, "f_44007_");
            return (Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>>) field.get(mgr);
        } catch (Exception e) {
            LOGGER.error("[CWR] Reflection failed for recipes field", e);
            return null;
        }
    }

    private static void setRecipeMap(RecipeManager mgr, Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> newMap) {
        try {
            Field field = ObfuscationReflectionHelper.findField(RecipeManager.class, "f_44007_");
            field.set(mgr, newMap);
        } catch (Exception e) {
            LOGGER.error("[CWR] Failed to set recipes field", e);
        }
    }

}
