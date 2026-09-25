package com.yinfires.icecore.mixing;

import com.yinfires.icecore.feedback.PlayerFeedback;
import com.yinfires.icecore.item.ModItems;
import com.yinfires.icecore.workstation.WorkstationContainerCompat;
import com.yinfires.icecore.workstation.WorkstationContainerInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public final class MixingBowlBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(box(3,0,3,13,8,13), box(4,3,4,12,8,12));
    public MixingBowlBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MixingBowlBlockEntity(pos,state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModMixing.MIXING_BOWL_ENTITY.get(), MixingBowlBlockEntity::tick);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.WRENCH.get())) return InteractionResult.PASS;
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof MixingBowlBlockEntity bowl)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!held.isEmpty() || bowl.stage() == MixingBowlBlockEntity.Stage.OUTPUT) return InteractionResult.PASS;
            if (bowl.startStir()) {
                if (level instanceof ServerLevel server) for(ItemStack input:bowl.visibleItems()) if(!input.isEmpty())
                    server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,input),pos.getX()+.5,pos.getY()+.6,pos.getZ()+.5,1,.08,.05,.08,.05);
            }
            return InteractionResult.CONSUME;
        }
        if (bowl.stage() == MixingBowlBlockEntity.Stage.STIRRING) return InteractionResult.CONSUME;
        if (bowl.stage() == MixingBowlBlockEntity.Stage.OUTPUT) return handleOutput(serverPlayer, hand, held, bowl);

        if (held.isEmpty()) {
            ItemStack plain = bowl.takeLastPlainInput();
            if (!plain.isEmpty()) {
                player.setItemInHand(hand,plain);
                playItemSound(level, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM);
            } else {
                ItemStack required = bowl.requiredLastInputContainer();
                if (!required.isEmpty()) PlayerFeedback.show(serverPlayer,
                        Component.translatable("icecore.mixing.need_input_container", required.getHoverName()));
            }
            return InteractionResult.CONSUME;
        }

        // Holding an item means insertion by default. The only retrieval exceptions are
        // explicit empty carriers: a matching fluid container or the exact container
        // recorded for a containerized ingredient.
        WorkstationContainerCompat.FilledContainer filled = bowl.fillHeldContainer(held);
        if (filled != null) {
            exchangeContainer(serverPlayer, hand, held, filled.stack());
            level.playSound(null, pos, WorkstationContainerCompat.fillSound(filled.category()), SoundSource.BLOCKS, 1F, 1F);
            return InteractionResult.CONSUME;
        }
        if (WorkstationContainerCompat.isKnownEmptyContainer(held)) {
            ItemStack containerized = bowl.takeContainerizedInput(held);
            if (!containerized.isEmpty()) {
                exchangeContainer(serverPlayer, hand, held, containerized);
                playItemSound(level, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM);
                return InteractionResult.CONSUME;
            }
        }
        if (bowl.hasFluid() && WorkstationContainerCompat.isFluidContainer(held)
                && WorkstationContainerCompat.describe(held)
                .map(entry -> entry.behavior() != WorkstationContainerCompat.Behavior.FLUID).orElse(true)) {
            return InteractionResult.CONSUME;
        }
        var insertedContainer = WorkstationContainerCompat.describe(held);
        if (insertedContainer.isPresent() && insertedContainer.get().behavior() == WorkstationContainerCompat.Behavior.FLUID) {
            WorkstationContainerCompat.Entry entry = insertedContainer.get();
            if (bowl.addFluid(entry.fluid())) {
                exchangeContainer(serverPlayer, hand, held, entry.empty());
                level.playSound(null, pos, WorkstationContainerCompat.emptySound(entry.category()), SoundSource.BLOCKS, 1F, 1F);
            }
            return InteractionResult.CONSUME;
        }
        ItemStack inputContainer = insertedContainer
                .filter(entry -> entry.behavior() == WorkstationContainerCompat.Behavior.INGREDIENT)
                .map(WorkstationContainerCompat.Entry::empty).orElse(ItemStack.EMPTY);
        if (!bowl.addInput(held, inputContainer)) return InteractionResult.CONSUME;
        insertedContainer.ifPresentOrElse(entry -> exchangeContainer(serverPlayer,hand,held,entry.empty()), () -> { if(!player.getAbilities().instabuild) held.shrink(1); });
        playItemSound(level, pos, SoundEvents.ITEM_FRAME_ADD_ITEM);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult handleOutput(ServerPlayer player, InteractionHand hand, ItemStack held, MixingBowlBlockEntity bowl) {
        Ingredient required = bowl.requiredOutputCarrier();
        ItemStack result = bowl.takeOutput(held);
        if (!result.isEmpty()) {
            if (required.isEmpty()) player.setItemInHand(hand,result);
            else exchangeContainer(player,hand,held,result);
            playTakeSound(player.level(), bowl.getBlockPos(), result);
        } else if (!required.isEmpty()) {
            ItemStack[] options=required.getItems();
            if(options.length>0) PlayerFeedback.show(player,Component.translatable("icecore.mixing.need_output_container",options[0].getHoverName()));
        }
        return InteractionResult.CONSUME;
    }

    private static void exchangeContainer(ServerPlayer player, InteractionHand hand, ItemStack held, ItemStack replacement) {
        WorkstationContainerInteraction.exchange(player, hand, held, replacement);
    }

    private static void playTakeSound(Level level, BlockPos pos, ItemStack restored) {
        var entry = WorkstationContainerCompat.describe(restored);
        if (entry.isPresent() && entry.get().behavior() == WorkstationContainerCompat.Behavior.FLUID) {
            level.playSound(null, pos, WorkstationContainerCompat.fillSound(entry.get().category()), SoundSource.BLOCKS, 1F, 1F);
        } else {
            playItemSound(level, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM);
        }
    }

    private static void playItemSound(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, 1.1F);
    }
}
