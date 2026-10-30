package com.yinfires.icecore.compat.bakery.client;

import com.yinfires.icecore.compat.bakery.KitchenConnections;
import com.yinfires.icecore.compat.bakery.KitchenConnections.Shape;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class KitchenCounterTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try (var hooks = mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }
    @Test void nativeInnerOuterLeftRightAndPriorityInAllFourDirections() {
        for (var facing : Direction.Plane.HORIZONTAL) {
            assertEquals(Shape.DEFAULT, KitchenConnections.choose(facing, null, null));
            assertEquals(Shape.DEFAULT, KitchenConnections.choose(facing, facing, facing.getOpposite()));
            assertEquals(Shape.INSIDE_CORNER_RIGHT, KitchenConnections.choose(facing, facing.getClockWise(), null));
            assertEquals(Shape.INSIDE_CORNER_LEFT, KitchenConnections.choose(facing, facing.getCounterClockWise(), null));
            assertEquals(Shape.OUTSIDE_CORNER_LEFT, KitchenConnections.choose(facing, null, facing.getClockWise()));
            assertEquals(Shape.OUTSIDE_CORNER_RIGHT, KitchenConnections.choose(facing, null, facing.getCounterClockWise()));
            assertEquals(Shape.INSIDE_CORNER_LEFT,
                    KitchenConnections.choose(facing, facing.getCounterClockWise(), facing.getClockWise()));
        }
    }

    @Test void sinkBaseCollisionIsFullWidthWithoutFillingTheBasinAndCachesOnlyFourOrientations() {
        var slab = net.minecraft.world.level.block.Block.box(0, 0, 2, 16, 12, 16);
        var rim = net.minecraft.world.phys.shapes.Shapes.or(
                net.minecraft.world.level.block.Block.box(0, 12, 0, 16, 16, 3),
                net.minecraft.world.level.block.Block.box(0, 12, 12, 16, 16, 16),
                net.minecraft.world.level.block.Block.box(0, 12, 3, 3, 16, 12),
                net.minecraft.world.level.block.Block.box(13, 12, 3, 16, 16, 12));
        var original = net.minecraft.world.phys.shapes.Shapes.or(slab, rim);
        var expectedBase = net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 12, 16);
        var basin = net.minecraft.world.level.block.Block.box(3, 12, 3, 13, 16, 12);
        for (var facing : Direction.Plane.HORIZONTAL) {
            var expanded = com.yinfires.icecore.compat.bakery.KitchenSinkShapes.extend(original, facing);
            assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(expectedBase, expanded, net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST));
            assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(original, expanded, net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST));
            assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(basin, expanded, net.minecraft.world.phys.shapes.BooleanOp.AND));
            assertSame(expanded, com.yinfires.icecore.compat.bakery.KitchenSinkShapes.extend(original, facing));
        }
    }

    @Test void chunkModelDataSelectsPrebakedGeometryWithoutStickyCornersAndPreservesForeignData() {
        var models = new EnumMap<Shape, BakedModel>(Shape.class);
        var expected = new EnumMap<Shape, List<BakedQuad>>(Shape.class);
        for (var shape : Shape.values()) {
            var model = mock(BakedModel.class);
            var quads = List.of(mock(BakedQuad.class));
            when(model.getQuads(any(), nullable(Direction.class), any(), any(), nullable(net.minecraft.client.renderer.RenderType.class))).thenReturn(quads);
            models.put(shape, model); expected.put(shape, quads);
        }
        var counter = new KitchenCounterGeometry.Counter(models);
        var world = mock(BlockAndTintGetter.class);
        var state = mock(BlockState.class);
        when(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)).thenReturn(true);
        when(state.getValue(BlockStateProperties.HORIZONTAL_FACING)).thenReturn(Direction.NORTH);
        var marker = new ModelProperty<String>();
        var foreign = ModelData.builder().with(marker, "preserved").build();
        try (var lookup = mockStatic(KitchenConnections.class)) {
            for (var shape : Shape.values()) {
                lookup.when(() -> KitchenConnections.shape(world, BlockPos.ZERO, Direction.NORTH)).thenReturn(shape);
                var data = counter.getModelData(world, BlockPos.ZERO, state, ModelData.EMPTY);
                assertSame(data, counter.getModelData(world, BlockPos.ZERO, state, ModelData.EMPTY));
                assertSame(expected.get(shape), counter.getQuads(state, Direction.UP, RandomSource.create(0), data, null));
                var merged = counter.getModelData(world, BlockPos.ZERO, state, foreign);
                assertEquals("preserved", merged.get(marker));
                assertSame(expected.get(shape), counter.getQuads(state, Direction.UP, RandomSource.create(0), merged, null));
            }
            lookup.when(() -> KitchenConnections.shape(world, BlockPos.ZERO, Direction.NORTH)).thenReturn(Shape.DEFAULT);
            var cleared = counter.getModelData(world, BlockPos.ZERO, state, ModelData.EMPTY);
            assertSame(expected.get(Shape.DEFAULT), counter.getQuads(state, Direction.UP, RandomSource.create(0), cleared, null));
        }
    }
}
