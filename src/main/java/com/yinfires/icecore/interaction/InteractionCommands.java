package com.yinfires.icecore.interaction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.yinfires.icecore.building.BuildingStructure;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;
import static net.minecraft.commands.Commands.*;

public final class InteractionCommands {
    private static final InteractionManager MANAGER = InteractionManager.INSTANCE;
    @FunctionalInterface private interface Action { Component run() throws Exception; }
    private static int run(CommandContext<CommandSourceStack> context, Action action) {
        try { PlayerFeedback.show(context.getSource(), action.run()); return 1; }
        catch (Exception error) {
            PlayerFeedback.showFailure(context.getSource(), Component.translatable("icecore.interaction.failed", error.getMessage())); return 0;
        }
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = literal("interaction").requires(source -> source.hasPermission(2));
        for (String list : List.of("blacklist", "whitelist")) {
            boolean allow = list.equals("whitelist"); var listNode = literal(list);
            for (String operation : List.of("add", "remove")) {
                boolean add = operation.equals("add"); var operationNode = literal(operation);
                for (String kind : List.of("block", "tag", "mod", "modtag")) {
                    var value = argument("value", ResourceLocationArgument.id()).suggests((context, builder) -> {
                        if (!add) return SharedSuggestionProvider.suggest(MANAGER.config().removalSuggestions(allow, kind, null), builder);
                        java.util.stream.Stream<String> choices = switch (kind) {
                            case "block" -> BuiltInRegistries.BLOCK.keySet().stream().map(Object::toString);
                            case "tag" -> BuiltInRegistries.BLOCK.getTagNames().map(tag -> tag.location().toString());
                            default -> BuiltInRegistries.BLOCK.keySet().stream().map(id -> id.getNamespace()).distinct();
                        }; return SharedSuggestionProvider.suggest(choices, builder);
                    });
                    if (kind.equals("modtag")) value.then(argument("tag", ResourceLocationArgument.id())
                            .suggests((context, builder) -> SharedSuggestionProvider.suggest(add
                                    ? BuiltInRegistries.BLOCK.getTagNames().map(tag -> tag.location().toString())
                                    : MANAGER.config().removalSuggestions(allow, kind, ResourceLocationArgument.getId(context, "value").getPath()), builder))
                            .executes(context -> edit(context, allow, add, kind)));
                    else value.executes(context -> edit(context, allow, add, kind));
                    operationNode.then(literal(kind).then(value));
                }
                listNode.then(operationNode);
            }
            listNode.then(literal("list").executes(context -> list(context, allow, 1))
                    .then(argument("page", IntegerArgumentType.integer(1)).executes(context -> list(context, allow, IntegerArgumentType.getInteger(context, "page")))));
            root.then(listNode);
        }
        var exceptions = literal("exception");
        exceptions.then(target("add", context -> exception(context, true, false), context -> exception(context, true, true)));
        exceptions.then(target("remove", context -> exception(context, false, false), context -> exception(context, false, true)));
        exceptions.then(literal("list").executes(context -> exceptions(context, 1)).then(argument("page", IntegerArgumentType.integer(1))
                .executes(context -> exceptions(context, IntegerArgumentType.getInteger(context, "page")))));
        root.then(exceptions);
        root.then(target("inspect", context -> inspect(context, false), context -> inspect(context, true)));
        root.then(literal("reload").executes(context -> run(context, () -> { MANAGER.reload(); return message("reloaded"); })));
        dispatcher.register(literal("icecore").then(root));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> target(String name,
            com.mojang.brigadier.Command<CommandSourceStack> pointed, com.mojang.brigadier.Command<CommandSourceStack> explicit) {
        return literal(name).executes(pointed).then(argument("pos", BlockPosArgument.blockPos()).executes(explicit)
                .then(argument("dimension", DimensionArgument.dimension()).executes(explicit)));
    }
    private static int edit(CommandContext<CommandSourceStack> context, boolean allow, boolean add, String kind) {
        return run(context, () -> {
            var value = ResourceLocationArgument.getId(context, "value");
            String text = kind.equals("mod") || kind.equals("modtag") ? value.getPath() : value.toString();
            // ResourceLocationArgument accepts a bare namespace token as minecraft:<token>.
            if ((kind.equals("mod") || kind.equals("modtag")) && !value.getNamespace().equals("minecraft"))
                throw new IllegalArgumentException("Expected bare namespace");
            InteractionConfig.Rule rule = switch (kind) {
                case "block" -> new InteractionConfig.Rule(text, null, null);
                case "tag" -> new InteractionConfig.Rule(null, null, text);
                case "mod" -> new InteractionConfig.Rule(null, text, null);
                default -> new InteractionConfig.Rule(null, text, ResourceLocationArgument.getId(context, "tag").toString());
            };
            boolean changed = MANAGER.edit(allow, add, rule);
            return message(changed ? "changed" : "unchanged");
        });
    }
    private record Target(ServerLevel level, BlockPos position) {}
    private static Target target(CommandContext<CommandSourceStack> context, boolean explicit) throws Exception {
        CommandSourceStack source = context.getSource(); ServerLevel level = source.getLevel(); BlockPos pos;
        if (explicit) {
            boolean hasDimension;
            try { context.getArgument("dimension", net.minecraft.resources.ResourceLocation.class); hasDimension = true; }
            catch (IllegalArgumentException ignored) { hasDimension = false; }
            if (hasDimension) level = DimensionArgument.getDimension(context, "dimension");
            pos = BlockPosArgument.getBlockPos(context, "pos");
        } else {
            var player = source.getPlayerOrException(); double reach = player.getBlockReach();
            Vec3 eye = player.getEyePosition(); Vec3 end = eye.add(player.getViewVector(1).scale(reach));
            BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.BLOCK || !player.canReach(hit.getBlockPos(), 0)) throw new IllegalArgumentException(message("no_target").getString());
            pos = hit.getBlockPos();
        }
        if (!level.hasChunkAt(pos) || !level.isInWorldBounds(pos)) throw new IllegalArgumentException(message("unloaded").getString());
        if (level.getBlockState(pos).isAir()) throw new IllegalArgumentException(message("no_target").getString());
        BlockPos primary = BuildingStructure.primaryPosition(level.getBlockState(pos), pos);
        if (!level.hasChunkAt(primary)) throw new IllegalArgumentException(message("unloaded").getString());
        return new Target(level, primary);
    }
    private static int exception(CommandContext<CommandSourceStack> context, boolean add, boolean explicit) {
        return run(context, () -> {
            Target target = target(context, explicit);
            String block = add ? BuiltInRegistries.BLOCK.getKey(target.level().getBlockState(target.position()).getBlock()).toString() : null;
            return message(MANAGER.exception(InteractionSnapshot.location(target.level(), target.position()), block) ? "changed" : "unchanged");
        });
    }
    private static int list(CommandContext<CommandSourceStack> context, boolean allow, int page) {
        return run(context, () -> page((allow ? MANAGER.config().whitelist() : MANAGER.config().blacklist()).stream().map(Object::toString).toList(), page));
    }
    private static int exceptions(CommandContext<CommandSourceStack> context, int page) {
        return run(context, () -> page(MANAGER.snapshot().exceptions().entries().entrySet().stream().map(entry -> entry.getKey() + " = " + entry.getValue()).toList(), page));
    }
    private static Component page(List<String> entries, int page) {
        int pages = Math.max(1, (entries.size() + 19) / 20);
        if (page > pages) throw new IllegalArgumentException(message("page_invalid").getString());
        String rows = String.join("; ", entries.subList((page - 1) * 20, Math.min(entries.size(), page * 20)));
        return Component.translatable("icecore.interaction.page", page, pages, entries.size(), rows);
    }
    private static int inspect(CommandContext<CommandSourceStack> context, boolean explicit) {
        return run(context, () -> {
            Target target = target(context, explicit); var state = target.level().getBlockState(target.position());
            String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            Set<String> tags = state.getTags().map(tag -> tag.location().toString()).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
            var deny = MANAGER.config().blacklist().stream().filter(rule -> rule.matches(id, tags::contains)).toList();
            var allow = MANAGER.config().whitelist().stream().filter(rule -> rule.matches(id, tags::contains)).toList();
            boolean exception = id.equals(MANAGER.snapshot().exceptions().entries().get(InteractionSnapshot.location(target.level(), target.position())));
            boolean blocked = MANAGER.snapshot().blocked(target.level(), target.position());
            return Component.translatable("icecore.interaction.inspect", id, BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace(),
                    String.join(", ", tags), deny.toString(), allow.toString(), message(exception ? "yes" : "no"), message(blocked ? "denied" : "allowed"));
        });
    }
    private static Component message(String key) { return Component.translatable("icecore.interaction." + key); }
}
