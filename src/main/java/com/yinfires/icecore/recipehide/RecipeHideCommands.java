package com.yinfires.icecore.recipehide;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Operator-only recipe-hide administration commands. Attached under the {@code /icecore} root. */
public final class RecipeHideCommands {
    private RecipeHideCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var recipe = literal("recipe")
                .then(literal("hide")
                        .then(literal("all").executes(RecipeHideCommands::hideAll))
                        .then(literal("item").then(argument("item", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                .executes(ctx -> hideItem(ctx)))))
                .then(literal("show")
                        .then(literal("all").executes(RecipeHideCommands::showAll))
                        .then(literal("item").then(argument("item", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder))
                                .executes(ctx -> showItem(ctx)))))
                .then(literal("exempt")
                        .then(literal("on").executes(ctx -> setExempt(ctx, Boolean.TRUE)))
                        .then(literal("off").executes(ctx -> setExempt(ctx, Boolean.FALSE)))
                        .then(literal("toggle").executes(ctx -> setExempt(ctx, null))));
        root.then(recipe);
    }

    // Server-side transient view of each player's exempt toggle, for the "toggle" verb.
    private static final java.util.Set<java.util.UUID> EXEMPT = new java.util.HashSet<>();

    private static int setExempt(CommandContext<CommandSourceStack> context, Boolean value) {
        net.minecraft.server.level.ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            PlayerFeedback.showFailure(context.getSource(),
                    Component.translatable("icecore.recipe.exempt.player_only"));
            return 0;
        }
        boolean exempt = value != null ? value : !EXEMPT.contains(player.getUUID());
        if (exempt) {
            EXEMPT.add(player.getUUID());
        } else {
            EXEMPT.remove(player.getUUID());
        }
        RecipeHideService.setExempt(player, exempt);
        boolean shown = exempt;
        PlayerFeedback.showSuccess(context.getSource(),
                () -> Component.translatable(shown ? "icecore.recipe.exempt.on" : "icecore.recipe.exempt.off"), true);
        return 1;
    }

    private static int hideAll(CommandContext<CommandSourceStack> context) {
        if (!RecipeHideService.hideAll()) {
            return notReady(context);
        }
        PlayerFeedback.showSuccess(context.getSource(),
                () -> Component.translatable("icecore.recipe.hide.all"), true);
        return 1;
    }

    private static int showAll(CommandContext<CommandSourceStack> context) {
        if (!RecipeHideService.showAll()) {
            return notReady(context);
        }
        PlayerFeedback.showSuccess(context.getSource(),
                () -> Component.translatable("icecore.recipe.show.all"), true);
        return 1;
    }

    private static int hideItem(CommandContext<CommandSourceStack> context) {
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            PlayerFeedback.showFailure(context.getSource(),
                    Component.translatable("icecore.recipe.item.invalid", itemId.toString()));
            return 0;
        }
        if (!RecipeHideService.isReady()) {
            return notReady(context);
        }
        int count = RecipeHideService.hideItem(itemId);
        String key = count > 0 ? "icecore.recipe.hide.item" : "icecore.recipe.hide.item.only";
        PlayerFeedback.showSuccess(context.getSource(),
                () -> Component.translatable(key, itemId.toString()), true);
        return 1;
    }

    private static int showItem(CommandContext<CommandSourceStack> context) {
        ResourceLocation itemId = ResourceLocationArgument.getId(context, "item");
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            PlayerFeedback.showFailure(context.getSource(),
                    Component.translatable("icecore.recipe.item.invalid", itemId.toString()));
            return 0;
        }
        if (!RecipeHideService.isReady()) {
            return notReady(context);
        }
        int count = RecipeHideService.showItem(itemId);
        String key = count > 0 ? "icecore.recipe.show.item" : "icecore.recipe.show.item.only";
        PlayerFeedback.showSuccess(context.getSource(),
                () -> Component.translatable(key, itemId.toString()), true);
        return 1;
    }

    private static int notReady(CommandContext<CommandSourceStack> context) {
        PlayerFeedback.showFailure(context.getSource(),
                Component.translatable("icecore.recipe.not_ready"));
        return 0;
    }
}
