package com.yinfires.icecore.client;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.yinfires.icecore.workstation.CuttingBoardAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemStackHandler;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class CuttingBoardItemsTest {
    @org.junit.jupiter.api.BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try (var hooks = mockStatic(net.minecraftforge.network.NetworkHooks.class)) {
            net.minecraft.server.Bootstrap.bootStrap();
        }
    }

    private static ItemTransform generatedFixed() throws Exception {
        // Read the real Minecraft model, including its inherited FIXED rotation.
        try (var stream = CuttingBoardItemsTest.class.getResourceAsStream("/assets/minecraft/models/item/generated.json")) {
            assertNotNull(stream);
            var json = com.google.gson.JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return new GsonBuilder().registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer()).create()
                    .fromJson(json.getAsJsonObject("display").get("fixed"), ItemTransform.class);
        }
    }

    private static List<Matrix4f> render(Direction direction, boolean threeDimensional, boolean cookery,
                                          boolean products, boolean emptyMiddle) throws Exception {
        BlockEntity entity = mock(BlockEntity.class, withSettings().extraInterfaces(CuttingBoardAccess.class));
        CuttingBoardAccess board = (CuttingBoardAccess) entity;
        BlockState state = mock(BlockState.class);
        when(entity.getBlockState()).thenReturn(state);
        when(state.getValue(BlockStateProperties.HORIZONTAL_FACING)).thenReturn(direction);
        when(entity.getBlockPos()).thenReturn(new BlockPos(7, 12, 19));
        ItemStack item = mock(ItemStack.class);
        ItemStack empty = mock(ItemStack.class);
        when(empty.isEmpty()).thenReturn(true);
        when(board.icecore$products()).thenReturn(products ? List.of(item, emptyMiddle ? empty : item, item) : List.of());
        ItemStackHandler inventory = mock(ItemStackHandler.class);
        when(board.icecore$inventory()).thenReturn(inventory);
        when(inventory.getStackInSlot(0)).thenReturn(item);
        BakedModel model = mock(BakedModel.class);
        when(model.isGui3d()).thenReturn(threeDimensional);
        Minecraft minecraft = mock(Minecraft.class);
        ItemRenderer renderer = mock(ItemRenderer.class);
        when(minecraft.getItemRenderer()).thenReturn(renderer);
        when(renderer.getModel(eq(item), isNull(), isNull(), eq(0))).thenReturn(model);
        ModList mods = mock(ModList.class);
        when(mods.isLoaded("kaleidoscope_cookery")).thenReturn(cookery);
        ItemTransform fixed = threeDimensional ? ItemTransform.NO_TRANSFORM : generatedFixed();
        List<Matrix4f> captured = new ArrayList<>();
        doAnswer(call -> {
            assertEquals(ItemDisplayContext.FIXED, call.getArgument(1));
            assertEquals(false, call.getArgument(2));
            assertSame(model, call.getArgument(7));
            PoseStack pose = call.getArgument(3);
            // ItemRenderer applies the model camera transform before its centering translation.
            pose.pushPose();
            fixed.apply(false, pose);
            pose.translate(-.5, -.5, -.5);
            captured.add(new Matrix4f(pose.last().pose()));
            pose.popPose();
            return null;
        }).when(renderer).render(eq(item), any(), eq(false), any(), any(), eq(15728880), eq(0), eq(model));
        try (var mc = mockStatic(Minecraft.class); var ml = mockStatic(ModList.class);
             var bridge = mockStatic(CuttingBoardCookeryModel.class)) {
            mc.when(Minecraft::getInstance).thenReturn(minecraft);
            ml.when(ModList::get).thenReturn(mods);
            bridge.when(() -> CuttingBoardCookeryModel.model(entity, item)).thenReturn(model);
            PoseStack pose = new PoseStack();
            CuttingBoardItems.render(entity, pose, mock(MultiBufferSource.class), 15728880, 0);
            assertEquals(new Matrix4f(), pose.last().pose(), "No transform may leak to another block");
            if (cookery) bridge.verify(() -> CuttingBoardCookeryModel.model(entity, item), times(captured.size()));
            else bridge.verifyNoInteractions();
        }
        return captured;
    }

    @Test void asymmetricSpriteUvFacesUpForAllDirectionsAndLayers() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) for (boolean cookery : new boolean[]{false, true})
            for (boolean products : new boolean[]{false, true}) {
                var transforms = render(facing, false, cookery, products, false);
                assertEquals(products ? 3 : 1, transforms.size());
                var random = net.minecraft.util.RandomSource.create(new BlockPos(7, 12, 19).asLong());
                for (int i = 0; i < transforms.size(); i++) {
                    int angle = 90 + random.nextInt(90);
                    // Independently place three unequal UV-labelled landmarks of an asymmetric sprite.
                    // GUI's front is +Z; the UV-right vector is +X and UV-up is +Y.
                    Matrix4f expected = new Matrix4f().translation(.5f, .1f + i * .0125f, .5f)
                            .rotateY((float) Math.toRadians(facing.get2DDataValue() * 90))
                            .rotateX(-(float) Math.PI / 2).scale(.5f)
                            .rotateZ((float) Math.toRadians(i * angle)).translate(-.5f, -.5f, -.5f);
                    for (Vector3f uvLandmark : new Vector3f[]{new Vector3f(.1f, .2f, .5f),
                            new Vector3f(.9f, .2f, .5f), new Vector3f(.25f, .8f, .5f)}) {
                        assertTrue(expected.transformPosition(new Vector3f(uvLandmark))
                                .equals(transforms.get(i).transformPosition(new Vector3f(uvLandmark)), .00001f));
                    }
                    Vector3f normal = transforms.get(i).transformDirection(new Vector3f(0, 0, 1)).normalize();
                    assertEquals(1, normal.y, .00001f, "The textured front must face the viewer above the board");
                }
            }
    }

    @Test void threeDimensionalModelsKeepTheirExistingTransforms() throws Exception {
        for (Direction facing : Direction.Plane.HORIZONTAL) for (boolean cookery : new boolean[]{false, true}) {
            var transforms = render(facing, true, cookery, true, false);
            var random = net.minecraft.util.RandomSource.create(new BlockPos(7, 12, 19).asLong());
            for (int i = 0; i < 3; i++) {
                int angle = 90 + random.nextInt(90);
                Matrix4f expected = new Matrix4f().translation(.5f, .1f, .5f)
                        .rotateY((float) Math.toRadians(facing.get2DDataValue() * 90))
                        .rotateX(-(float) Math.PI / 2).scale(.5f).translate(0, 0, i * .025f)
                        .rotateZ((float) Math.toRadians(i * angle)).translate(-.5f, -.5f, -.5f);
                assertTrue(expected.equals(transforms.get(i), .00001f));
            }
        }
    }

    @Test void emptyProductsKeepFollowingLayerAndSeededRotation() throws Exception {
        var full = render(Direction.NORTH, false, false, true, false);
        var gap = render(Direction.NORTH, false, false, true, true);
        assertEquals(2, gap.size());
        assertEquals(full.get(0), gap.get(0));
        assertEquals(full.get(2), gap.get(1));
    }

    @Test void previousTransformShowsTheBackOfTheAsymmetricSprite() throws Exception {
        ItemTransform fixed = generatedFixed();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            PoseStack previous = new PoseStack();
            previous.mulPose(com.mojang.math.Axis.YP.rotationDegrees(facing.get2DDataValue() * 90));
            previous.mulPose(com.mojang.math.Axis.XN.rotationDegrees(90));
            fixed.apply(false, previous);
            Vector3f normal = previous.last().pose().transformDirection(new Vector3f(0, 0, 1));
            assertEquals(-1, normal.y, .00001f, "Negative control: old transform faces down");
            Vector3f uvRight = previous.last().pose().transformDirection(new Vector3f(1, 0, 0));
            Vector3f expectedRight = new Matrix4f().rotateY((float) Math.toRadians(facing.get2DDataValue() * 90))
                    .transformDirection(new Vector3f(1, 0, 0));
            assertEquals(-1, uvRight.dot(expectedRight), .00001f, "Negative control: UV right becomes left");
        }
    }
}
