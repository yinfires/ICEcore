package com.yinfires.icecore.compat.dewdrop;

import cool.bot.dewdropfarmland.item.FertilizerItem;
import cool.bot.dewdropwateringcans.event.WateringCanFillEvent;
import cool.bot.dewdropwateringcans.item.wateringCan.WateringCanItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

/** Registered only when both Dew Drop mods exist. No per-tick work or reflective dispatch. */
public final class DewDropIrrigationEvents {
    private static final ResourceLocation SINK = ResourceLocation.fromNamespaceAndPath("bakery", "kitchen_sink");
    private DewDropIrrigationEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, DewDropIrrigationEvents::rightClick);
    }

    private static void rightClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        boolean can = stack.getItem() instanceof WateringCanItem;
        if (!can && !(stack.getItem() instanceof FertilizerItem)) return;
        Level level = event.getLevel();
        if (can && SINK.equals(ForgeRegistries.BLOCKS.getKey(level.getBlockState(event.getPos()).getBlock()))) {
            fillSink(event);
            return;
        }
        BlockPos soil = DewDropIrrigation.soil(level, event.getPos());
        if (soil == null) return;
        // Consume this interaction even on failure so another hand or upstream crop use cannot retry.
        InteractionResult result;
        if (can) {
            result = level.isClientSide ? InteractionResult.SUCCESS
                    : stack.getItem().use(level, event.getEntity(), event.getHand()).getResult();
        } else {
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
            result = level.isClientSide ? InteractionResult.SUCCESS
                    : stack.getItem().useOn(new UseOnContext(event.getEntity(), event.getHand(), hit));
        }
        finish(event, result == InteractionResult.PASS ? InteractionResult.FAIL : result);
    }

    private static void fillSink(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockState clicked = level.getBlockState(event.getPos());
        if (!clicked.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            finish(event, InteractionResult.FAIL);
            return;
        }
        BlockPos lower = clicked.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                ? event.getPos() : event.getPos().below();
        BlockPos upper = lower.above();
        if (!level.hasChunkAt(lower) || !level.hasChunkAt(upper)) {
            finish(event, InteractionResult.FAIL);
            return;
        }
        BlockState bottom = level.getBlockState(lower);
        BlockState top = level.getBlockState(upper);
        var filled = bottom.getBlock().getStateDefinition().getProperty("filled");
        if (bottom.getBlock() != clicked.getBlock() || top.getBlock() != clicked.getBlock()
                || !bottom.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || !top.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || bottom.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER
                || top.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.UPPER
                || !(filled instanceof net.minecraft.world.level.block.state.properties.BooleanProperty water)
                || !bottom.getValue(water) || !top.hasProperty(water)
                || event.getItemStack().getDamageValue() == 0
                || event.getEntity().getCooldowns().isOnCooldown(event.getItemStack().getItem())) {
            finish(event, InteractionResult.FAIL);
            return;
        }
        if (level instanceof ServerLevel server && event.getEntity() instanceof ServerPlayer player) {
            level.setBlock(lower, bottom.setValue(water, false), 3);
            level.setBlock(upper, top.setValue(water, false), 3);
            MinecraftForge.EVENT_BUS.post(new WateringCanFillEvent(server, player, event.getItemStack()));
        }
        finish(event, InteractionResult.sidedSuccess(level.isClientSide));
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
