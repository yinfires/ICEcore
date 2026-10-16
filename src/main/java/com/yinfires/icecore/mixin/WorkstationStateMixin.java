package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = {
    "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.PotBlockEntity",
    "com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity",
    "dev.xkmc.youkaishomecoming.content.pot.ferment.FermentationTankBlockEntity",
    "com.bmt.kaleidoscope_world_liquor.block.entity.FreezerBlockEntity"
}, remap = false)
@org.spongepowered.asm.mixin.Pseudo
public abstract class WorkstationStateMixin implements WorkstationAccess {
    @Unique private final WorkstationSlotState icecore$state = new WorkstationSlotState();
    public WorkstationSlotState icecore$slots() { return icecore$state; }
}
