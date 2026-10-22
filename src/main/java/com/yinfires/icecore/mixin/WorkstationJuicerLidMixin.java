package com.yinfires.icecore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import com.yinfires.icecore.workstation.ExternalWorkstation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gate native extraction and preserve lid access even with mixed results waiting. */
@Mixin(targets="com.bmt.kaleidoscope_fragrantorchard.block.JuicerBlock",remap=false)
public abstract class WorkstationJuicerLidMixin {
    @Shadow @Final public static BooleanProperty OPEN;
    @Inject(method="m_6227_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",at=@At("HEAD"),cancellable=true)
    private void icecore$lid(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir) {
        boolean open=state.getValue(OPEN);
        if(!player.isShiftKeyDown()) {
            if(!open)cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
            return;
        }
        cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        if(level.isClientSide)return;
        var entity=level.getBlockEntity(pos);
        if(entity==null || (Boolean)ExternalWorkstation.call(entity,"isCrafting"))return;
        if(!open && level.getBlockState(pos.above()).isRedstoneConductor(level,pos.above()))return;
        if(open)ExternalWorkstation.call(entity,"tryStartCrafting");
        level.setBlock(pos,state.setValue(OPEN,!open),3);
        level.playSound(null,pos,open?SoundEvents.BARREL_CLOSE:SoundEvents.BARREL_OPEN,SoundSource.BLOCKS,1,1);
    }
}
