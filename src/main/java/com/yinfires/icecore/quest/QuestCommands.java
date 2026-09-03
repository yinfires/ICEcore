package com.yinfires.icecore.quest;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Operator-only quest administration under {@code /icecore quest}. */
public final class QuestCommands {
    private QuestCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var quest = literal("quest");
        quest.then(literal("acquire").then(questArg().executes(ctx -> forEach(ctx, (player, id) -> QuestService.acquire(player, id)))
                .then(targetsArg(ctx -> forEachTarget(ctx, (player, id) -> QuestService.acquire(player, id))))));
        quest.then(literal("complete").then(questArg().executes(ctx -> forEach(ctx, (player, id) -> QuestService.complete(player, id)))
                .then(targetsArg(ctx -> forEachTarget(ctx, (player, id) -> QuestService.complete(player, id))))));
        quest.then(literal("mark").then(questArg().executes(ctx -> forEach(ctx, (player, id) -> QuestService.setMarked(player, id)))
                .then(targetsArg(ctx -> forEachTarget(ctx, (player, id) -> QuestService.setMarked(player, id))))));
        quest.then(literal("unmark").executes(ctx -> {
            ServerPlayer self = ctx.getSource().getPlayerOrException();
            return QuestService.setMarked(self, null) ? 1 : 0;
        }).then(argument("targets", EntityArgument.players()).executes(ctx -> {
            int changed = 0;
            for (ServerPlayer player : EntityArgument.getPlayers(ctx, "targets")) {
                if (QuestService.setMarked(player, null)) {
                    changed++;
                }
            }
            return changed;
        })));
        quest.then(literal("progress").then(questArg()
                .then(argument("index", IntegerArgumentType.integer(0))
                        .then(argument("count", IntegerArgumentType.integer(0)).executes(QuestCommands::progressSelf)))));
        quest.then(literal("reset").then(questArg().executes(ctx -> forEach(ctx, (player, id) -> QuestService.reset(player, id)))
                .then(targetsArg(ctx -> forEachTarget(ctx, (player, id) -> QuestService.reset(player, id))))));
        root.then(quest);
    }

    private interface QuestAction {
        boolean run(ServerPlayer player, ResourceLocation questId);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> questArg() {
        return argument("quest", ResourceLocationArgument.id()).suggests((ctx, builder) ->
                SharedSuggestionProvider.suggestResource(QuestDefinitionManager.INSTANCE.ids(), builder));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, net.minecraft.commands.arguments.selector.EntitySelector> targetsArg(
            com.mojang.brigadier.Command<CommandSourceStack> command) {
        return argument("targets", EntityArgument.players()).executes(command);
    }

    private static int forEach(CommandContext<CommandSourceStack> ctx, QuestAction action) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        return action.run(self, id) ? 1 : 0;
    }

    private static int forEachTarget(CommandContext<CommandSourceStack> ctx, QuestAction action) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        int success = 0;
        for (ServerPlayer player : targets) {
            if (action.run(player, id)) {
                success++;
            }
        }
        return success;
    }

    private static int progressSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer self = ctx.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "quest");
        int index = IntegerArgumentType.getInteger(ctx, "index");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        return QuestService.setObjectiveProgress(self, id, index, count) ? 1 : 0;
    }

    static List<ResourceLocation> suggestionIds() {
        return List.copyOf(QuestDefinitionManager.INSTANCE.ids());
    }
}
