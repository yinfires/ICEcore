package com.yinfires.icecore.compat.cozycafe.client;

import com.yinfires.icecore.compat.cozycafe.CafeTableAccess;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.*;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

/** Hide only the menu's baked geometry; the native block-entity renderer stays active. */
@Mod.EventBusSubscriber(modid="icecore",value=net.minecraftforge.api.distmarker.Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CafeMenuModels {
    private static final ModelProperty<Boolean> HIDDEN=new ModelProperty<>();
    private CafeMenuModels() {}
    @SubscribeEvent public static void bake(ModelEvent.ModifyBakingResult event) {
        CafeModelBounds.clear();
        CafePlateItemCache.clear();
        CafePlatedFoodModels.clear();
        event.getModels().replaceAll((id,model)->id.getNamespace().equals("cozycafe") && id.getPath().equals("cafe_menu") ? new Menu(model) : model);
    }
    private static final class Menu extends BakedModelWrapper<BakedModel> {
        Menu(BakedModel model) { super(model); }
        @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data) {
            ModelData nativeData=originalModel.getModelData(level,pos,state,data);
            return nativeData.derive().with(HIDDEN,level.getBlockEntity(pos) instanceof CafeTableAccess table && table.icecore$hideMenu()).build();
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,net.minecraft.client.renderer.RenderType type) {
            return Boolean.TRUE.equals(data.get(HIDDEN)) ? List.of() : originalModel.getQuads(state,side,random,data,type);
        }
    }
}
