package com.yinfires.icecore.mixing;

import com.yinfires.icecore.workstation.WorkstationContainerCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.ArrayList;
import java.util.List;

public final class MixingBowlBlockEntity extends BlockEntity implements ContainerContentAccess {
    public enum Stage { INPUT, STIRRING, OUTPUT }
    private final NonNullList<ItemStack> inputs = NonNullList.withSize(9, ItemStack.EMPTY);
    private final NonNullList<ItemStack> inputContainers = NonNullList.withSize(9, ItemStack.EMPTY);
    private final long[] inputSequence = new long[9];
    private final NonNullList<ItemStack> outputs = NonNullList.withSize(9, ItemStack.EMPTY);
    private final List<Ingredient> carriers = new ArrayList<>();
    private final FluidTank fluid = new FluidTank(1000) {
        @Override protected void onContentsChanged() { if (!loading) changed(); }
    };
    private final LazyOptional<IFluidHandler> fluidView = LazyOptional.of(() -> new ReadOnlyFluidHandler());
    private final LazyOptional<IItemHandler> itemView = LazyOptional.of(() -> new ReadOnlyItemHandler());
    private Stage stage = Stage.INPUT;
    private int stirringTicks;
    private int progress;
    private boolean loading;
    private long nextInputSequence = 1L;

    public MixingBowlBlockEntity(BlockPos pos, BlockState state) { super(ModMixing.MIXING_BOWL_ENTITY.get(), pos, state); }
    public Stage stage() { return stage; }
    public int progress() { return progress; }
    public int stirringTicks() { return stirringTicks; }
    public NonNullList<ItemStack> visibleItems() { return stage == Stage.OUTPUT ? outputs : inputs; }
    public FluidStack visibleFluid() { return stage == Stage.OUTPUT ? FluidStack.EMPTY : fluid.getFluid().copy(); }
    public int occupiedInputSlots() { return inputCount() + (fluid.isEmpty() ? 0 : 1); }
    public int inputCount() { return count(inputs); }
    public boolean hasFluid() { return !fluid.isEmpty(); }
    public boolean hasOutputs() { return count(outputs) > 0; }

    public boolean addInput(ItemStack stack, ItemStack requiredContainer) {
        if (stage != Stage.INPUT || occupiedInputSlots() >= 9 || WorkstationContainerCompat.describe(stack)
                .map(entry -> entry.behavior() == WorkstationContainerCompat.Behavior.FLUID).orElse(false)) return false;
        for (int i = 0; i < 9; i++) if (inputs.get(i).isEmpty()) {
            inputs.set(i, stack.copyWithCount(1));
            inputContainers.set(i, requiredContainer.copyWithCount(1));
            inputSequence[i] = nextInputSequence++;
            changed();
            return true;
        }
        return false;
    }

    public boolean startStir() {
        if (stage == Stage.OUTPUT || stirringTicks > 6) return false;
        stage = Stage.STIRRING;
        stirringTicks = 10;
        changed();
        return true;
    }

