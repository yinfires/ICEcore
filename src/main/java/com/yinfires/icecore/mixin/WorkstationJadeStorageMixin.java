package com.yinfires.icecore.mixin;

import com.yinfires.icecore.compat.jade.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.*;
import snownee.jade.api.view.ViewGroup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Selects one standard storage projection before upstream/fallback providers can duplicate it. */
@Pseudo
@Mixin(targets = {"snownee.jade.addon.universal.ItemStorageProvider", "snownee.jade.addon.universal.FluidStorageProvider"}, remap = false)
public abstract class WorkstationJadeStorageMixin {
    @Inject(method = "putData(Lsnownee/jade/api/Accessor;)V", at = @At("HEAD"), cancellable = true)
    private static void icecore$storage(Accessor<?> accessor, CallbackInfo ci) {
        Object target = accessor.getTarget();
        if (accessor instanceof BlockAccessor block && net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(block.getBlock()).toString().equals("kaleidoscope_tavern:barrel")) target = TavernJadeAccess.origin(block);
        if (!(target instanceof BlockEntity entity) || !WorkstationJadeProvider.supported(entity)) return;
        ci.cancel();
        var data = accessor.getServerData();
        if (data.getBoolean("ICEcoreStorageProjected")) return;
        data.putBoolean("ICEcoreStorageProjected", true);
        var player = (ServerPlayer)accessor.getPlayer();
        var level = player.serverLevel();
        var items = WorkstationItemStorage.INSTANCE.getGroups(player, level, entity, accessor.showDetails());
        data.remove("JadeItemStorage"); data.remove("JadeItemStorageUid");
        if (ViewGroup.saveList(data, "JadeItemStorage", items, item -> item.save(new net.minecraft.nbt.CompoundTag())))
            data.putString("JadeItemStorageUid", WorkstationItemStorage.INSTANCE.getUid().toString());
        var fluids = WorkstationFluidStorage.INSTANCE.getGroups(player, level, entity, accessor.showDetails());
        data.remove("JadeFluidStorage"); data.remove("JadeFluidStorageUid");
        if (ViewGroup.saveList(data, "JadeFluidStorage", fluids, fluid -> fluid))
            data.putString("JadeFluidStorageUid", WorkstationFluidStorage.INSTANCE.getUid().toString());
    }
}
