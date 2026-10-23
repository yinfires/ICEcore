package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerClientState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.item.CafeManagerItem",remap=false)
public abstract class CozyCafeCustomerItemMixin {
    @Inject(method="m_7373_(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V",at=@At("TAIL"))
    private void icecore$sharedTotal(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag,CallbackInfo ci){
        lines.removeIf(line->line.getContents() instanceof TranslatableContents text && (text.getKey().equals("tooltip.cozycafe.cafe_manager.reputation")||text.getKey().equals("tooltip.cozycafe.cafe_manager.no_reputation")));
        lines.add(Component.translatable("icecore.cafe.total",CafeCustomerClientState.total()).withStyle(ChatFormatting.GRAY));
    }
}
