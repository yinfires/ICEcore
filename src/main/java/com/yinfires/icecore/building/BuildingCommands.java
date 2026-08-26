package com.yinfires.icecore.building;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.yinfires.icecore.compat.cozycafe.range.CozyCafeRangeCommands;
import com.yinfires.icecore.currency.CurrencyCommands;
import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.time.TimeCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

/** Operator-only data administration commands. Mutations are copied, validated, and saved atomically. */
public final class BuildingCommands {
    private BuildingCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = literal("icecore").requires(source -> source.hasPermission(2));
        var build = literal("build").then(literal("reload").executes(BuildingCommands::reload));

        var addPos2 = argument("pos2", BlockPosArgument.blockPos()).executes(ctx -> addRegion(ctx, true));
        var addPos1 = argument("pos1", BlockPosArgument.blockPos()).then(addPos2);
        var addName = argument("name", StringArgumentType.word()).suggests(BuildingCommands::unusedRegion)
                .executes(ctx -> addRegion(ctx, false)).then(addPos1);
        var regionAdd = literal("add").then(addName);
        var setPos2 = argument("pos2", BlockPosArgument.blockPos()).executes(BuildingCommands::setRegion);
        var setPos1 = argument("pos1", BlockPosArgument.blockPos()).then(setPos2);
        var setName = argument("name", StringArgumentType.word()).suggests(BuildingCommands::regionNames)
                .executes(BuildingCommands::enterAdjust).then(setPos1);
        var regionSet = literal("set").then(setName);
        build.then(literal("region").then(regionAdd).then(regionSet));

