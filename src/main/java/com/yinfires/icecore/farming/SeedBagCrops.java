package com.yinfires.icecore.farming;

import com.yinfires.icecore.item.ModItems;
import com.yinfires.icecore.item.SeedBagDefinition;
import com.yinfires.icecore.item.SeedBagDefinition.Shape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared seed-bag crop rules: structure of each plant, maturity, hoe harvesting, silent removal
 * when the farmland underneath disappears, and planting. All lookups are identity-map hits so
 * the hooks on hot vanilla paths (block removal, bush survival) stay O(1).
 */
public final class SeedBagCrops {
    /** Upper bound for climbing vines; vanilla build height keeps real columns far below this. */
    private static final int MAX_CLIMB = 64;
    private static final int FLAG_QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private SeedBagCrops() {}

    /** A seed-bag definition bound to the blocks and items of the installed mods. */
    public record Crop(SeedBagDefinition definition, Block root, Block upper, IntegerProperty age,
                       IntegerProperty upperAge, Item seedBag, List<Item> produce) {}

    private static final class Holder {
        /** Root and split-upper blocks of every installed seed-bag crop. */
        static final Map<Block, Crop> BY_BLOCK = resolve();
    }

    private static Map<Block, Crop> resolve() {
        Map<Block, Crop> map = new IdentityHashMap<>();
        for (SeedBagDefinition definition : ModItems.SEED_BAG_DEFINITIONS) {
            if (!ForgeRegistries.BLOCKS.containsKey(definition.crop())) continue;
            Block root = ForgeRegistries.BLOCKS.getValue(definition.crop());
            Block upper = definition.upperCrop() != null && ForgeRegistries.BLOCKS.containsKey(definition.upperCrop())
                    ? ForgeRegistries.BLOCKS.getValue(definition.upperCrop()) : null;
            if (definition.shape() == Shape.SPLIT && upper == null) continue;
            IntegerProperty age = ageProperty(root);
            IntegerProperty upperAge = upper != null ? ageProperty(upper) : null;
            if (age == null || (upper != null && upperAge == null)) continue;
            List<Item> produce = new ArrayList<>();
            for (var id : definition.produce()) {
                if (ForgeRegistries.ITEMS.containsKey(id)) produce.add(ForgeRegistries.ITEMS.getValue(id));
            }
            if (produce.isEmpty()) continue;
            Crop crop = new Crop(definition, root, upper, age, upperAge,
                    ModItems.SEED_BAGS.get(definition.id()).get(), List.copyOf(produce));
            map.put(root, crop);
            if (upper != null) map.put(upper, crop);
        }
        return map;
    }

    private static IntegerProperty ageProperty(Block block) {
        Property<?> property = block.getStateDefinition().getProperty("age");
        return property instanceof IntegerProperty integer ? integer : null;
    }

    public static Crop find(Block block) {
        return Holder.BY_BLOCK.get(block);
    }

