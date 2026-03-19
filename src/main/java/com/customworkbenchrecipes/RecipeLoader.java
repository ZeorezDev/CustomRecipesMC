package com.customworkbenchrecipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class RecipeLoader {
    private static final Logger LOGGER = CustomWorkbenchRecipesMod.LOGGER;
    private static MinecraftServer serverRef;

    public static void setServer(MinecraftServer server) {
        serverRef = server;
    }

    public static MinecraftServer getServer() {
        return serverRef;
    }

    public static RecipeManager getRecipeManager() {
        if (serverRef != null) {
            return serverRef.getRecipeManager();
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void scanAllRecipes() {
        RecipeManager mgr = getRecipeManager();
        if (mgr == null) {
            LOGGER.warn("[CWR] RecipeManager not available");
            return;
        }

        LOGGER.info("[CWR] === Recipe Type Scan ===");
        for (RecipeType<?> type : ForgeRegistries.RECIPE_TYPES) {
            ResourceLocation typeId = ForgeRegistries.RECIPE_TYPES.getKey(type);
            if (typeId == null) continue;
            try {
                int count = mgr.getAllRecipesFor((RecipeType) type).size();
                String namespace = typeId.getNamespace();
                if ("trajanscore".equals(namespace) || "trajanstanks".equals(namespace)) {
                    LOGGER.info("[CWR] >> Trajan's recipe type: {} ({} recipes)", typeId, count);
                } else {
                    LOGGER.debug("[CWR] Recipe type: {} ({} recipes)", typeId, count);
                }
            } catch (Exception e) {
                LOGGER.debug("[CWR] Error scanning type {}: {}", typeId, e.getMessage());
            }
        }
        LOGGER.info("[CWR] === End Recipe Type Scan ===");
    }

    /**
     * Dumps all recipes for a given namespace to the log.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void dumpRecipesForNamespace(String namespace) {
        RecipeManager mgr = getRecipeManager();
        if (mgr == null) {
            LOGGER.warn("[CWR] RecipeManager not available for dump");
            return;
        }

        LOGGER.info("[CWR] === Dumping recipes for namespace: {} ===", namespace);
        int total = 0;
        for (RecipeType<?> type : ForgeRegistries.RECIPE_TYPES) {
            ResourceLocation typeId = ForgeRegistries.RECIPE_TYPES.getKey(type);
            if (typeId == null) continue;
            try {
                List<Recipe<?>> recipes = mgr.getAllRecipesFor((RecipeType) type);
                for (Recipe<?> recipe : recipes) {
                    ResourceLocation recipeId = recipe.getId();
                    if (recipeId.getNamespace().equals(namespace)) {
                        LOGGER.info("[CWR]   {} (type: {})", recipeId, typeId);
                        total++;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("[CWR] Error dumping type {}: {}", typeId, e.getMessage());
            }
        }
        LOGGER.info("[CWR] === Total: {} recipes for namespace {} ===", total, namespace);
    }
}
