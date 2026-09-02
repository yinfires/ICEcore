package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.CozyCafeMenuStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** Exposes CozyCafe's synchronized manager-menu state without a hard compile dependency. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.gui.CafeManagerMenu", remap = false)
public abstract class CozyCafeManagerMenuStateMixin implements CozyCafeMenuStateAccess {
    @Shadow
    public abstract boolean getIsCafeOpen();

    @Override
    public boolean icecore$isCafeOpen() {
        return getIsCafeOpen();
    }
}
