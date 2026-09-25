package com.yinfires.icecore.mixing;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class MixingBowlImplementationTest {
    @Test void recipeSerializerAndNineSlotStateRemainBounded() throws IOException {
        String recipe = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlRecipe.java");
        String entity = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlockEntity.java");
        assertTrue(recipe.contains("inputJson.size() + (hasFluid ? 1 : 0) > 9"));
        assertTrue(recipe.contains("fluid.stack().getAmount() != actualFluid.getAmount()"));
        assertTrue(recipe.contains("Fluid containers must be declared in the mixing bowl fluid field"));
        assertTrue(recipe.contains("outputJson.size() > 9"));
        assertTrue(entity.contains("NonNullList.withSize(9"));
        assertTrue(entity.contains("new FluidTank(1000)"));
        assertTrue(entity.contains("inputCount() + (fluid.isEmpty() ? 0 : 1)"));
        assertTrue(entity.contains("progress >= 50"));
    }

    @Test void fluidRendersInWorldWhileJeiKeepsTheDeclaredBucketItem() throws IOException {
        String renderer = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlRenderer.java");
        String jei = read("src/main/java/com/yinfires/icecore/mixing/client/MixingBowlJeiPlugin.java");
        assertTrue(renderer.contains("FluidRenderer.renderFluidBox"));
        assertTrue(renderer.contains("fluid.getAmount() / 1000F"));
        assertTrue(jei.contains("layout.addSlot(RecipeIngredientRole.INPUT, x, y)"));
        assertTrue(jei.contains("addItemStack(recipe.fluid().display())"));
        assertTrue(jei.contains("youkaisfeasts\", \"textures/gui/ferment.png"));
        assertTrue(jei.contains("layout.addSlot(RecipeIngredientRole.INPUT, 64, 1)"));
        assertFalse(jei.contains("setFluidRenderer"));
        assertFalse(jei.contains("addTooltipCallback"));
    }

    @Test void worldRendererUsesOnlyEmptyTextureAndPlacesItemsBelowFluid() throws IOException {
        String renderer = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlRenderer.java");
        assertFalse(renderer.contains("crafting_bowl_full.png"));
        assertTrue(renderer.contains("i * 40F"));
        assertTrue(renderer.contains("pose.translate(.5F,3F / 16F,.5F)"));
        assertTrue(renderer.contains("pose.translate(-.10F,-.10F,0F)"));
        assertTrue(renderer.contains("pose.scale(.30F,.30F,.30F)"));
        assertFalse(renderer.contains("i % 3"));
        assertTrue(renderer.indexOf("renderItems(bowl") < renderer.indexOf("renderFluid(bowl"));
    }

    @Test void sparseNetworkUpdatesClearOldSlotsAndTicksDoNotBroadcast() throws IOException {
        String entity = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlockEntity.java");
        assertTrue(entity.contains("clear(inputs);"));
        assertTrue(entity.contains("clear(outputs);"));
        assertTrue(entity.contains("fluid.setFluid(FluidStack.EMPTY);"));
        assertTrue(entity.contains("if (!loading) changed();"));
        assertTrue(entity.contains("if (level.isClientSide)"));
        assertTrue(entity.contains("bowl.setChanged();"));
    }

    @Test void successfulTransfersUseJuicerMatchingSounds() throws IOException {
        String block = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlock.java");
        String compat = read("src/main/java/com/yinfires/icecore/workstation/WorkstationContainerCompat.java");
        assertTrue(block.contains("SoundEvents.ITEM_FRAME_ADD_ITEM"));
        assertTrue(block.contains("SoundEvents.ITEM_FRAME_REMOVE_ITEM"));
        assertTrue(compat.contains("SoundEvents.BUCKET_EMPTY"));
        assertTrue(compat.contains("SoundEvents.BUCKET_FILL"));
        assertTrue(compat.contains("SoundEvents.BOTTLE_EMPTY"));
        assertTrue(compat.contains("SoundEvents.BOTTLE_FILL"));
        assertTrue(block.contains("WorkstationContainerCompat.isFluidContainer(held)"));
        assertTrue(block.contains("SoundSource.BLOCKS, 1F, 1F"));
        assertTrue(compat.contains("available.getAmount() < capacity"));
    }

    @Test void stirringSoundsMayOverlapButStopImmediatelyWithTheBowl() throws IOException {
        String block = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlock.java");
        String entity = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlockEntity.java");
        String sound = read("src/main/java/com/yinfires/icecore/mixing/client/MixingBowlStirringSound.java");
        assertFalse(block.contains("crafting_bowl_stirring"));
        assertTrue(entity.contains("MixingBowlStirringSound.update(this, stage == Stage.STIRRING)"));
        assertTrue(sound.contains("looping = false"));
        assertTrue(sound.contains("ACTIVE.computeIfAbsent(bowl, ignored -> new ArrayList<>()).add(created)"));
        assertTrue(sound.contains("Minecraft.getInstance().getSoundManager().play(created)"));
        assertTrue(sound.contains("if (!stirring)"));
        assertTrue(sound.contains("stopAll(bowl)"));
        assertTrue(sound.contains("sounds.forEach(MixingBowlStirringSound::stop)"));
        assertTrue(sound.contains("if (!stillStirring) stopAndForget()"));
        assertFalse(sound.contains("FADE_TICKS"));
    }

    @Test void containerizedIngredientsNeedTheirRecordedContainer() throws IOException {
        String entity = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlockEntity.java");
        String block = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlock.java");
        String compat = read("src/main/java/com/yinfires/icecore/workstation/WorkstationContainerCompat.java");
        assertTrue(entity.contains("NonNullList.withSize(9, ItemStack.EMPTY)"));
        assertTrue(entity.contains("inputContainers.set(i, requiredContainer.copyWithCount(1))"));
        assertTrue(entity.contains("WorkstationContainerCompat.emptyMatches(inputContainers.get(i), held)"));
        assertTrue(entity.contains("tag.put(\"InputContainers\""));
        assertTrue(entity.contains("tag.putLongArray(\"InputSequence\""));
        assertTrue(entity.contains("lastInputSlot(ItemStack held, boolean requireMatch)"));
        assertTrue(block.contains("icecore.mixing.need_input_container"));
        assertTrue(compat.contains("Behavior.FLUID"));
        assertTrue(compat.contains("Behavior.INGREDIENT"));
        assertTrue(compat.contains("stack.getCraftingRemainingItem()"));
        assertTrue(compat.contains("objects.entrySet().stream().sorted(Map.Entry.comparingByKey())"));
        assertTrue(compat.contains("value.filled().hasTag()).reversed()"));
        assertTrue(compat.contains("BuiltInRegistries.ITEM.getKey(value.filled().getItem()).toString()"));
    }

    @Test void heldItemsInsertUnlessTheyAreExplicitRetrievalContainers() throws IOException {
        String block = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlock.java");
        int emptyHand = block.indexOf("if (held.isEmpty())");
        int takeFluid = block.indexOf("bowl.fillHeldContainer(held)");
        int takeIngredient = block.indexOf("bowl.takeContainerizedInput(held)");
        int rejectFluidContainer = block.indexOf("bowl.hasFluid() && WorkstationContainerCompat.isFluidContainer(held)");
        int insertFluid = block.indexOf("insertedContainer.get().behavior() == WorkstationContainerCompat.Behavior.FLUID");
        int insertItem = block.indexOf("bowl.addInput(held, inputContainer)");
        assertTrue(emptyHand >= 0 && emptyHand < takeFluid);
        assertTrue(takeFluid < takeIngredient);
        assertTrue(takeIngredient < rejectFluidContainer);
        assertTrue(block.substring(0, takeIngredient).contains("WorkstationContainerCompat.isKnownEmptyContainer(held)"));
        assertTrue(rejectFluidContainer < insertFluid);
        assertTrue(insertFluid < insertItem);
        assertTrue(block.contains("Component.translatable(\"icecore.mixing.need_input_container\", required.getHoverName())"));
    }

    @Test void jadeCapabilitiesAreReadOnlyAndExposeFluidCapacity() throws IOException {
        String entity = read("src/main/java/com/yinfires/icecore/mixing/MixingBowlBlockEntity.java");
        assertTrue(entity.contains("capability == ForgeCapabilities.FLUID_HANDLER"));
        assertTrue(entity.contains("capability == ForgeCapabilities.ITEM_HANDLER"));
        assertTrue(entity.contains("getTankCapacity(int tank) { return tank == 0 ? 1000 : 0; }"));
        assertTrue(entity.contains("fill(FluidStack resource, FluidAction action) { return 0; }"));
        assertTrue(entity.contains("insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }"));
    }

    @Test void forcedRemovalNeedsUnchangedSecondConfirmation() throws IOException {
        String actions = read("src/main/java/com/yinfires/icecore/building/BuildingServerActions.java");
        assertTrue(actions.contains("FORCE_CONFIRM_TICKS = 60L"));
        assertTrue(actions.contains("confirmation.fingerprint() == contentFingerprint"));
        assertTrue(actions.contains("List<ItemStack> returns = List.of(blockStack)"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
