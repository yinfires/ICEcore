package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.customer.*;
import com.yinfires.icecore.compat.cozycafe.seating.CozyCafeSeatingService;
import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;
import io.github.chakyl.cozycafe.blockentities.*;
import io.github.chakyl.cozycafe.data.CafeMenuItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Owns admission while retaining native computer/sign updates and menu execution. */
@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.blockentities.CafeManagerBlockEntity",remap=false,priority=1200)
public abstract class CozyCafeCustomerManagerMixin extends BlockEntity implements CafeCustomerManagerAccess {
    @Shadow private BlockPos getFirstPos(BlockState state,BlockPos pos){throw new AssertionError();}
    @Shadow private BlockPos getSecondPos(BlockState state,BlockPos pos){throw new AssertionError();}
    protected CozyCafeCustomerManagerMixin(BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    @Unique private CafeManagerBlockEntity icecore$self(){return (CafeManagerBlockEntity)(Object)this;}
    @Inject(method="addToMenu(Lnet/minecraft/world/item/ItemStack;)Z",at=@At("RETURN"))
    private void icecore$addedMenu(ItemStack stack,CallbackInfoReturnable<Boolean> ci){CafeCustomers.invalidateMenu(icecore$self());}
    @Inject(method={"removeFromMenu(I)V","clearCafeData()V","m_142466_(Lnet/minecraft/nbt/CompoundTag;)V"},at=@At("RETURN"))
    private void icecore$changedMenu(CallbackInfo ci){CafeCustomers.invalidateMenu(icecore$self());}
    @Override public void icecore$restoreCustomerRoutes(){
        if(!(level instanceof ServerLevel serverLevel))return;
        var entrances=CozyCafeSpawnRegionService.entranceCandidates(serverLevel,worldPosition);
        if(entrances.isEmpty())entrances=CozyCafeSeatingService.defaultEntrance(icecore$self());
        if(CafeCustomers.owns(icecore$self())&&CafeCustomers.data().active)
            CozyCafeSeatingService.restoreRouteSnapshot(icecore$self(),CafeCustomers.data().seats.stream().map(BlockPos::of).toList(),entrances);
        else CozyCafeSeatingService.buildRouteSnapshot(icecore$self(),getFirstPos(getBlockState(),worldPosition),getSecondPos(getBlockState(),worldPosition),entrances);
    }
    @Inject(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$admissionTick(Level l,BlockPos p,BlockState s,CallbackInfo ci){
        if(!l.isClientSide)CafeCustomers.tick(icecore$self());
        ci.cancel();
    }
    @Inject(method="assignCustomersInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$noNativeAdmission(CallbackInfo ci){ci.cancel();}
    @Inject(method="canBeOpened(Lnet/minecraft/server/level/ServerPlayer;)Z",at=@At("HEAD"),cancellable=true)
    private void icecore$opening(ServerPlayer player,CallbackInfoReturnable<Boolean> ci){
        if(level instanceof ServerLevel serverLevel&&CozyCafeSpawnRegionService.hasValidRegion(serverLevel,worldPosition))return;
        byte failure=CafeCustomers.openingFailure(icecore$self());
        if(failure<0){
            icecore$restoreCustomerRoutes();
            if(CozyCafeSeatingService.snapshotMenus(player.serverLevel(),worldPosition).isEmpty())failure=5;
        }
        if(failure>=0)io.github.chakyl.cozycafe.network.EvilPacketsIHateThem.sendToPlayer(new io.github.chakyl.cozycafe.network.ClientBoundCafeCannotOpenPacket(failure),player);
        ci.setReturnValue(failure<0);
    }
    @Inject(method="setOpen(ZZ)V",at=@At("HEAD"),cancellable=true)
    private void icecore$sessionTransition(boolean open,boolean force,CallbackInfo ci){
        if(!(level instanceof ServerLevel))return;
        if(CozyCafeSpawnRegionService.hasValidRegion((ServerLevel)level,worldPosition))return;
        boolean was=icecore$self().isOpen();
        if(open&&!was){
            if(CafeCustomers.openingFailure(icecore$self())>=0){ci.cancel();return;}
            if(!CozyCafeSeatingService.hasRouteSnapshot((ServerLevel)level,worldPosition))icecore$restoreCustomerRoutes();
            CafeCustomers.opened(icecore$self());
        }else if(!open&&was)CafeCustomers.closed(icecore$self());
    }
    @Inject(method="sendCloseCommandToMenus(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$stopAdmissionsOnly(CallbackInfo ci){ci.cancel();}
    @Inject(method="getReputation()I",at=@At("HEAD"),cancellable=true)
    private void icecore$total(CallbackInfoReturnable<Integer> ci){ci.setReturnValue(level!=null&&level.isClientSide?CafeCustomerClientState.total():CafeCustomers.total());}
    @Inject(method="getStarsFromReputation()I",at=@At("HEAD"),cancellable=true)
    private void icecore$stars(CallbackInfoReturnable<Integer> ci){int total=level!=null&&level.isClientSide?CafeCustomerClientState.total():CafeCustomers.total();ci.setReturnValue(Math.min(5,total/120));}
    @Redirect(method={"getFirstPos(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;","getSecondPos(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;"},at=@At(value="INVOKE",target="Lio/github/chakyl/cozycafe/blockentities/CafeManagerBlockEntity;getStarsFromReputation()I"))
    private int icecore$cosmeticStars(CafeManagerBlockEntity manager){return 0;}
    @Inject(method={"setReputation(I)V","increaseReputation(I)V","decreaseReputation(I)V"},at=@At("HEAD"),cancellable=true)
    private void icecore$ignoreLegacyReputation(CallbackInfo ci){ci.cancel();}
    @Inject(method="handleSuccessfulServe(Lio/github/chakyl/cozycafe/data/CafeMenuItem;Lnet/minecraft/world/item/ItemStack;D)V",at=@At("HEAD"),cancellable=true)
    private void icecore$noLegacyServe(CallbackInfo ci){ci.cancel();}
    @Inject(method="rollMenuCourse(Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$chooseCourse(CafeMenuBlockEntity table,CallbackInfo ci){CafeCustomers.chooseCourse(icecore$self(),table);ci.cancel();}
}
