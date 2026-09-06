package com.yinfires.icecore.compat.cozycafe.seating;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public final class CozyCafeSeatingEvents {
    private CozyCafeSeatingEvents() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false,
                PlayerInteractEvent.RightClickBlock.class, CozyCafeSeatingEvents::onRightClick);
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CozyCafeSeatingService.clearRuntimeState());
        MinecraftForge.EVENT_BUS.addListener(CozyCafeSeatingEvents::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(CozyCafeSeatingEvents::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(CozyCafeSeatingEvents::onChunkUnload);
        MinecraftForge.EVENT_BUS.addListener(CozyCafeSeatingEvents::onBlockPlaced);
        MinecraftForge.EVENT_BUS.addListener(CozyCafeSeatingEvents::onBlockBroken);
    }
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            CozyCafeSeatingService.indexChunk(level, chunk, true);
        }
    }
    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            CozyCafeSeatingService.indexChunk(level, chunk, false);
        }
    }
    private static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) CozyCafeSeatingService.indexBlock(level, event.getPos(), event.getPlacedBlock());
    }
    private static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) CozyCafeSeatingService.removeIndexedBlock(level, event.getPos());
    }
    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) CozyCafeSeatingService.tickWarmups(event.getServer());
    }
    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !CozyCafeSeatingService.isReservedInteraction(level, event.getPos())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }
}
