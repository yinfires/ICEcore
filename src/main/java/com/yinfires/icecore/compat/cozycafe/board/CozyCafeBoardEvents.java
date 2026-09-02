package com.yinfires.icecore.compat.cozycafe.board;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.deco.TextBlockEntity;
import com.yinfires.icecore.ICECore;
import com.yinfires.icecore.feedback.PlayerFeedback;
import io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;

public final class CozyCafeBoardEvents {
    public static final TagKey<net.minecraft.world.level.block.Block> SANDWICH_BOARDS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_tavern", "sandwich_board"));
    private CozyCafeBoardEvents() {}
    public static void register() { MinecraftForge.EVENT_BUS.register(CozyCafeBoardEvents.class); }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBind(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown()) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CafeManagerBlockEntity manager)) return;
        if (!(event.getItemStack().getItem() instanceof BlockItem item) || !isBoard(item.getBlock().defaultBlockState())) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            CozyCafeBoardService.bindItem(event.getItemStack(), manager);
            PlayerFeedback.show(player, Component.translatable("icecore.cozycafe.board.bound", manager.getCafeName()));
        }
        event.setCanceled(true); event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAdventureUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().getAbilities().mayBuild) return;
        if (isBoard(event.getLevel().getBlockState(event.getPos()))) {
            event.setCanceled(true); event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        net.minecraft.core.BlockPos entityPos = event.getPos();
        var state = event.getState();
        var half = state.getProperties().stream()
                .filter(property -> property.getName().equals("half") && property.getValueClass() == Half.class)
                .findFirst().orElse(null);
        if (half != null && state.getValue((net.minecraft.world.level.block.state.properties.Property<Half>) half) == Half.TOP) {
            entityPos = entityPos.below();
        }
        if (event.getLevel().getBlockEntity(entityPos) instanceof TextBlockEntity board) {
            CozyCafeBoardService.unbind(board);
            board.setText("");
            board.refresh();
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) return;
        chunk.getBlockEntities().values().forEach(entity -> {
            if (entity instanceof TextBlockEntity board) CozyCafeBoardService.registerLoaded(board);
        });
    }

    @SubscribeEvent public static void onStop(ServerStoppingEvent event) { CozyCafeBoardService.clear(); }

    public static boolean isBoard(net.minecraft.world.level.block.state.BlockState state) {
        ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return state.is(SANDWICH_BOARDS) || ResourceLocation.fromNamespaceAndPath("kaleidoscope_tavern", "chalkboard").equals(id);
    }
}
