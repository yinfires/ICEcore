package com.yinfires.icecore.tutorial;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Operator-only tutorial administration under {@code /icecore tutorial}. */
public final class TutorialCommands {
    private TutorialCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var tutorial = literal("tutorial");
        tutorial.then(literal("unlock").then(tutorialArg()
                .executes(ctx -> forEach(ctx, TutorialService::unlock))
                .then(argument("targets", EntityArgument.players())
                        .executes(ctx -> forEachTarget(ctx, TutorialService::unlock)))));
        tutorial.then(literal("read").then(tutorialArg()
                .executes(ctx -> forEach(ctx, TutorialService::markRead))
                .then(argument("targets", EntityArgument.players())
                        .executes(ctx -> forEachTarget(ctx, TutorialService::markRead)))));
        tutorial.then(literal("reset").then(tutorialArg()
                .executes(ctx -> forEach(ctx, TutorialService::reset))
                .then(argument("targets", EntityArgument.players())
                        .executes(ctx -> forEachTarget(ctx, TutorialService::reset)))));
        root.then(tutorial);
    }

    private interface TutorialAction {
        boolean run(ServerPlayer player, ResourceLocation tutorialId);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> tutorialArg() {
        return argument("tutorial", ResourceLocationArgument.id()).suggests((ctx, builder) ->
                SharedSuggestionProvider.suggestResource(TutorialDefinitionManager.INSTANCE.ids(), builder));
    }

    private static int forEach(CommandContext<CommandSourceStack> ctx, TutorialAction action) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "tutorial");
        return action.run(self, id) ? 1 : 0;
    }

    private static int forEachTarget(CommandContext<CommandSourceStack> ctx, TutorialAction action) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "tutorial");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int success = 0;
        for (ServerPlayer player : targets) {
            if (action.run(player, id)) {
                success++;
            }
        }
        return success;
    }
}
