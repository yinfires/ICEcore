package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.*;
import com.yinfires.icecore.compat.cozycafe.customer.*;
import io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity",remap=false,priority=1050)
public abstract class CozyCafeCustomerMenuMixin implements CafeCustomerVisit {
    @Unique private CompoundTag icecore$visit=new CompoundTag();
    @Unique private boolean icecore$available;
    @Shadow private void setChangedForRender(){throw new AssertionError();}
    public CompoundTag icecore$visit(){return icecore$visit;}
    public void icecore$visit(CompoundTag value){icecore$visit=value;setChangedForRender();}
    @Redirect(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At(value="FIELD",target="Lio/github/chakyl/cozycafe/blockentities/CafeMenuBlockEntity;MAX_WAIT_TIME:I",opcode=178))
    private int icecore$orderPatience(){
        return !icecore$visit.isEmpty()&&!icecore$visit.getBoolean("Settled")?Math.max(1,icecore$visit.getInt("Patience")):CafeMenuBlockEntity.getMaxWaitTime();
    }
    @Inject(method="handleServe(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",at=@At("HEAD"),cancellable=true)
    private void icecore$deadlineBeforeServe(CallbackInfo ci){
        CafeMenuBlockEntity self=(CafeMenuBlockEntity)(Object)this;
        if(self.getLevel() instanceof net.minecraft.server.level.ServerLevel&&!icecore$visit.isEmpty()&&!icecore$visit.getBoolean("Settled")
                &&self.getWaitTime()>=Math.max(1,icecore$visit.getInt("Patience"))){self.closeMenu(false);ci.cancel();}
    }
    @Inject(method="tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",at=@At("TAIL"))
    private void icecore$seatState(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state,CallbackInfo ci){
        if(level.isClientSide)return;
        CafeMenuBlockEntity self=(CafeMenuBlockEntity)(Object)this;
        boolean available=!self.getHasCustomer()&&self.getCustomerTravelTime()==-1
                &&((com.yinfires.icecore.compat.cozycafe.seating.CozyCafeMenuSeatAccess)self).icecore$getCustomerUuid()==null
                &&!((CafeTableAccess)self).icecore$hasContainerContent();
        if(available!=icecore$available){icecore$available=available;CafeCustomers.seatChanged(level,pos);}
    }
    @Inject(method="orderedDessert(I)Z",at=@At("HEAD"),cancellable=true)
    private void icecore$plannedCourses(int course,CallbackInfoReturnable<Boolean> ci){if(!icecore$visit.isEmpty())ci.setReturnValue(true);}
    @Inject(method="canReceiveNewChoice()Z",at=@At("RETURN"),cancellable=true)
    private void icecore$clearBeforeOrder(CallbackInfoReturnable<Boolean> ci){
        CafeMenuBlockEntity self=(CafeMenuBlockEntity)(Object)this;
        // Eating is native-timed: finish() runs at ORDER_TIME before this query. Retained vessels block the next order.
        boolean remaining=CafeCustomerRules.hasLaterCourse(icecore$visit.getInt("Courses"),self.getCurrentCourse());
        if(remaining&&!((CafeTableAccess)self).icecore$tableRemainder().isEmpty())ci.setReturnValue(false);
    }
    @Inject(method="closeMenu(Z)V",at=@At("HEAD"))
    private void icecore$timeout(boolean gentle,CallbackInfo ci){if(!gentle)CafeCustomers.timeout((CafeMenuBlockEntity)(Object)this);}
    @Inject(method="m_183515_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$save(CompoundTag tag,CallbackInfo ci){tag.put("icecoreCustomerVisit",icecore$visit.copy());}
    @Inject(method="m_142466_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$load(CompoundTag tag,CallbackInfo ci){icecore$visit=tag.getCompound("icecoreCustomerVisit").copy();}
}
