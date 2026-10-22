package com.yinfires.icecore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.yinfires.icecore.compat.cozycafe.CafeDisplayRules;
import com.yinfires.icecore.compat.cozycafe.client.CafeModelBounds;
import com.yinfires.icecore.compat.cozycafe.client.CafeTableRendering;
import io.github.chakyl.cozycafe.util.GeneralUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** Native composite plate model, with the same block-food layout as placed plates. */
@Mixin(targets="io.github.chakyl.cozycafe.client.model.ServingPlateModel",remap=false)
public abstract class CozyCafePlateItemModelMixin {
    @Shadow @Final private BakedModel plateModel;
    @Shadow @Final private ItemStack food;

    @Inject(method="getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/minecraftforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",at=@At("HEAD"),cancellable=true)
    private void icecore$fit(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type,CallbackInfoReturnable<List<BakedQuad>> cir) {
        if(food==null || food.isEmpty()) return;
        BakedModel model=com.yinfires.icecore.compat.cozycafe.client.CafePlatedFoodModels.model(food);
        if(model==(Object)this) return;
        var key=new com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.Key(plateModel,model,type,side);
        boolean cache=state==null && data==ModelData.EMPTY;
        if(cache) {
            var cached=com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.get(key);
            if(cached!=null) { cir.setReturnValue(cached); return; }
        }
        PoseStack pose=new PoseStack();
        pose.translate(.5,2.0/16+.001,.5);
        var transform=QuadTransformers.applying(new Transformation(pose.last().pose()));
        List<BakedQuad> result=new ArrayList<>(plateModel.getQuads(state,side,random,data,type));
        for(BakedQuad quad:model.getQuads(state,side,random,data,type)) result.add(transform.process(quad));
        List<BakedQuad> immutable=List.copyOf(result);
        if(cache) com.yinfires.icecore.compat.cozycafe.client.CafePlateItemCache.put(key,immutable);
        cir.setReturnValue(immutable);
    }
}
