package com.customworkbenchrecipes;

import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class CompatibilityManager {
    public static final Logger LOGGER = CustomWorkbenchRecipesMod.LOGGER;

    public static boolean hasCreate;
    public static boolean hasImmEng;
    public static boolean hasJEI;
    public static boolean hasKubeJS;
    public static boolean hasCraftTweaker;
    public static boolean hasJade;
    public static boolean hasArchitectury;
    public static boolean hasImmersiveVehicles;
    public static boolean hasGeckoLib;
    public static boolean hasTrajansTanks;
    public static boolean hasTrajansCore;

    public static void initializeIntegrations() {
        ModList list = ModList.get();
        hasCreate = list.isLoaded("create");
        hasImmEng = list.isLoaded("immersiveengineering");
        hasJEI = list.isLoaded("jei");
        hasKubeJS = list.isLoaded("kubejs");
        hasCraftTweaker = list.isLoaded("crafttweaker") || list.isLoaded("ctgui");
        hasJade = list.isLoaded("jade");
        hasArchitectury = list.isLoaded("architectury");
        hasImmersiveVehicles = list.isLoaded("immersivevehicles");
        hasGeckoLib = isClassPresent("software.bernie.geckolib.GeckoLib") || isClassPresent("software.bernie.geckolib3.GeckoLib");
        hasTrajansTanks = list.isLoaded("trajanstanks");
        hasTrajansCore = list.isLoaded("trajanscore");

        LOGGER.info("[CWR] Compatibility detected -> trajansTanks=" + hasTrajansTanks +
            ", trajansCore=" + hasTrajansCore +
            ", create=" + hasCreate +
            ", jei=" + hasJEI + ", geckoLib=" + hasGeckoLib);
    }

    public static boolean isTrajansTanksAvailable() {
        return hasTrajansTanks && hasTrajansCore;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, CompatibilityManager.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
