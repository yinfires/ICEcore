package com.yinfires.icecore.mixin;

import org.spongepowered.asm.mixin.Mixin;

/** Marker for the server-only preApply repair of upstream sound links. */
@Mixin(targets = "com.bmt.kaleidoscope_fragrantorchard.block.entity.JuicerBlockEntity", remap = false)
public abstract class WorkstationJuicerServerSoundMixin {}
