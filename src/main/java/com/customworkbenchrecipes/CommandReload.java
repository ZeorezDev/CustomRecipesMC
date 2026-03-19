package com.customworkbenchrecipes;

import com.customworkbenchrecipes.compat.TrajansTanksCompat;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CommandReload {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("cwr")
                .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    RecipeLoader.setServer(ctx.getSource().getServer());
                    ConfigManager.reload();
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[CWR] Reloaded " + ConfigManager.overrides.size() + " recipe overrides"), true);
                    return 1;
                }))
                .then(Commands.literal("list").executes(ctx -> {
                    if (ConfigManager.overrides.isEmpty()) {
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("[CWR] No active recipe overrides"), false);
                        return 0;
                    }
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("[CWR] Active overrides (" + ConfigManager.overrides.size() + "):"), false);
                    for (ConfigManager.StoredOverride so : ConfigManager.overrides) {
                        ConfigManager.RecipeConfig rc = so.rc;
                        String action = rc.action != null ? rc.action : "replace";
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("  " + action + ": " + rc.recipe_id + " [" + (rc.recipe_type != null ? rc.recipe_type : "n/a") + "]"), false);
                    }
                    return ConfigManager.overrides.size();
                }))
                .then(Commands.literal("dump")
                    .then(Commands.argument("namespace", StringArgumentType.string()).executes(ctx -> {
                        String namespace = StringArgumentType.getString(ctx, "namespace");
                        RecipeLoader.setServer(ctx.getSource().getServer());
                        RecipeLoader.dumpRecipesForNamespace(namespace);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("[CWR] Dumped recipes for namespace '" + namespace + "' to log file"), true);
                        return 1;
                    }))
                    .executes(ctx -> {
                        // No namespace specified - dump Trajan's recipes
                        RecipeLoader.setServer(ctx.getSource().getServer());
                        int count = TrajansTanksCompat.dumpAllTrajanRecipes();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("[CWR] Dumped " + count + " Trajan's recipes to log file"), true);
                        return 1;
                    })
                )
        );
    }
}
