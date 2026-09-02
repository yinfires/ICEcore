package com.yinfires.icecore.mixin;

import com.mojang.authlib.GameProfile;
import com.yinfires.icecore.compat.cozycafe.CozyCafeManagerStateAccess;
import com.yinfires.icecore.compat.cozycafe.spawn.CozyCafeSpawnRegionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recovers tables persisted in the active-customer state after an old CozyCafe clear crash. */
@Pseudo
@Mixin(targets = "io.github.chakyl.cozycafe.blockentities.CafeMenuBlockEntity", remap = false)
public abstract class CozyCafeOrphanedMenuMixin {
    @Shadow private boolean hasCustomer;
    @Shadow private int waitTime;
    @Shadow private int orderTime;
    @Shadow private int customerTravelTime;
    @Shadow private int currentCourse;
    @Shadow private BlockPos cafeManager;
    @Shadow private String customerSkin;
    @Shadow private GameProfile gameProfile;
    @Shadow private ItemStack requestedItem;
    @Shadow private ItemStack eatingItem;

    @Inject(method = "closeMenu(Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void icecore$recoverOrphanedCustomer(boolean forced, CallbackInfo callback) {
        if (!hasCustomer) {
            return;
        }

        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockEntity manager = cafeManager == null ? null : level.getBlockEntity(cafeManager);
        if (manager instanceof CozyCafeManagerStateAccess access) {
            if (access.icecore$getLinkedSign() != null) return;
            if (level instanceof ServerLevel serverLevel
                    && CozyCafeSpawnRegionService.hasValidRegion(serverLevel, manager.getBlockPos())) return;
        }

        hasCustomer = false;
        waitTime = -1;
        orderTime = -1;
        customerTravelTime = -1;
        currentCourse = 0;
        customerSkin = "";
        gameProfile = null;
        requestedItem = ItemStack.EMPTY;
        eatingItem = ItemStack.EMPTY;

        BlockState state = self.getBlockState();
        BooleanProperty dish = state.getProperties().stream()
                .filter(property -> property instanceof BooleanProperty && property.getName().equals("dish"))
                .map(property -> (BooleanProperty) property)
                .findFirst()
                .orElse(null);
        if (dish != null && state.getValue(dish)) {
            level.setBlock(self.getBlockPos(), state.setValue(dish, false), 3);
        }
        self.setChanged();
        callback.cancel();
    }

}
