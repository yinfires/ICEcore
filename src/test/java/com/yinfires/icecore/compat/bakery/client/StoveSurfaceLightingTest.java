package com.yinfires.icecore.compat.bakery.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StoveSurfaceLightingTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try (var hooks = mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }

    @Test void emissionBypassDoesNotChangeWorldLightOrOtherBlocks() throws Exception {
        var world = mock(BlockGetter.class);
        var state = mock(BlockState.class);
        // The unit runtime has no FD registry entry; use the exact identity resolved
        // by the production helper, not a replacement registry or new world state.
        var target = Class.forName(StoveSurfaceLighting.class.getName() + "$Target").getDeclaredField("STOVE");
        target.setAccessible(true);
        when(state.getBlock()).thenReturn((net.minecraft.world.level.block.Block) target.get(null));
        when(state.hasProperty(BlockStateProperties.LIT)).thenReturn(true);
        when(state.getValue(BlockStateProperties.LIT)).thenReturn(true);
        when(state.getLightEmission(world, BlockPos.ZERO)).thenReturn(13);
        assertTrue(StoveSurfaceLighting.isLitStove(state));
        assertEquals(0, StoveSurfaceLighting.emission(state, world, BlockPos.ZERO));
        // The engine's state method is intact: the same state still really emits 13.
        assertEquals(13, state.getLightEmission(world, BlockPos.ZERO));
        var unlit = mock(BlockState.class);
        when(state.setValue(BlockStateProperties.LIT, false)).thenReturn(unlit);
        when(unlit.getShadeBrightness(world, BlockPos.ZERO)).thenReturn(.2F);
        assertEquals(.2F, StoveSurfaceLighting.shade(state, world, BlockPos.ZERO));
        verifyNoInteractions(world);

        when(state.getValue(BlockStateProperties.LIT)).thenReturn(false);
        when(state.getShadeBrightness(world, BlockPos.ZERO)).thenReturn(.6F);
        assertEquals(13, StoveSurfaceLighting.emission(state, world, BlockPos.ZERO));
        assertEquals(.6F, StoveSurfaceLighting.shade(state, world, BlockPos.ZERO));
        when(state.getBlock()).thenReturn(Blocks.FURNACE);
        when(state.getValue(BlockStateProperties.LIT)).thenReturn(true);
        assertFalse(StoveSurfaceLighting.isLitStove(state));
        assertEquals(13, StoveSurfaceLighting.emission(state, world, BlockPos.ZERO));
        assertEquals(.6F, StoveSurfaceLighting.shade(state, world, BlockPos.ZERO));
    }
}
