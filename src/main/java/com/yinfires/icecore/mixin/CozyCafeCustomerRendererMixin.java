package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerVisit;
import com.yinfires.icecore.compat.cozycafe.customer.client.CafeCustomerSkins;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import io.github.chakyl.cozycafe.util.CustomerSkinInfo;
import com.mojang.authlib.GameProfile;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.entities.renderer.CustomerRenderer",remap=false)
public abstract class CozyCafeCustomerRendererMixin {
    @Unique private CustomerEntity icecore$rendering;
    @Inject(method="getTextureLocation(Lio/github/chakyl/cozycafe/entities/CustomerEntity;)Lnet/minecraft/resources/ResourceLocation;",at=@At("HEAD"),cancellable=true)
    private void icecore$texture(CustomerEntity entity,CallbackInfoReturnable<ResourceLocation> ci){
        if(!((CafeCustomerVisit)entity).icecore$visit().isEmpty())ci.setReturnValue(CafeCustomerSkins.texture(((CafeCustomerVisit)entity).icecore$visit()));
    }
    @Inject(method="render(Lio/github/chakyl/cozycafe/entities/CustomerEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("HEAD"))
    private void icecore$capture(CustomerEntity entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,int light,CallbackInfo ci){icecore$rendering=entity;}
    @Redirect(method="render(Lio/github/chakyl/cozycafe/entities/CustomerEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/util/CustomerSkinUtils;getCustomerSkinInfo(Lcom/mojang/authlib/GameProfile;)Lio/github/chakyl/cozycafe/util/CustomerSkinInfo;"))
    private CustomerSkinInfo icecore$model(GameProfile profile){
        var visit=((CafeCustomerVisit)icecore$rendering).icecore$visit();
        return visit.isEmpty()?io.github.chakyl.cozycafe.util.CustomerSkinUtils.getCustomerSkinInfo(profile):new CustomerSkinInfo(CafeCustomerSkins.slim(visit),CafeCustomerSkins.texture(visit));
    }
    @Inject(method="render(Lio/github/chakyl/cozycafe/entities/CustomerEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At("RETURN"))
    private void icecore$release(CallbackInfo ci){icecore$rendering=null;}
}
