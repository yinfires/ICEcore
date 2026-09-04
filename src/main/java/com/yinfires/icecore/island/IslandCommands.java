package com.yinfires.icecore.island;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.yinfires.icecore.building.BuildingDataManager;
import com.yinfires.icecore.building.RegionDefinition;
import com.yinfires.icecore.feedback.PlayerFeedback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Operator-only island prototype commands: capture a named region, record its entrance camera, hide it (real removal), show it (entrance cutscene + real placement). */
public final class IslandCommands {
    /** Prototype hide spread duration in server ticks (3 s). */
    private static final int DEFAULT_DURATION_TICKS = 60;
    /** Ticks from client receipt to a fully-white (fully-shattered) screen. Sent as the
     *  packet's {@code buildup}; the client reveals the view, then runs its stepped shatter
     *  so the screen finishes white exactly at this tick. */
    private static final int ENTRANCE_BUILDUP_TICKS = 48;
    /** Extra ticks past the client's full-white moment before placement starts, to absorb
     *  network latency so the island is ALWAYS placed behind an already-white screen. */
    private static final int PLACE_MARGIN_TICKS = 10;
    /** Server delay before placement: client receipt→white time + latency margin. */
    private static final int ENTRANCE_PLACE_DELAY_TICKS =
            ENTRANCE_BUILDUP_TICKS + PLACE_MARGIN_TICKS;

    private IslandCommands() {
    }

    public static void attach(LiteralArgumentBuilder<CommandSourceStack> root) {
        var capture = literal("capture")
                .then(argument("island", StringArgumentType.word())
                        .then(argument("region", StringArgumentType.word()).suggests(IslandCommands::regionNames)
                                .executes(IslandCommands::capture)));
        var hide = literal("hide")
                .then(argument("island", StringArgumentType.word()).suggests(IslandCommands::islandNames)
                        .executes(ctx -> transition(ctx, false)));
        var show = literal("show")
                .then(argument("island", StringArgumentType.word()).suggests(IslandCommands::islandNames)
                        .executes(ctx -> transition(ctx, true)));
        var remove = literal("remove")
                .then(argument("island", StringArgumentType.word()).suggests(IslandCommands::islandNames)
                        .executes(IslandCommands::remove));
        var recapture = literal("recapture")
                .then(argument("island", StringArgumentType.word()).suggests(IslandCommands::islandNames)
                        .executes(IslandCommands::recapture));
        var camera = literal("camera")
                .then(literal("set")
                        .then(argument("island", StringArgumentType.word()).suggests(IslandCommands::islandNames)
                                .executes(IslandCommands::cameraSet)))
                .then(literal("clear")
                        .then(argument("island", StringArgumentType.word()).suggests(IslandCommands::islandNames)
                                .executes(IslandCommands::cameraClear)));
        root.then(literal("island").then(capture).then(hide).then(show).then(remove).then(recapture).then(camera));
    }

