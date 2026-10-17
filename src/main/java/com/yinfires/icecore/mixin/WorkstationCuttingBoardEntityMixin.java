package com.yinfires.icecore.mixin;

import com.yinfires.icecore.workstation.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

@Pseudo
@Mixin(targets = "vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity", remap = false)
public abstract class WorkstationCuttingBoardEntityMixin implements CuttingBoardAccess {
    @Shadow @Final private ItemStackHandler inventory;
    @Shadow private boolean isItemCarvingBoard;
    @Unique private final List<ItemStack> icecore$results = new ArrayList<>();
    @Unique private final WorkstationSlotState icecore$metadata = new WorkstationSlotState();
    @Unique private final WorkstationSlotState icecore$inputMetadata = new WorkstationSlotState();
    public List<ItemStack> icecore$products() { return icecore$results; }
    public WorkstationSlotState icecore$containers() { return icecore$results.isEmpty() ? icecore$inputMetadata : icecore$metadata; }
    public ItemStackHandler icecore$inventory() { return inventory; }
    public boolean icecore$carved() { return isItemCarvingBoard; }
    public void icecore$uncarve() { isItemCarvingBoard = false; }

    @Inject(method = "createHandler()Lnet/minecraftforge/items/ItemStackHandler;", at = @At("HEAD"), cancellable = true)
    private void icecore$singleInput(CallbackInfoReturnable<ItemStackHandler> cir) {
        cir.setReturnValue(new CuttingBoardInputHandler((BlockEntity)(Object)this));
    }
    @Inject(method = "processStoredItemUsingTool(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void icecore$finished(ItemStack tool, Player player, CallbackInfoReturnable<Boolean> cir) {
        BlockEntity entity = (BlockEntity)(Object)this;
        if (!icecore$results.isEmpty() || entity.getLevel() == null || entity.getLevel().isClientSide) cir.setReturnValue(false);
    }
    @Redirect(method = "lambda$processStoredItemUsingTool$2(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lvectorwing/farmersdelight/common/crafting/CuttingBoardRecipe;)V",
            at = @At(value = "INVOKE", target = "Lvectorwing/farmersdelight/common/crafting/CuttingBoardRecipe;rollResults(Lnet/minecraft/util/RandomSource;ILnet/minecraftforge/items/wrapper/RecipeWrapper;)Ljava/util/List;"))
    private List<ItemStack> icecore$retain(@Coerce Object recipe, RandomSource random, int fortune, RecipeWrapper wrapper) {
        @SuppressWarnings("unchecked") List<ItemStack> results = (List<ItemStack>)ExternalWorkstation.invoke(recipe, "rollResults",
                new Class<?>[]{RandomSource.class, int.class, RecipeWrapper.class}, random, fortune, wrapper);
        for (ItemStack result : results) if (!result.isEmpty()) icecore$results.add(result.copy());
        icecore$metadata.reconcile(icecore$results.size(), icecore$results::get);
        return List.of(); // Original durability, particles, sound, ingredient consumption and advancement still run.
    }
    @Redirect(method = {
            "lambda$processStoredItemUsingTool$2(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lvectorwing/farmersdelight/common/crafting/CuttingBoardRecipe;)V",
            "getMatchingRecipe(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;m_5661_(Lnet/minecraft/network/chat/Component;Z)V"))
    private void icecore$silent(Player player, Component text, boolean actionBar) {}

    @Inject(method = "m_183515_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void icecore$save(CompoundTag tag, CallbackInfo ci) {
        ListTag results = new ListTag();
        for (ItemStack item : icecore$results) results.add(item.save(new CompoundTag()));
        tag.put("ICEcoreCuttingProducts", results); icecore$metadata.save(tag);
        CompoundTag input = new CompoundTag(); icecore$inputMetadata.save(input); tag.put("ICEcoreCuttingInput", input);
    }
    @Inject(method = "m_142466_(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void icecore$load(CompoundTag tag, CallbackInfo ci) {
        icecore$results.clear();
        ListTag results = tag.getList("ICEcoreCuttingProducts", 10);
        for (int i = 0; i < results.size(); i++) icecore$results.add(ItemStack.of(results.getCompound(i)));
        icecore$metadata.load(tag);
        icecore$inputMetadata.load(tag.getCompound("ICEcoreCuttingInput"));
    }
    @Inject(method = "m_6211_()V", at = @At("HEAD"))
    private void icecore$clear(CallbackInfo ci) { icecore$results.clear(); isItemCarvingBoard = false; icecore$metadata.load(new CompoundTag()); icecore$inputMetadata.load(new CompoundTag()); }
}
