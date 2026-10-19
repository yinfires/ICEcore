package com.yinfires.icecore.shelf;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;

/** Inseparable two-cell shell. Only its lower half owns the inventory. */
public final class ShelfBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape SHAPE = Shapes.or(box(1,0,1,2,16,2), box(14,0,1,15,16,2),
            box(1,0,14,2,16,15), box(14,0,14,15,16,15), box(0,7,0,16,8,16), box(0,15,0,16,16,16));
    public ShelfBlock() {
        super(Properties.copy(Blocks.OAK_PLANKS).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, HALF); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    public static BlockPos base(BlockState state, BlockPos pos) { return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos; }
    public static boolean paired(BlockState state, BlockState other) {
        return other.is(state.getBlock()) && other.getValue(HALF) != state.getValue(HALF) && other.getValue(FACING) == state.getValue(FACING);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockState lower=state.setValue(HALF,DoubleBlockHalf.LOWER);
        level.setBlock(pos,lower,2);
        level.setBlock(pos.above(),lower.setValue(HALF,DoubleBlockHalf.UPPER),2);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ShelfBlockEntity(pos, state) : null;
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return rotate(state, mirror.getRotation(state.getValue(FACING))); }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.BLOCK; }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(base(state,pos)) instanceof ShelfBlockEntity shelf)
            NetworkHooks.openScreen(server, shelf, shelf.getBlockPos());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) { schedule(level,pos); }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) { schedule(level,pos); }
    private void schedule(Level level, BlockPos pos) {
        if (!level.isClientSide && !level.restoringBlockSnapshots) level.scheduleTick(pos,this,1);
    }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) shelf.flushUpdate();
        BlockPos partner = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        if (!paired(state,level.getBlockState(partner))) level.destroyBlock(pos,true);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (state.is(replacement.getBlock())) return;
        if (!level.isClientSide && !level.restoringBlockSnapshots) {
            if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
                if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) Containers.dropContents(level,pos,shelf);
                shelf.clearContent();
            }
            BlockPos partner = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            if (paired(state,level.getBlockState(partner))) level.setBlock(partner,Blocks.AIR.defaultBlockState(),3);
            level.updateNeighbourForOutputSignal(pos,this);
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
}
