package com.yinfires.icecore.compat.cozycafe.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yinfires.icecore.compat.cozycafe.CafeTableAccess;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import io.github.chakyl.cozycafe.blocks.CafeMenuBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public final class CafeTableRendering {
    private static final Set<String> ICONS=Set.of("cold_cut_ham_slices","oil_splashed_fish","braised_pork_ribs","cold_roasted_meat");
    private CafeTableRendering() {}
    public static boolean icon(ItemStack stack) {
        ResourceLocation id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id!=null && id.getNamespace().equals("kaleidoscope_cookery") && ICONS.contains(id.getPath());
    }
    public static boolean custom(CafeMenuBlockEntity menu) {
        CafeTableAccess table=(CafeTableAccess)menu;
        return !table.icecore$tableRemainder().isEmpty() || !table.icecore$tableDrink() && !table.icecore$tablePlated();
    }
    public static ItemStack nativeFood(CafeMenuBlockEntity menu) {
        return custom(menu) ? ItemStack.EMPTY : menu.getEatingItem();
    }
    /** Native course advances independently of the physical plate used for this serving. */
    public static int displayCourse(CafeMenuBlockEntity menu) {
        CafeTableAccess table=(CafeTableAccess)menu;
        return com.yinfires.icecore.compat.cozycafe.CafeDisplayRules.course(table.icecore$tablePlated(),table.icecore$tableDrink(),menu.getCurrentCourse());
    }
    public static void render(CafeMenuBlockEntity menu, PoseStack pose, MultiBufferSource buffers, int light,int overlay) {
        if (!custom(menu)) return;
        CafeTableAccess table=(CafeTableAccess)menu;
        ItemStack stack=table.icecore$tableRemainder().isEmpty() ? menu.getEatingItem() : table.icecore$tableRemainder();
        if (stack.isEmpty()) return;
        var mc=Minecraft.getInstance();
        var facing=menu.getBlockState().getValue(CafeMenuBlock.FACING).getOpposite();
        pose.pushPose();
        if(table.icecore$tableDrink()) {
            // The complete native drink branch, independent of the next requested course.
            pose.translate(.45F+facing.getStepX()*.35F+facing.getClockWise().getStepX()*.3F,0,
                .45F+facing.getStepZ()*.35F+facing.getClockWise().getStepZ()*.3F);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()-20));
            if (!(stack.getItem() instanceof BlockItem)) {
                pose.translate(0,.25F,0);
                pose.mulPose(Axis.XP.rotationDegrees(-90));
            }
            pose.scale(.65F,.65F,.65F);
            io.github.chakyl.cozycafe.util.GeneralUtils.renderFood(stack,pose,buffers,menu,light,overlay);
            pose.popPose();
            return;
        }
        pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        boolean block=stack.getItem() instanceof BlockItem && !icon(stack);
        if(block) {
            float scale=.9F;
            BakedModel model=mc.getBlockRenderer().getBlockModel(((BlockItem)stack.getItem()).getBlock().defaultBlockState());
            pose.translate(0,-scale*CafeModelBounds.minimumY(model,ItemDisplayContext.NONE,true),0);
            pose.scale(scale,scale,scale);
            // Use the same native block-food renderer as CozyCafe.
            io.github.chakyl.cozycafe.util.GeneralUtils.renderFood(stack,pose,buffers,menu,light,overlay);
        } else {
            var renderer=mc.getItemRenderer();
            BakedModel model=renderer.getModel(stack,menu.getLevel(),null,0);
            boolean solid=model.isGui3d() && !icon(stack);
            ItemDisplayContext context=solid ? ItemDisplayContext.NONE : ItemDisplayContext.FIXED;
            float scale=solid ? .9F : .5F;
            pose.translate(0,-scale*CafeModelBounds.minimumY(model,context,false),0);
            pose.scale(scale,scale,scale);
            renderer.render(stack,context,false,pose,buffers,light,overlay,model);
        }
        pose.popPose();
    }
}
