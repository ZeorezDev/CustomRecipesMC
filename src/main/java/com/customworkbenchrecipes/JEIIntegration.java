package com.customworkbenchrecipes;

// JEI integration is optional and uses reflection to avoid hard dependency at compile time.
public class JEIIntegration {
    public static void init() {
        try {
            // Check if JEI API is present on the classpath
            Class.forName("mezz.jei.api.IModPlugin");
            CustomWorkbenchRecipesMod.LOGGER.info("[CWR-JEI] JEI detected. Integration scaffold loaded (no runtime API calls).");
        } catch (ClassNotFoundException e) {
            CustomWorkbenchRecipesMod.LOGGER.info("[CWR-JEI] JEI not detected. Skipping JEI integration.");
        } catch (Throwable t) {
            CustomWorkbenchRecipesMod.LOGGER.error("[CWR-JEI] Initialization failed", t);
        }
    }
}