        var listAdd = literal("add").then(argument("name", StringArgumentType.word()).suggests(BuildingCommands::unusedList)
                .executes(BuildingCommands::addList));
        var listRemove = literal("remove").then(argument("name", StringArgumentType.word()).suggests(BuildingCommands::listNames)
                .executes(BuildingCommands::removeList));
        var blockName = argument("list", StringArgumentType.word()).suggests(BuildingCommands::listNames);
        // A block id contains ':' and a tag entry starts with '#'; both characters are
        // intentionally accepted by the final argument instead of word() parsing.
        blockName.then(literal("add").then(argument("entry", StringArgumentType.greedyString())
                .suggests(BuildingCommands::blockEntries).executes(ctx -> editEntry(ctx, true))));
        blockName.then(literal("remove").then(argument("entry", StringArgumentType.greedyString()).suggests(BuildingCommands::entryNames)
                .executes(ctx -> editEntry(ctx, false))));
        var blockEdit = literal("block").then(blockName);
        var regionName = argument("region", StringArgumentType.word()).suggests(BuildingCommands::regionNames);
        regionName.then(literal("add").executes(ctx -> editRegionBinding(ctx, true)));
        regionName.then(literal("remove").executes(ctx -> editRegionBinding(ctx, false)));
        var regionEdit = literal("region").then(argument("list", StringArgumentType.word()).suggests(BuildingCommands::listNames).then(regionName));
        var supportName = argument("support", StringArgumentType.word()).suggests(BuildingCommands::listNames);
        supportName.then(literal("add").executes(ctx -> editSupportBinding(ctx, true)));
        supportName.then(literal("remove").executes(ctx -> editSupportBinding(ctx, false)));
        var supportEdit = literal("support").then(argument("list", StringArgumentType.word()).suggests(BuildingCommands::listNames).then(supportName));
        var faceName = argument("face", StringArgumentType.word()).suggests((ctx, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                new String[]{"up", "down", "north", "south", "east", "west", "side"}, builder));
        faceName.then(literal("add").executes(ctx -> editFace(ctx, true)));
        faceName.then(literal("remove").executes(ctx -> editFace(ctx, false)));
        var faceEdit = literal("face").then(argument("list", StringArgumentType.word()).suggests(BuildingCommands::listNames).then(faceName));
        build.then(literal("list").then(listAdd).then(listRemove).then(blockEdit).then(regionEdit).then(supportEdit).then(faceEdit));
        root.then(build);
        CurrencyCommands.attach(root);
        CozyCafeRangeCommands.attach(root);
        TimeCommands.attach(root);
        dispatcher.register(root);
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        return BuildingDataManager.get().reload(context.getSource().getPlayer()) ? 1 : 0;
    }

    private static int addRegion(CommandContext<CommandSourceStack> context, boolean bounded) {
        String name = getString(context, "name");
        if (BuildingDataManager.get().data().regions().containsKey(name)) return fail(context, "icecore.build.exists");
        return mutate(context, data -> {
            RegionDefinition region = new RegionDefinition(name);
            if (bounded) region.setBounds(context.getSource().getLevel().dimension(),
                    BlockPosArgument.getBlockPos(context, "pos1"), BlockPosArgument.getBlockPos(context, "pos2"));
            data.regions().put(name, region);
        });
    }

    private static int enterAdjust(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        String name = getString(context, "name");
        if (!BuildingDataManager.get().data().regions().containsKey(name)) return fail(context, "icecore.build.missing");
        if (!AdjustmentModeManager.enter(player, name)) return fail(context, "icecore.build.adjust.invalid_stick");
        PlayerFeedback.show(player, Component.translatable("icecore.build.adjust.enter", name));
        return 1;
    }

    private static int setRegion(CommandContext<CommandSourceStack> context) {
        String name = getString(context, "name");
        if (!BuildingDataManager.get().data().regions().containsKey(name)) return fail(context, "icecore.build.missing");
        return mutate(context, data -> data.regions().get(name).setBounds(context.getSource().getLevel().dimension(),
                BlockPosArgument.getBlockPos(context, "pos1"), BlockPosArgument.getBlockPos(context, "pos2")));
    }

    private static int addList(CommandContext<CommandSourceStack> context) {
        String name = getString(context, "name");
        if (BuildingDataManager.get().data().blockLists().containsKey(name)) return fail(context, "icecore.build.exists");
        return mutate(context, data -> data.blockLists().put(name, new BlockListDefinition(name)));
    }

    private static int removeList(CommandContext<CommandSourceStack> context) {
        String name = getString(context, "name");
        if (!BuildingDataManager.get().data().blockLists().containsKey(name)) return fail(context, "icecore.build.missing");
        return mutate(context, data -> {
            data.blockLists().remove(name);
            data.blockLists().values().forEach(list -> {
                list.supports().remove(name);
            });
        });
    }

    private static int editEntry(CommandContext<CommandSourceStack> context, boolean add) {
        String name = getString(context, "list");
        String entry = getString(context, "entry");
        return mutate(context, data -> {
            BlockListDefinition list = data.blockLists().get(name);
            if (list == null) throw new IllegalArgumentException("missing block list");
            if (add && !list.entries().contains(entry)) list.entries().add(entry);
            if (!add) list.entries().remove(entry);
        });
    }

    private static int editRegionBinding(CommandContext<CommandSourceStack> context, boolean add) {
        return mutate(context, data -> {
            BlockListDefinition list = data.blockLists().get(getString(context, "list"));
            if (list == null || !data.regions().containsKey(getString(context, "region"))) throw new IllegalArgumentException("missing binding");
            String region = getString(context, "region");
            if (add) list.regions().add(region); else list.regions().remove(region);
        });
    }

    private static int editSupportBinding(CommandContext<CommandSourceStack> context, boolean add) {
        return mutate(context, data -> {
            BlockListDefinition list = data.blockLists().get(getString(context, "list"));
            if (list == null || !data.blockLists().containsKey(getString(context, "support"))) throw new IllegalArgumentException("missing binding");
            String support = getString(context, "support");
            if (add) list.supports().add(support); else list.supports().remove(support);
        });
    }

    private static int editFace(CommandContext<CommandSourceStack> context, boolean add) {
        return mutate(context, data -> {
            BlockListDefinition list = data.blockLists().get(getString(context, "list"));
            if (list == null) throw new IllegalArgumentException("missing block list");
            String face = getString(context, "face");
            if (add && face.equals("side")) {
                list.allowedFaces().add("north");
                list.allowedFaces().add("south");
                list.allowedFaces().add("east");
                list.allowedFaces().add("west");
            } else if (!add && face.equals("side")) {
                list.allowedFaces().removeIf(value -> value.equals("north") || value.equals("south")
                        || value.equals("east") || value.equals("west"));
            } else if (add) {
                list.allowedFaces().add(face);
            } else {
                list.allowedFaces().remove(face);
            }
        });
    }

    private static int mutate(CommandContext<CommandSourceStack> context, java.util.function.Consumer<BuildingData> mutation) {
        ServerPlayer player = context.getSource().getPlayer();
        try {
            return BuildingDataManager.get().mutate(mutation, player) ? 1 : 0;
        } catch (RuntimeException exception) {
            return fail(context, "icecore.build.invalid");
        }
    }

    private static int fail(CommandContext<CommandSourceStack> context, String key) {
        PlayerFeedback.showFailure(context.getSource(), Component.translatable(key));
        return 0;
    }

    private static CompletableFuture<Suggestions> regionNames(CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return net.minecraft.commands.SharedSuggestionProvider.suggest(BuildingDataManager.get().data().regions().keySet(), builder);
    }

    private static CompletableFuture<Suggestions> listNames(CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return net.minecraft.commands.SharedSuggestionProvider.suggest(BuildingDataManager.get().data().blockLists().keySet(), builder);
    }

    private static CompletableFuture<Suggestions> entryNames(CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        BlockListDefinition list = BuildingDataManager.get().data().blockLists().get(getString(context, "list"));
        return net.minecraft.commands.SharedSuggestionProvider.suggest(list == null ? java.util.Collections.emptyList() : list.entries(), builder);
    }

    private static CompletableFuture<Suggestions> blockEntries(CommandContext<CommandSourceStack> context,
                                                                com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        var registry = context.getSource().registryAccess().registryOrThrow(Registries.BLOCK);
        var values = new java.util.ArrayList<String>();
        registry.keySet().stream().map(ResourceLocation::toString).forEach(id -> values.add(id));
        registry.getTagNames().map(tag -> "#" + tag.location()).forEach(values::add);
        return net.minecraft.commands.SharedSuggestionProvider.suggest(values, builder);
    }

    private static CompletableFuture<Suggestions> unusedRegion(CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return CompletableFuture.completedFuture(builder.build());
    }

    private static CompletableFuture<Suggestions> unusedList(CommandContext<CommandSourceStack> context, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        return net.minecraft.commands.SharedSuggestionProvider.suggest(java.util.Collections.<String>emptyList(), builder);
    }
}
