package com.yinfires.icecore.mixin;

import com.yinfires.icecore.config.ICECoreClientConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Chinese Food uses Cookery FoodWithEffectsItem; only intercept its effect builder. */
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.FoodWithEffectsItem", remap = false)
public abstract class CookeryFoodTooltipMixin {
    @Redirect(method = "m_7373_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionUtils;m_257410_(Ljava/util/List;Ljava/util/List;F)V", remap = false), remap = false)
    private void icecore$hideFoodEffects(List<MobEffectInstance> effects, List<Component> lines, float scale) {
        if (!ICECoreClientConfig.hideFoodEffects()) PotionUtils.addPotionTooltip(effects, lines, scale);
    }
}