    private static int capture(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String islandName = getString(ctx, "island");
        String regionName = getString(ctx, "region");
        RegionDefinition region = BuildingDataManager.get().data().regions().get(regionName);
        if (region == null || !region.isComplete()) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.region.missing", regionName));
            return 0;
        }
        ServerLevel level = resolveLevel(source, region.dimension());
        if (level == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.region.dimension", region.dimension()));
            return 0;
        }
        int[] p1 = region.pos1();
        int[] p2 = region.pos2();
        boolean ok = IslandManager.get().capture(islandName, regionName, level,
                new BlockPos(p1[0], p1[1], p1[2]), new BlockPos(p2[0], p2[1], p2[2]));
        if (!ok) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.capture.failed", islandName));
            return 0;
        }
        long volume = IslandManager.get().entry(islandName).data().volume();
        PlayerFeedback.showSuccess(source,
                () -> Component.translatable("icecore.island.capture.success", islandName, volume), true);
        return 1;
    }

    private static int transition(CommandContext<CommandSourceStack> ctx, boolean show) {
        CommandSourceStack source = ctx.getSource();
        String islandName = getString(ctx, "island");
        IslandManager manager = IslandManager.get();
        IslandManager.Entry entry = manager.entry(islandName);
        if (entry == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        if (!IslandPlacer.idle()) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.busy"));
            return 0;
        }
        ServerLevel level = manager.levelOf(entry);
        if (level == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.region.dimension",
                    entry.dimension().location().toString()));
            return 0;
        }
        if (show) {
            if (entry.hasEntrance()) {
                // Broadcast the entrance cutscene, hold placement for the buildup while
                // the swirl ramps up, then place batched behind the intensifying warp.
                // The onComplete broadcast tells clients placement is done → flash + reveal.
                int placeTicks = IslandPlacer.estimatePlaceTicks(entry.data().volume());
                ClientBoundIslandEntrancePacket packet = new ClientBoundIslandEntrancePacket(
                        entry.dimension().location(),
                        entry.entranceX(), entry.entranceY(), entry.entranceZ(),
                        entry.entranceYaw(), entry.entrancePitch(),
                        ENTRANCE_BUILDUP_TICKS, placeTicks);
                com.yinfires.icecore.network.ICECoreNetwork.sendToDimension(packet, level);
                // Placement is held until after the client is fully white (charge + iris +
                // margin), so the island is never placed while the distortion is on screen.
                IslandPlacer.enqueueShow(level, entry.min(), entry.data(), ENTRANCE_PLACE_DELAY_TICKS, () -> {
                    manager.setState(islandName, IslandManager.State.SHOWN);
                    com.yinfires.icecore.network.ICECoreNetwork.sendToDimension(
                            new ClientBoundIslandEntranceDonePacket(entry.dimension().location()), level);
                });
            } else {
                // No entrance camera recorded: place near-instantly with no cutscene.
                IslandPlacer.enqueueShow(level, entry.min(), entry.data(), 1,
                        () -> manager.setState(islandName, IslandManager.State.SHOWN));
            }
            PlayerFeedback.showSuccess(source,
                    () -> Component.translatable("icecore.island.show.started", islandName), true);
        } else {
            IslandPlacer.enqueueHide(level, entry.min(), entry.data(), DEFAULT_DURATION_TICKS,
                    () -> manager.setState(islandName, IslandManager.State.HIDDEN));
            PlayerFeedback.showSuccess(source,
                    () -> Component.translatable("icecore.island.hide.started", islandName), true);
        }
        return 1;
    }

    private static int cameraSet(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String islandName = getString(ctx, "island");
        IslandManager manager = IslandManager.get();
        if (manager.entry(islandName) == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.camera.playeronly"));
            return 0;
        }
        Vec3 eye = player.getEyePosition();
        if (!manager.setEntrance(islandName, eye.x, eye.y, eye.z, player.getYRot(), player.getXRot())) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        PlayerFeedback.showSuccess(source,
                () -> Component.translatable("icecore.island.camera.set", islandName), true);
        return 1;
    }

    private static int cameraClear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String islandName = getString(ctx, "island");
        IslandManager manager = IslandManager.get();
        if (!manager.clearEntrance(islandName)) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        PlayerFeedback.showSuccess(source,
                () -> Component.translatable("icecore.island.camera.cleared", islandName), true);
        return 1;
    }

    private static int recapture(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String islandName = getString(ctx, "island");
        IslandManager manager = IslandManager.get();
        IslandManager.Entry entry = manager.entry(islandName);
        if (entry == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        // Rescanning mid-placement would read a half-processed volume.
        if (!IslandPlacer.idle()) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.busy"));
            return 0;
        }
        // A HIDDEN island's blocks are gone from the world; rescanning would just
        // capture air over the snapshot. Show it back first.
        if (entry.state() == IslandManager.State.HIDDEN) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.recapture.hidden", islandName));
            return 0;
        }
        ServerLevel level = manager.levelOf(entry);
        if (level == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.region.dimension",
                    entry.dimension().location().toString()));
            return 0;
        }
        if (!manager.recapture(islandName)) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.capture.failed", islandName));
            return 0;
        }
        long volume = manager.entry(islandName).data().volume();
        PlayerFeedback.showSuccess(source,
                () -> Component.translatable("icecore.island.recapture.success", islandName, volume), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String islandName = getString(ctx, "island");
        IslandManager manager = IslandManager.get();
        IslandManager.Entry entry = manager.entry(islandName);
        if (entry == null) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        // Removing while a placement runs would leave a half-processed volume.
        if (!IslandPlacer.idle()) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.busy"));
            return 0;
        }
        // A HIDDEN island's blocks are gone from the world; dropping the snapshot
        // makes that removal permanent. Warn and refuse so the operator shows it
        // back first (or must delete the file by hand knowingly).
        if (entry.state() == IslandManager.State.HIDDEN) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.remove.hidden", islandName));
            return 0;
        }
        if (!manager.remove(islandName)) {
            PlayerFeedback.showFailure(source, Component.translatable("icecore.island.missing", islandName));
            return 0;
        }
        PlayerFeedback.showSuccess(source,
                () -> Component.translatable("icecore.island.remove.success", islandName), true);
        return 1;
    }

    private static ServerLevel resolveLevel(CommandSourceStack source, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id == null) {
            return null;
        }
        return source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    private static CompletableFuture<Suggestions> regionNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BuildingDataManager.get().data().regions().keySet(), builder);
    }

    private static CompletableFuture<Suggestions> islandNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(IslandManager.get().names(), builder);
    }
}