    public ItemStack takeLastPlainInput() {
        if (stage != Stage.INPUT) return ItemStack.EMPTY;
        int slot = lastInputSlot(ItemStack.EMPTY, false);
        if (slot < 0 || !inputContainers.get(slot).isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = inputs.get(slot);
        clearInputSlot(slot); changed(); return stack;
    }

    public ItemStack requiredLastInputContainer() {
        if (stage != Stage.INPUT) return ItemStack.EMPTY;
        int slot = lastInputSlot(ItemStack.EMPTY, false);
        return slot < 0 ? ItemStack.EMPTY : inputContainers.get(slot).copy();
    }

    public ItemStack takeContainerizedInput(ItemStack held) {
        if (stage != Stage.INPUT || held.isEmpty()) return ItemStack.EMPTY;
        int slot = lastInputSlot(held, true);
        if (slot >= 0) {
            ItemStack stack = inputs.get(slot);
            clearInputSlot(slot);
            changed();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    public boolean canAcceptFluid(FluidStack candidate) {
        return stage == Stage.INPUT && !candidate.isEmpty()
                && (!fluid.isEmpty() || inputCount() < 9)
                && fluid.fill(candidate, IFluidHandler.FluidAction.SIMULATE) == candidate.getAmount();
    }

    public boolean addFluid(FluidStack candidate) {
        if (!canAcceptFluid(candidate)) return false;
        return fluid.fill(candidate, IFluidHandler.FluidAction.EXECUTE) == candidate.getAmount();
    }

    public WorkstationContainerCompat.FilledContainer fillHeldContainer(ItemStack empty) {
        if (stage != Stage.INPUT || fluid.isEmpty()) return null;
        var filled = WorkstationContainerCompat.fillContainer(empty, fluid.getFluid());
        if (filled.isEmpty()) return null;
        fluid.drain(filled.get().amount(), IFluidHandler.FluidAction.EXECUTE);
        return filled.get();
    }

    public Ingredient requiredOutputCarrier() {
        if (stage != Stage.OUTPUT) return Ingredient.EMPTY;
        for (int i = 8; i >= 0; i--) if (!outputs.get(i).isEmpty()) return carriers.get(i);
        return Ingredient.EMPTY;
    }

    public ItemStack takeOutput(ItemStack held) {
        if (stage != Stage.OUTPUT) return ItemStack.EMPTY;
        for (int i = 8; i >= 0; i--) if (!outputs.get(i).isEmpty()) {
            Ingredient carrier = carriers.get(i);
            if (!carrier.isEmpty() && !carrier.test(held)) return ItemStack.EMPTY;
            if (carrier.isEmpty() && !held.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = outputs.get(i);
            outputs.set(i, ItemStack.EMPTY); carriers.set(i, Ingredient.EMPTY);
            if (!hasOutputs()) stage = Stage.INPUT;
            changed(); return result;
        }
        return ItemStack.EMPTY;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MixingBowlBlockEntity bowl) {
        if (bowl.stage != Stage.STIRRING) return;
        if (level.isClientSide) {
            if (bowl.stirringTicks > 0) bowl.stirringTicks--;
            return;
        }
        if (bowl.stirringTicks > 0) {
            bowl.stirringTicks--;
            bowl.progress++;
            if (bowl.progress >= 50) { bowl.finish((ServerLevel) level); return; }
            bowl.setChanged();
        } else {
            bowl.progress = 0;
            bowl.stage = Stage.INPUT;
            bowl.changed();
        }
    }

    private void finish(ServerLevel level) {
        SimpleContainer container = new SimpleContainer(inputs.toArray(ItemStack[]::new));
        var recipe = level.getRecipeManager().getAllRecipesFor(ModMixing.MIXING_BOWL_RECIPE.get()).stream()
                .filter(value -> value.matches(container, fluid.getFluid(), level)).findFirst();
        stirringTicks = 0; progress = 0;
        if (recipe.isPresent()) {
            clear(inputs);
            clear(inputContainers); clearInputSequence();
            fluid.setFluid(FluidStack.EMPTY);
            clear(outputs);
            carriers.clear();
            for (int i = 0; i < 9; i++) carriers.add(Ingredient.EMPTY);
            List<MixingBowlRecipe.Result> results = recipe.get().results();
            for (int i = 0; i < results.size(); i++) {
                outputs.set(i, results.get(i).stack().copy());
                carriers.set(i, results.get(i).effectiveCarrier());
            }
            stage = Stage.OUTPUT;
        } else stage = Stage.INPUT;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    private static int count(NonNullList<ItemStack> stacks) { int n=0; for (ItemStack s:stacks) if(!s.isEmpty()) n++; return n; }
    private static void clear(NonNullList<ItemStack> stacks) { for(int i=0;i<stacks.size();i++) stacks.set(i,ItemStack.EMPTY); }
    private void clearInputSlot(int slot) { inputs.set(slot, ItemStack.EMPTY); inputContainers.set(slot, ItemStack.EMPTY); inputSequence[slot] = 0L; }
    private void clearInputSequence() { java.util.Arrays.fill(inputSequence, 0L); nextInputSequence = 1L; }
    private int lastInputSlot(ItemStack held, boolean requireMatch) {
        int found = -1; long newest = Long.MIN_VALUE;
        for (int i = 0; i < inputs.size(); i++) {
            if (inputs.get(i).isEmpty() || inputSequence[i] <= newest) continue;
            if (requireMatch && !WorkstationContainerCompat.emptyMatches(inputContainers.get(i), held)) continue;
            found = i; newest = inputSequence[i];
        }
        return found;
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); ContainerHelper.saveAllItems(tag, inputs);
        CompoundTag inputContainerTag = new CompoundTag();
        ContainerHelper.saveAllItems(inputContainerTag, inputContainers); tag.put("InputContainers", inputContainerTag);
        tag.putLongArray("InputSequence", inputSequence); tag.putLong("NextInputSequence", nextInputSequence);
        tag.put("Fluid", fluid.writeToNBT(new CompoundTag()));
        CompoundTag out = new CompoundTag(); ContainerHelper.saveAllItems(out, outputs); tag.put("Outputs", out);
        tag.putString("Stage", stage.name()); tag.putInt("Stirring", stirringTicks); tag.putInt("Progress", progress);
        ListTag carrierList = new ListTag(); for(Ingredient carrier:carriers) carrierList.add(StringTag.valueOf(carrier.toJson().toString())); tag.put("Carriers",carrierList);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        loading = true;
        try {
            clear(inputs);
            clear(inputContainers);
            clearInputSequence();
            clear(outputs);
            fluid.setFluid(FluidStack.EMPTY);
            if (tag.contains("Fluid")) fluid.readFromNBT(tag.getCompound("Fluid"));
            ContainerHelper.loadAllItems(tag, inputs);
            if (tag.contains("InputContainers")) {
                ContainerHelper.loadAllItems(tag.getCompound("InputContainers"), inputContainers);
            } else {
                for (int i = 0; i < inputs.size(); i++) {
                    var entry = WorkstationContainerCompat.describe(inputs.get(i));
                    if (entry.isPresent() && entry.get().behavior() == WorkstationContainerCompat.Behavior.INGREDIENT) {
                        inputContainers.set(i, entry.get().empty().copy());
                    }
                }
            }
            long[] savedSequence = tag.getLongArray("InputSequence");
            if (savedSequence.length == inputSequence.length) {
                System.arraycopy(savedSequence, 0, inputSequence, 0, inputSequence.length);
                nextInputSequence = Math.max(tag.getLong("NextInputSequence"),
                        java.util.Arrays.stream(inputSequence).max().orElse(0L) + 1L);
            } else {
                long sequence = 1L;
                for (int i = 0; i < inputs.size(); i++) if (!inputs.get(i).isEmpty()) inputSequence[i] = sequence++;
                nextInputSequence = sequence;
            }
            if(tag.contains("Outputs")) ContainerHelper.loadAllItems(tag.getCompound("Outputs"),outputs);
            try { stage=Stage.valueOf(tag.getString("Stage")); } catch(Exception ignored){stage=Stage.INPUT;}
            stirringTicks=tag.getInt("Stirring"); progress=tag.getInt("Progress"); carriers.clear();
            ListTag list=tag.getList("Carriers",8);
            for(int i=0;i<9;i++) {
                Ingredient saved = i < list.size()
                        ? Ingredient.fromJson(com.google.gson.JsonParser.parseString(list.getString(i)))
                        : Ingredient.EMPTY;
                carriers.add(WorkstationContainerCompat.resolveOutputCarrier(outputs.get(i), saved));
            }
        } finally {
            loading = false;
        }
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet){
        if(packet.getTag()!=null) {
            load(packet.getTag());
            updateClientStirringSound();
        }
    }
    @Override public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        updateClientStirringSound();
    }
    private void updateClientStirringSound() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.yinfires.icecore.mixing.client.MixingBowlStirringSound.update(this, stage == Stage.STIRRING));
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluidView.invalidate(); itemView.invalidate(); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) return fluidView.cast();
        if (capability == ForgeCapabilities.ITEM_HANDLER) return itemView.cast();
        return super.getCapability(capability, side);
    }
    @Override public boolean icecore$hasContainerContent(){return inputCount()>0||!fluid.isEmpty()||hasOutputs()||stage==Stage.STIRRING;}
    @Override public int icecore$contentFingerprint(){return saveWithoutMetadata().hashCode();}

    private final class ReadOnlyFluidHandler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? fluid.getFluid().copy() : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? 1000 : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    }

    private final class ReadOnlyItemHandler implements IItemHandler {
        @Override public int getSlots() { return 9; }
        @Override public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= 9) return ItemStack.EMPTY;
            return visibleItems().get(slot).copy();
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    }
}
