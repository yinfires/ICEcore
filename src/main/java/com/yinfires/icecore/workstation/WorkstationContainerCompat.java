package com.yinfires.icecore.workstation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yinfires.icecore.ICECore;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorkstationContainerCompat {
    public enum Behavior { FLUID, INGREDIENT }
    public record Entry(ItemStack filled, ItemStack empty, String category, Behavior behavior, FluidStack fluid) {}
    public record FilledContainer(ItemStack stack, int amount, String category) {}
    private record ConfiguredEntry(Ingredient matcher, ItemStack template, Entry entry, int priority) {}

    private static final TagKey<net.minecraft.world.item.Item> ICECORE_INGREDIENT_CONTAINER = itemTag("icecore", "ingredient_container");
    private static final TagKey<net.minecraft.world.item.Item> ICECORE_BOWL_CONTAINER = itemTag("icecore", "bowl_container");
    private static final TagKey<net.minecraft.world.item.Item> ICECORE_BOTTLE_CONTAINER = itemTag("icecore", "glass_bottle_container");
    private static final TagKey<net.minecraft.world.item.Item> ICECORE_BUCKET_CONTAINER = itemTag("icecore", "bucket_container");
    private static final TagKey<net.minecraft.world.item.Item> COOKERY_INGREDIENT_CONTAINER = itemTag("kaleidoscope_cookery", "ingredient_container");
    private static final TagKey<net.minecraft.world.item.Item> COOKERY_BOWL_CONTAINER = itemTag("kaleidoscope_cookery", "bowl_container");
    private static final TagKey<net.minecraft.world.item.Item> COOKERY_BOTTLE_CONTAINER = itemTag("kaleidoscope_cookery", "glass_bottle_container");
    private static final TagKey<net.minecraft.world.item.Item> COOKERY_BUCKET_CONTAINER = itemTag("kaleidoscope_cookery", "bucket_container");
    private static final int EMPTY_CONTAINER_CACHE_LIMIT = 512;
    private static final int DISCOVERED_CONTAINER_LIMIT = 256;
    private static volatile List<ConfiguredEntry> configured = List.of();
    private static final List<ItemStack> discoveredEmptyContainers = new CopyOnWriteArrayList<>();
    private static final Map<String, Optional<ItemStack>> emptyContainerCache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Optional<ItemStack>> eldest) {
                    return size() > EMPTY_CONTAINER_CACHE_LIMIT;
                }
            });

    private WorkstationContainerCompat() {}

    @SubscribeEvent public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static Optional<Entry> describe(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        for (ConfiguredEntry configuredEntry : configured) {
            if (matches(configuredEntry, stack)) return Optional.of(copyEntry(configuredEntry.entry(), stack));
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
        return resolveEmptyContainer(stack).map(empty -> new Entry(stack.copyWithCount(1), empty,
                categoryFor(empty, 0), Behavior.INGREDIENT, FluidStack.EMPTY));
    }

    public static Optional<ItemStack> resolveEmptyContainer(ItemStack filled) {
        if (filled.isEmpty()) return Optional.empty();
        String key = cacheKey(filled);
        Optional<ItemStack> cached = emptyContainerCache.get(key);
        if (cached != null) return cached.map(ItemStack::copy);
        Optional<ItemStack> resolved = resolveEmptyContainerUncached(filled).map(stack -> stack.copyWithCount(1));
        resolved.ifPresent(WorkstationContainerCompat::rememberEmptyContainer);
        emptyContainerCache.put(key, resolved.map(ItemStack::copy));
        return resolved.map(ItemStack::copy);
    }

    public static Ingredient resolveOutputCarrier(ItemStack result, Ingredient explicitCarrier) {
        if (explicitCarrier != null && !explicitCarrier.isEmpty()) return explicitCarrier;
        return resolveEmptyContainer(result).map(WorkstationContainerCompat::exactIngredient).orElse(Ingredient.EMPTY);
    }

    public static boolean isKnownEmptyContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE) || stack.is(Items.BOWL)) return true;
        if (stack.is(ICECORE_INGREDIENT_CONTAINER) || stack.is(COOKERY_INGREDIENT_CONTAINER)) return true;
        for (ItemStack empty : discoveredEmptyContainers) if (matchesTemplate(empty, stack)) return true;
        return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    public static boolean isFluidContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (ConfiguredEntry configuredEntry : configured) {
            Entry entry = configuredEntry.entry();
            if (entry.behavior() == Behavior.FLUID
                    && (matches(configuredEntry, stack) || matchesTemplate(entry.empty(), stack))) return true;
        }
        return FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent();
    }

    public static boolean emptyMatches(ItemStack required, ItemStack held) {
        return !required.isEmpty() && !held.isEmpty() && ItemStack.isSameItemSameTags(required, held);
    }

    public static Optional<FilledContainer> fillContainer(ItemStack empty, FluidStack available) {
        if (empty.isEmpty() || available.isEmpty()) return Optional.empty();
        for (ConfiguredEntry configuredEntry : configured) {
            Entry entry = configuredEntry.entry();
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

    private static Optional<ItemStack> resolveEmptyContainerUncached(ItemStack filled) {
        for (ConfiguredEntry configuredEntry : configured) {
            if (matches(configuredEntry, filled)) return Optional.of(configuredEntry.entry().empty().copy());
        }
        ItemStack remainder = filled.getCraftingRemainingItem();
        if (!remainder.isEmpty()) return Optional.of(remainder);
        Optional<ItemStack> fluidContainer = drainFluidContainer(filled);
        if (fluidContainer.isPresent()) return fluidContainer;
        if (ModList.get().isLoaded("kaleidoscope_cookery")) {
            Optional<ItemStack> cookery = com.yinfires.icecore.compat.kaleidoscopecookery.KaleidoscopeCookeryCompat
                    .resolveContainerItem(filled);
            if (cookery.isPresent()) return cookery;
        }
        if (filled.getItem() instanceof BowlFoodItem || filled.is(ICECORE_BOWL_CONTAINER) || filled.is(COOKERY_BOWL_CONTAINER)) {
            return Optional.of(Items.BOWL.getDefaultInstance());
        }
        if (filled.is(ICECORE_BOTTLE_CONTAINER) || filled.is(COOKERY_BOTTLE_CONTAINER) || filled.is(Items.POTION)) {
            return Optional.of(Items.GLASS_BOTTLE.getDefaultInstance());
        }
        if (filled.is(ICECORE_BUCKET_CONTAINER) || filled.is(COOKERY_BUCKET_CONTAINER)) {
            return Optional.of(Items.BUCKET.getDefaultInstance());
        }
        return Optional.empty();
    }

    private static Optional<ItemStack> drainFluidContainer(ItemStack filled) {
        LazyOptional<net.minecraftforge.fluids.capability.IFluidHandlerItem> capability =
                FluidUtil.getFluidHandler(filled.copyWithCount(1));
        if (!capability.isPresent()) return Optional.empty();
        var handler = capability.orElseThrow(IllegalStateException::new);
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stored = handler.getFluidInTank(tank);
            if (stored.isEmpty()) continue;
            FluidStack drained = handler.drain(stored.getAmount(), IFluidHandler.FluidAction.EXECUTE);
            ItemStack empty = handler.getContainer();
            if (drained.getAmount() == stored.getAmount() && !empty.isEmpty()) {
                return Optional.of(empty.copyWithCount(1));
            }
        }
        return Optional.empty();
    }

    private static boolean matches(ConfiguredEntry entry, ItemStack actual) {
        return !entry.template().isEmpty() ? matchesTemplate(entry.template(), actual) : entry.matcher().test(actual);
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

    private static void rememberEmptyContainer(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        for (ItemStack known : discoveredEmptyContainers) if (matchesTemplate(known, copy)) return;
        while (discoveredEmptyContainers.size() >= DISCOVERED_CONTAINER_LIMIT) discoveredEmptyContainers.remove(0);
        discoveredEmptyContainers.add(copy);
    }

    private static String cacheKey(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + "|" + (stack.hasTag() ? stack.getTag().toString() : "");
    }

    private static Ingredient exactIngredient(ItemStack stack) {
        ItemStack exact = stack.copyWithCount(1);
        return exact.hasTag() ? StrictNBTIngredient.of(exact) : Ingredient.of(exact);
    }

    private static TagKey<net.minecraft.world.item.Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private Loader() { super(new Gson(), "icecore/container_compat"); }

        @Override protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
            List<ConfiguredEntry> next = new ArrayList<>();
            objects.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(value -> next.add(parse(value.getKey(), value.getValue().getAsJsonObject())));
            next.sort(Comparator.comparingInt(ConfiguredEntry::priority).reversed()
                    .thenComparing(value -> value.template().isEmpty() ? value.matcher().toJson().toString()
                            : BuiltInRegistries.ITEM.getKey(value.template().getItem()).toString())
                    .thenComparing(value -> BuiltInRegistries.ITEM.getKey(value.entry().empty().getItem()).toString()));
            configured = List.copyOf(next);
            discoveredEmptyContainers.clear();
            next.forEach(value -> rememberEmptyContainer(value.entry().empty()));
            emptyContainerCache.clear();
        }

        private static ConfiguredEntry parse(ResourceLocation id, JsonObject json) {
            ParsedMatcher filled = parseMatcher(json.get("filled"), id);
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
            Entry entry = new Entry(filled.template(), empty, category, behavior, fluid);
            return new ConfiguredEntry(filled.matcher(), filled.template(), entry, filled.priority());
        }

        private static ParsedMatcher parseMatcher(JsonElement element, ResourceLocation id) {
            if (element.isJsonObject() && element.getAsJsonObject().has("tag")) {
                Ingredient ingredient = Ingredient.fromJson(element);
                if (ingredient.isEmpty()) throw new IllegalArgumentException("Empty container tag matcher in " + id);
                return new ParsedMatcher(ingredient, ItemStack.EMPTY, 0);
            }
            ItemStack stack = parseStack(element, id);
            return new ParsedMatcher(Ingredient.of(stack), stack, stack.hasTag() ? 2 : 1);
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

        private record ParsedMatcher(Ingredient matcher, ItemStack template, int priority) {}
    }
}
