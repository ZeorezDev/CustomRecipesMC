package com.customworkbenchrecipes.compat;

import com.customworkbenchrecipes.CompatibilityManager;
import com.customworkbenchrecipes.CustomWorkbenchRecipesMod;
import com.customworkbenchrecipes.RecipeLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Trajan's Tanks / Trajan's Core compatibility handler.
 *
 * Known custom recipe types (from trajanscore):
 * - trajanscore:crafter          (Tank Crafter - final tank assembly)
 * - trajanscore:engine_fabricator (Engine Fabricator)
 * - trajanscore:plating_press    (Plating Press)
 * - trajanscore:shell_workbench  (Shell Workbench)
 * - trajanscore:steel_manufacturer (Steel Manufacturer)
 * - trajanscore:turret_factory   (Turret Factory)
 *
 * All use the same JSON format:
 * { "type": "trajanscore:<type>", "ingredients": [{"item": "..."}], "output": {"item": "..."} }
 *
 * Blueprint recipes use standard minecraft:crafting_shaped.
 */
public final class TrajansTanksCompat {
    private static final Logger LOGGER = CustomWorkbenchRecipesMod.LOGGER;
    private static final List<ResourceLocation> TRAJAN_RECIPE_TYPES = new ArrayList<>();

    public static void init() {
        if (!CompatibilityManager.isTrajansTanksAvailable()) {
            LOGGER.info("[CWR] Trajan's Tanks not detected, skipping TrajansTanksCompat init");
            return;
        }

        TRAJAN_RECIPE_TYPES.clear();
        for (RecipeType<?> type : ForgeRegistries.RECIPE_TYPES) {
            ResourceLocation id = ForgeRegistries.RECIPE_TYPES.getKey(type);
            if (id != null && ("trajanscore".equals(id.getNamespace()) || "trajanstanks".equals(id.getNamespace()))) {
                TRAJAN_RECIPE_TYPES.add(id);
                LOGGER.info("[CWR] Found Trajan's recipe type: {}", id);
            }
        }
        LOGGER.info("[CWR] TrajansTanksCompat initialized with {} recipe types", TRAJAN_RECIPE_TYPES.size());
    }

    public static List<ResourceLocation> getTrajanRecipeTypes() {
        return Collections.unmodifiableList(TRAJAN_RECIPE_TYPES);
    }

    /**
     * Dumps all Trajan's recipes to the log for discovery purposes.
     */
    public static int dumpAllTrajanRecipes() {
        RecipeManager mgr = RecipeLoader.getRecipeManager();
        if (mgr == null) {
            LOGGER.warn("[CWR] RecipeManager not available for Trajan's dump");
            return 0;
        }

        int total = 0;
        LOGGER.info("[CWR] === Trajan's Tanks/Core Recipe Dump ===");

        // Dump trajanscore recipes
        total += dumpNamespace(mgr, "trajanscore");
        // Dump trajanstanks recipes
        total += dumpNamespace(mgr, "trajanstanks");

        LOGGER.info("[CWR] === Total: {} Trajan's recipes ===", total);
        return total;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int dumpNamespace(RecipeManager mgr, String namespace) {
        int count = 0;
        for (RecipeType<?> type : ForgeRegistries.RECIPE_TYPES) {
            ResourceLocation typeId = ForgeRegistries.RECIPE_TYPES.getKey(type);
            if (typeId == null) continue;

            try {
                List<Recipe<?>> recipes = mgr.getAllRecipesFor((RecipeType) type);
                for (Recipe<?> recipe : recipes) {
                    ResourceLocation recipeId = recipe.getId();
                    if (recipeId.getNamespace().equals(namespace)) {
                        LOGGER.info("[CWR]   {} [type: {}]", recipeId, typeId);
                        count++;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[CWR] Error dumping type {}: {}", typeId, e.getMessage());
            }
        }
        return count;
    }
}
