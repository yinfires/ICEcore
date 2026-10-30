package com.yinfires.icecore.compat.bakery.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.yinfires.icecore.compat.bakery.KitchenConnections;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.*;
import net.minecraftforge.client.model.geometry.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Native model-state rotations bake all five variants; world reads occur only during chunk builds. */
@Mod.EventBusSubscriber(modid = "icecore", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KitchenCounterGeometry implements IUnbakedGeometry<KitchenCounterGeometry> {
    private final Map<KitchenConnections.Shape, BlockModel> variants;

    private KitchenCounterGeometry(Map<KitchenConnections.Shape, BlockModel> variants) { this.variants = variants; }

    @SubscribeEvent public static void loaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("kitchen_counter", (IGeometryLoader<KitchenCounterGeometry>) KitchenCounterGeometry::read);
    }

    private static KitchenCounterGeometry read(JsonObject json, JsonDeserializationContext context) {
        Map<KitchenConnections.Shape, BlockModel> models = new EnumMap<>(KitchenConnections.Shape.class);
        for (var shape : KitchenConnections.Shape.values()) {
            models.put(shape, context.deserialize(json.getAsJsonObject("variants")
                    .get(shape.name().toLowerCase(Locale.ROOT)), BlockModel.class));
        }
        return new KitchenCounterGeometry(models);
    }

    @Override public void resolveParents(Function<ResourceLocation, UnbakedModel> getter, IGeometryBakingContext context) {
        variants.values().forEach(model -> model.resolveParents(getter));
    }

    @Override public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
            Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides, ResourceLocation id) {
        Map<KitchenConnections.Shape, BakedModel> baked = new EnumMap<>(KitchenConnections.Shape.class);
        variants.forEach((shape, model) -> baked.put(shape, model.bake(baker, model, sprites, state, id, true)));
        return new Counter(baked);
    }

    static final class Counter extends BakedModelWrapper<BakedModel> {
        private static final ModelProperty<KitchenConnections.Shape> SHAPE = new ModelProperty<>();
        private static final Map<KitchenConnections.Shape, ModelData> DATA = new EnumMap<>(KitchenConnections.Shape.class);
        static { for (var shape : KitchenConnections.Shape.values()) DATA.put(shape, ModelData.builder().with(SHAPE, shape).build()); }
        private final Map<KitchenConnections.Shape, BakedModel> variants;

        Counter(Map<KitchenConnections.Shape, BakedModel> variants) {
            super(variants.get(KitchenConnections.Shape.DEFAULT));
            this.variants = Map.copyOf(variants);
        }

        @Override public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return data;
            var shape = KitchenConnections.shape(level, pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            return data == ModelData.EMPTY ? DATA.get(shape) : data.derive().with(SHAPE, shape).build();
        }

        @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
            var shape = data.get(SHAPE);
            return variants.get(shape == null ? KitchenConnections.Shape.DEFAULT : shape).getQuads(state, side, random, data, type);
        }
    }
}
