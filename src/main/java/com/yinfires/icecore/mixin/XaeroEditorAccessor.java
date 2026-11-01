package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Pinned Xaero 1.47.0 camera, in physical pixels per block. */
@Mixin(targets="xaero.map.gui.GuiMap", remap=false)
public interface XaeroEditorAccessor {
    @Accessor("cameraX") double icecore$cameraX();
    @Accessor("cameraZ") double icecore$cameraZ();
    @Accessor("cameraX") void icecore$cameraX(double value);
    @Accessor("cameraZ") void icecore$cameraZ(double value);
    @Accessor("scale") double icecore$scale();
    @Accessor("attachedCamera") static boolean icecore$attached(){throw new AssertionError();}
    @Accessor("attachedCamera") static void icecore$attached(boolean value){throw new AssertionError();}
}
