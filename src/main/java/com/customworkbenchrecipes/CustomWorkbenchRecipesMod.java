package com.customworkbenchrecipes;

import com.customworkbenchrecipes.compat.TrajansTanksCompat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod("recipemod")
public class CustomWorkbenchRecipesMod {
    public static final Logger LOGGER = LogManager.getLogger("CWR");

    public CustomWorkbenchRecipesMod() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent event) {
        LOGGER.info("[CWR] Mod setup starting");
        ConfigManager.loadConfig();
        CompatibilityManager.initializeIntegrations();
        JEIIntegration.init();
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = "recipemod")
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            LOGGER.info("[CWR] Server started - applying recipe overrides");
            RecipeLoader.setServer(event.getServer());
            TrajansTanksCompat.init();
            RecipeLoader.scanAllRecipes();
            RecipeOverrideManager.applyOverrides();
        }
    }
}
