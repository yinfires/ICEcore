package com.yinfires.icecore.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = {
    "dev.xkmc.youkaishomecoming.content.pot.overlay.HintOverlay",
    "dev.xkmc.youkaishomecoming.content.pot.overlay.TileInfoDisplay"
}, remap = false)
@org.spongepowered.asm.mixin.Pseudo
public abstract class WorkstationOverlayMixin {
    @Inject(method = "render(Lnet/minecraftforge/client/gui/overlay/ForgeGui;Lnet/minecraft/client/gui/GuiGraphics;FII)V", at = @At("HEAD"), cancellable = true)
    private void icecore$jadeOnly(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)) return;
        var entity = minecraft.level.getBlockEntity(hit.getBlockPos());
        if (entity == null) return;
        String type = entity.getClass().getSimpleName();
        if (type.equals("CuisineBoardBlockEntity") || type.equals("FermentationTankBlockEntity")) ci.cancel();
    }
}
