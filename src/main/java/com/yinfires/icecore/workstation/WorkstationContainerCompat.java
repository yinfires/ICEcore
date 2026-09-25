package com.yinfires.icecore.workstation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yinfires.icecore.ICECore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorkstationContainerCompat {
    public enum Behavior { FLUID, INGREDIENT }
    public record Entry(ItemStack filled, ItemStack empty, String category, Behavior behavior, FluidStack fluid) {}
    public record FilledContainer(ItemStack stack, int amount, String category) {}
    private static volatile List<Entry> configured = List.of();

    private WorkstationContainerCompat() {}

    @SubscribeEvent public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static Optional<Entry> describe(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (Entry entry : configured) {
            if (matchesTemplate(entry.filled(), stack)) return Optional.of(copyEntry(entry, stack));
        }
        LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> capability = FluidUtil.getFluidHandler(stack.copyWithCount(1));
        if (capability.isPresent()) {
            var handler = capability.orElseThrow(IllegalStateException::new);
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stored = handler.getFluidInTank(tank);
                if (stored.isEmpty()) continue;
                int amount = stored.getAmount();
                FluidStack drained = handler.drain(amount, IFluidHandler.FluidAction.EXECUTE);
                ItemStack empty = handler.getContainer().copyWithCount(1);
                if (drained.getAmount() == amount && !empty.isEmpty()) {
                    FluidStack measured = stored.copy();
                    measured.setAmount(amount);
                    return Optional.of(new Entry(stack.copyWithCount(1), empty, categoryFor(empty, amount), Behavior.FLUID, measured));
                }
            }
        }
        ItemStack remainder = stack.getCraftingRemainingItem();
        if (!remainder.isEmpty()) {
            return Optional.of(new Entry(stack.copyWithCount(1), remainder.copyWithCount(1), categoryFor(remainder, 0),
                    Behavior.INGREDIENT, FluidStack.EMPTY));
        }
        return Optional.empty();
    }

    public static boolean isKnownEmptyContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BOWL)) return true;
        for (Entry entry : configured) if (matchesTemplate(entry.empty(), stack)) return true;
        return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Entry entry : configured) {
            if (entry.behavior() == Behavior.FLUID
                    && (matchesTemplate(entry.filled(), stack) || matchesTemplate(entry.empty(), stack))) return true;
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    public static boolean emptyMatches(ItemStack required, ItemStack held) {
        return !required.isEmpty() && !held.isEmpty() && ItemStack.isSameItemSameTags(required, held);
    }

    public static Optional<FilledContainer> fillContainer(ItemStack empty, FluidStack available) {
        if (empty.isEmpty() || available.isEmpty()) return Optional.empty();
        for (Entry entry : configured) {
            if (entry.behavior() == Behavior.FLUID && matchesTemplate(entry.empty(), empty)
                    && entry.fluid().isFluidEqual(available) && available.getAmount() >= entry.fluid().getAmount()) {
                return Optional.of(new FilledContainer(entry.filled().copy(), entry.fluid().getAmount(), entry.category()));
            }
        }
        LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> capability = FluidUtil.getFluidHandler(empty.copyWithCount(1));
        if (!capability.isPresent()) return Optional.empty();
        var handler = capability.orElseThrow(IllegalStateException::new);
        int capacity = handler.getTanks() == 0 ? 0 : handler.getTankCapacity(0) - handler.getFluidInTank(0).getAmount();
        if (capacity <= 0 || available.getAmount() < capacity) return Optional.empty();
        FluidStack requested = available.copy();
        requested.setAmount(capacity);
        if (handler.fill(requested, IFluidHandler.FluidAction.SIMULATE) != capacity) return Optional.empty();
        handler.fill(requested, IFluidHandler.FluidAction.EXECUTE);
        return Optional.of(new FilledContainer(handler.getContainer().copyWithCount(1), capacity, categoryFor(empty, capacity)));
    }

    public static SoundEvent fillSound(String category) {
        return "bottle".equals(category) ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL;
    }

    public static SoundEvent emptySound(String category) {
        return "bottle".equals(category) ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY;
    }

    private static Entry copyEntry(Entry entry, ItemStack actual) {
        return new Entry(actual.copyWithCount(1), entry.empty().copy(), entry.category(), entry.behavior(), entry.fluid().copy());
    }

    private static boolean matchesTemplate(ItemStack template, ItemStack actual) {
        if (!template.is(actual.getItem())) return false;
        return !template.hasTag() || ItemStack.isSameItemSameTags(template, actual);
    }

    private static String categoryFor(ItemStack empty, int amount) {
        if (empty.is(Items.GLASS_BOTTLE) || amount > 0 && amount <= 250) return "bottle";
        if (empty.is(Items.BOWL)) return "bowl";
        if (empty.is(Items.BUCKET) || amount >= 1000) return "bucket";
        return "container";
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private Loader() { super(new Gson(), "icecore/container_compat"); }

        @Override protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            List<Entry> next = new ArrayList<>();
            objects.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(value -> next.add(parse(value.getKey(), value.getValue().getAsJsonObject())));
            next.sort(Comparator.comparing((Entry value) -> value.filled().hasTag()).reversed()
                    .thenComparing(value -> BuiltInRegistries.ITEM.getKey(value.filled().getItem()).toString())
                    .thenComparing(value -> value.filled().hasTag() ? value.filled().getTag().toString() : "")
                    .thenComparing(value -> BuiltInRegistries.ITEM.getKey(value.empty().getItem()).toString()));
            configured = List.copyOf(next);
        }

        private static Entry parse(ResourceLocation id, JsonObject json) {
            ItemStack filled = parseStack(json.get("filled"), id);
            ItemStack empty = parseStack(json.get("empty"), id);
            String category = json.has("category") ? json.get("category").getAsString() : categoryFor(empty, 0);
            Behavior behavior = json.has("behavior")
                    ? Behavior.valueOf(json.get("behavior").getAsString().toUpperCase(java.util.Locale.ROOT))
                    : json.has("fluid") ? Behavior.FLUID : Behavior.INGREDIENT;
            FluidStack fluid = FluidStack.EMPTY;
            if (behavior == Behavior.FLUID) {
                ResourceLocation fluidId = ResourceLocation.tryParse(json.get("fluid").getAsString());
                var registeredFluid = fluidId == null ? null : ForgeRegistries.FLUIDS.getValue(fluidId);
                int amount = json.has("amount") ? json.get("amount").getAsInt() : 1000;
                if (registeredFluid == null || amount <= 0)
                    throw new IllegalArgumentException("Invalid fluid container entry " + id);
                fluid = new FluidStack(registeredFluid, amount);
            }
            return new Entry(filled, empty, category, behavior, fluid);
        }

        private static ItemStack parseStack(JsonElement element, ResourceLocation id) {
            if (element.isJsonObject()) {
                ItemStack stack = CraftingHelper.getItemStack(element.getAsJsonObject(), true, false);
                if (!stack.isEmpty()) return stack.copyWithCount(1);
            } else if (element.isJsonPrimitive()) {
                ResourceLocation itemId = ResourceLocation.tryParse(element.getAsString());
                var item = itemId == null ? null : BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                if (item != null && item != Items.AIR) return item.getDefaultInstance();
            }
            throw new IllegalArgumentException("Invalid container item in " + id);
        }
    }
}