    public static boolean isHoe(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ToolActions.HOE_DIG);
    }

    /** Original seed items of seed-bag crops (any block item placing a crop block). */
    public static boolean isOriginalPlantingItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && find(blockItem.getBlock()) != null;
    }

    /** Widened soil rule used by the bush survival hook for crops that normally reject farmland. */
    public static boolean survivesOnFarmland(Block block, BlockState state, LevelReader level, BlockPos pos) {
        Crop crop = find(block);
        return crop != null && crop.definition().farmlandOverride() && block == crop.root()
                && isRootState(crop, state) && level.getBlockState(pos.below()).getBlock() instanceof FarmBlock;
    }

    // ---- plant structure -------------------------------------------------------------------

    private static boolean isRootState(Crop crop, BlockState state) {
        if (!state.is(crop.root())) return false;
        return switch (crop.definition().shape()) {
            case DOUBLE -> !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
            case CLIMBING -> rooted(state);
            default -> true;
        };
    }

    private static boolean rooted(BlockState state) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty("rooted");
        return !(property instanceof BooleanProperty bool) || state.getValue(bool);
    }

    /** Position of the plant's root block (the block standing on the soil), or null. */
    public static BlockPos rootOf(Crop crop, LevelReader level, BlockPos pos, BlockState state) {
        switch (crop.definition().shape()) {
            case SINGLE -> {
                return state.is(crop.root()) ? pos : null;
            }
            case DOUBLE -> {
                if (isRootState(crop, state)) return pos;
                BlockPos below = pos.below();
                return state.is(crop.root()) && isRootState(crop, level.getBlockState(below)) ? below : null;
            }
            case SPLIT -> {
                if (state.is(crop.root())) return pos;
                BlockPos below = pos.below();
                return state.is(crop.upper()) && level.getBlockState(below).is(crop.root()) ? below : null;
            }
            case CLIMBING -> {
                BlockPos cursor = pos;
                for (int i = 0; i < MAX_CLIMB; i++) {
                    BlockState current = level.getBlockState(cursor);
                    if (!current.is(crop.root())) return null;
                    if (rooted(current)) return cursor;
                    cursor = cursor.below();
                }
                return null;
            }
        }
        return null;
    }

    /** Root first, then every part above that belongs to the same plant. */
    private static List<BlockPos> partsOf(Crop crop, LevelReader level, BlockPos root) {
        List<BlockPos> parts = new ArrayList<>(2);
        parts.add(root);
        BlockPos cursor = root.above();
        switch (crop.definition().shape()) {
            case DOUBLE -> {
                BlockState above = level.getBlockState(cursor);
                if (above.is(crop.root()) && !isRootState(crop, above)) parts.add(cursor);
            }
            case SPLIT -> {
                if (level.getBlockState(cursor).is(crop.upper())) parts.add(cursor);
            }
            case CLIMBING -> {
                for (int i = 0; i < MAX_CLIMB; i++) {
                    BlockState above = level.getBlockState(cursor);
                    if (!above.is(crop.root()) || rooted(above)) break;
                    parts.add(cursor);
                    cursor = cursor.above();
                }
            }
            default -> {
            }
        }
        return parts;
    }

    /** Harvestable state for a hoe right-click on {@code pos}; climbing vines ripen per segment. */
    private static boolean readyAt(Crop crop, LevelReader level, BlockPos root, BlockPos pos) {
        return switch (crop.definition().shape()) {
            case SPLIT -> {
                BlockState upper = level.getBlockState(root.above());
                yield upper.is(crop.upper()) && upper.getValue(crop.upperAge()) >= crop.definition().matureAge();
            }
            case CLIMBING -> level.getBlockState(pos).getValue(crop.age()) >= crop.definition().matureAge();
            default -> level.getBlockState(root).getValue(crop.age()) >= crop.definition().matureAge();
        };
    }

    public static boolean isHarvestReady(Crop crop, LevelReader level, BlockPos pos, BlockState state) {
        BlockPos root = rootOf(crop, level, pos, state);
        return root != null && readyAt(crop, level, root, pos);
    }

    private static boolean anyPartReady(Crop crop, LevelReader level, BlockPos root, List<BlockPos> parts) {
        for (BlockPos part : parts) {
            if (readyAt(crop, level, root, part)) return true;
        }
        return false;
    }

    // ---- actions ---------------------------------------------------------------------------

    /** Hoe right-click: one produce into the inventory, the plant restarts at age 0. */
    public static InteractionResult harvestWithHoe(Level level, Player player, BlockPos pos, BlockState state) {
        Crop crop = find(state.getBlock());
        if (crop == null) return InteractionResult.PASS;
        BlockPos root = rootOf(crop, level, pos, state);
        if (root == null || !readyAt(crop, level, root, pos)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            give(player, new ItemStack(pickProduce(crop, level.random)));
            if (crop.definition().shape() == Shape.CLIMBING) {
                level.setBlock(pos, state.setValue(crop.age(), 0), Block.UPDATE_ALL);
            } else {
                List<BlockPos> parts = partsOf(crop, level, root);
                for (int i = parts.size() - 1; i > 0; i--) level.setBlock(parts.get(i), air(), FLAG_QUIET);
                level.setBlock(root, resetRoot(level.getBlockState(root), crop), Block.UPDATE_ALL);
            }
            level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Hoe left-click: whole plant removed, one seed bag plus one produce if ripe into the inventory. */
    public static void breakWithHoe(ServerLevel level, Player player, BlockPos pos, BlockState state) {
        Crop crop = find(state.getBlock());
        if (crop == null) return;
        BlockPos root = rootOf(crop, level, pos, state);
        if (root == null) return;
        List<BlockPos> parts = partsOf(crop, level, root);
        boolean ready = anyPartReady(crop, level, root, parts);
        level.levelEvent(2001, pos, Block.getId(state));
        removeParts(level, parts);
        level.gameEvent(player, GameEvent.BLOCK_DESTROY, root);
        give(player, new ItemStack(crop.seedBag()));
        if (ready) give(player, new ItemStack(pickProduce(crop, level.random)));
    }

    /** Soil at {@code soilPos} stopped being farmland: the plant above vanishes without drops. */
    public static void onSoilLost(Level level, BlockPos soilPos) {
        BlockPos above = soilPos.above();
        BlockState state = level.getBlockState(above);
        Crop crop = find(state.getBlock());
        if (crop == null || !isRootState(crop, state)) return;
        removeParts(level, partsOf(crop, level, above));
    }

    /** Called by dew's daily growth for tea-like double crops so the upper half is kept in sync. */
    public static boolean growDoubleCrop(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        Crop crop = find(state.getBlock());
        if (crop == null || crop.definition().shape() != Shape.DOUBLE
                || !(state.getBlock() instanceof CropBlock cropBlock)
                || !(state.getBlock() instanceof BonemealableBlock growable)) return false;
        if (!cropBlock.isMaxAge(state)) growable.performBonemeal(level, random, pos, state);
        return true;
    }

    /** Seed bag use on farmland; also the adventure-mode path since it bypasses ItemStack#useOn. */
    public static InteractionResult plant(Level level, Player player, InteractionHand hand, BlockPos soilPos,
                                          SeedBagDefinition definition, ItemStack stack) {
        if (!(level.getBlockState(soilPos).getBlock() instanceof FarmBlock)) return InteractionResult.PASS;
        Crop crop = cropFor(definition);
        BlockPos cropPos = soilPos.above();
        if (crop == null || !level.getBlockState(cropPos).isAir()) return InteractionResult.PASS;
        BlockState placed = null;
        if (!level.isClientSide && player != null) {
            placed = crop.root().getStateForPlacement(new BlockPlaceContext(player, hand, stack,
                    new BlockHitResult(Vec3.atBottomCenterOf(cropPos), Direction.UP, soilPos, false)));
        }
        if (placed == null || !placed.is(crop.root())) placed = crop.root().defaultBlockState();
        if (!placed.canSurvive(level, cropPos)) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(cropPos, placed, Block.UPDATE_ALL);
            level.playSound(null, cropPos, placed.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, cropPos);
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Crop cropFor(SeedBagDefinition definition) {
        for (Crop crop : Holder.BY_BLOCK.values()) {
            if (crop.definition() == definition) return crop;
        }
        return null;
    }

    private static BlockState resetRoot(BlockState root, Crop crop) {
        BlockState reset = root.setValue(crop.age(), 0);
        Property<?> supporting = root.getBlock().getStateDefinition().getProperty("supporting");
        return supporting instanceof BooleanProperty bool ? reset.setValue(bool, false) : reset;
    }

    private static void removeParts(LevelAccessor level, List<BlockPos> parts) {
        for (int i = parts.size() - 1; i > 0; i--) level.setBlock(parts.get(i), air(), FLAG_QUIET);
        level.setBlock(parts.get(0), air(), Block.UPDATE_ALL);
    }

    private static Item pickProduce(Crop crop, RandomSource random) {
        List<Item> produce = crop.produce();
        return produce.get(random.nextInt(produce.size()));
    }

    private static void give(Player player, ItemStack stack) {
        ItemHandlerHelper.giveItemToPlayer(player, stack);
    }

    private static BlockState air() {
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }
}
