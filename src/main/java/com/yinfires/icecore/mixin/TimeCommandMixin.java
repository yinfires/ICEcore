package com.yinfires.icecore.mixin;

import com.yinfires.icecore.time.TimeVoteManager;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {
    @Inject(
            method = {
                    "m_139077_(Lnet/minecraft/commands/CommandSourceStack;I)I",
                    "m_139082_(Lnet/minecraft/commands/CommandSourceStack;I)I"
            },
            at = @At("HEAD"),
            require = 1
    )
    private static void icecore$abortCutscene(CommandSourceStack source,int value,CallbackInfoReturnable<Integer> cir){TimeVoteManager.onExternalTimeSet();}
}
