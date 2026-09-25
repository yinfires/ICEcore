package com.yinfires.icecore.mixing;

import com.yinfires.icecore.ICECore;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import com.yinfires.icecore.oven.OvenRenderer;
import com.yinfires.icecore.oven.ModOven;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMixing {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ICECore.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ICECore.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ICECore.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, ICECore.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ICECore.MOD_ID);

    public static final RegistryObject<Block> MIXING_BOWL = BLOCKS.register("mixing_bowl", () -> new MixingBowlBlock(
            BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> MIXING_BOWL_ITEM = ITEMS.register("mixing_bowl", () -> new BlockItem(MIXING_BOWL.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<MixingBowlBlockEntity>> MIXING_BOWL_ENTITY = BLOCK_ENTITIES.register("mixing_bowl",
            () -> BlockEntityType.Builder.of(MixingBowlBlockEntity::new, MIXING_BOWL.get()).build(null));
    public static final RegistryObject<RecipeType<MixingBowlRecipe>> MIXING_BOWL_RECIPE = RECIPE_TYPES.register("mixing_bowl", () -> new RecipeType<>() {
        public String toString() { return ICECore.MOD_ID + ":mixing_bowl"; }
    });
    public static final RegistryObject<RecipeSerializer<MixingBowlRecipe>> MIXING_BOWL_SERIALIZER = RECIPE_SERIALIZERS.register("mixing_bowl", MixingBowlRecipe.Serializer::new);

    private ModMixing() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> BlockEntityRenderers.register(MIXING_BOWL_ENTITY.get(), MixingBowlRenderer::new));
        event.enqueueWork(() -> BlockEntityRenderers.register(ModOven.OVEN_ENTITY.get(), OvenRenderer::new));
    }
}
