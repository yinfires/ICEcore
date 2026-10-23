package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.cozycafe.customer.CafeCustomerVisit;
import io.github.chakyl.cozycafe.entities.CustomerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="io.github.chakyl.cozycafe.entities.CustomerEntity",remap=false)
public abstract class CozyCafeCustomerIdentityMixin implements CafeCustomerVisit {
    @Unique private static final EntityDataAccessor<CompoundTag> ICECORE_VISIT=SynchedEntityData.defineId(CustomerEntity.class,EntityDataSerializers.COMPOUND_TAG);
    @Inject(method="m_8097_()V",at=@At("TAIL"))
    private void icecore$define(CallbackInfo ci){((CustomerEntity)(Object)this).getEntityData().define(ICECORE_VISIT,new CompoundTag());}
    public CompoundTag icecore$visit(){return ((CustomerEntity)(Object)this).getEntityData().get(ICECORE_VISIT);}
    public void icecore$visit(CompoundTag tag){((CustomerEntity)(Object)this).getEntityData().set(ICECORE_VISIT,tag.copy());}
    @Inject(method="m_7380_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$save(CompoundTag tag,CallbackInfo ci){tag.put("icecoreCustomerVisit",icecore$visit().copy());}
    @Inject(method="m_7378_(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("TAIL"))
    private void icecore$load(CompoundTag tag,CallbackInfo ci){icecore$visit(tag.getCompound("icecoreCustomerVisit"));}
}
