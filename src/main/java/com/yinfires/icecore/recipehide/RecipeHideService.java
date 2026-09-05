package com.yinfires.icecore.recipehide;

import com.yinfires.icecore.network.ClientBoundRecipeHidePacket;
import com.yinfires.icecore.network.ICECoreNetwork;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-authoritative controller for recipe hiding. Owns {@link RecipeHideSavedData}, keeps the
 * server-side {@link RecipeHideRegistry} in sync, and broadcasts a full snapshot to all clients on
 * change / login / datapack reload.
 *
 * <p>Model: a {@code hideAll} baseline plus per-item overrides ({@code forcedHiddenItems} /
 * {@code forcedShownItems}). The effective forbidden-recipe set is derived on demand from the live
 * (unfiltered) recipe list, so datapack-added recipes are covered and a per-item "show" can exempt
 * recipes even while hide-all is active.
 */
public final class RecipeHideService {
    private static MinecraftServer server;
    private static RecipeHideSavedData data;

    private RecipeHideService() {
    }

    public static void start(MinecraftServer minecraftServer) {
        server = minecraftServer;
        RecipeHideRegistry.setServerThread(minecraftServer.getRunningThread());
        data = minecraftServer.overworld().getDataStorage().computeIfAbsent(
                RecipeHideSavedData::load, RecipeHideSavedData::new, RecipeHideSavedData.FILE_ID);
        recomputeRegistry();
    }

    public static void stop() {
        data = null;
        server = null;
        RecipeHideRegistry.setServerThread(null);
        RecipeHideRegistry.clear();
    }

    public static boolean isReady() {
        return data != null;
    }

    /** Recomputes the server-side registry (byType crafting block) from the current model. */
    public static void recomputeRegistry() {
        if (data == null) {
            RecipeHideRegistry.clear();
            return;
        }
        if (data.hideAll()) {
            // Everything forbidden except recipes producing an explicitly-shown item.
            RecipeHideRegistry.update(RecipeHideRegistry.Mode.ALL,
                    recipesProducingAny(data.forcedShownItems()));
        } else {
            // Only recipes producing an explicitly-hidden item are forbidden.
            RecipeHideRegistry.update(RecipeHideRegistry.Mode.PARTIAL,
                    recipesProducingAny(data.forcedHiddenItems()));
        }
    }

    /** All recipe ids whose result item is in {@code itemIds}. Uses the live (unfiltered) recipe list. */
    private static Set<ResourceLocation> recipesProducingAny(Set<ResourceLocation> itemIds) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (server == null || itemIds.isEmpty()) {
            return ids;
        }
        RegistryAccess access = server.registryAccess();
        for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
            ResourceLocation itemId = resultItemId(recipe, access);
            if (itemId != null && itemIds.contains(itemId)) {
                ids.add(recipe.getId());
            }
        }
        return ids;
    }

    /** All recipe ids whose result item is {@code itemId}. Uses the live (unfiltered) recipe list. */
    private static Set<ResourceLocation> recipesProducing(ResourceLocation itemId) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (server == null) {
            return ids;
        }
        RegistryAccess access = server.registryAccess();
        for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
            if (itemId.equals(resultItemId(recipe, access))) {
                ids.add(recipe.getId());
            }
        }
        return ids;
    }

    private static ResourceLocation resultItemId(Recipe<?> recipe, RegistryAccess access) {
        ItemStack result;
        try {
            result = recipe.getResultItem(access);
        } catch (Throwable ignored) {
            return null;
        }
        if (result == null || result.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.ITEM.getKey(result.getItem());
    }

    // --- Commands -----------------------------------------------------------------------------

    public static boolean hideAll() {
        if (data == null) {
            return false;
        }
        data.setHideAll(true);
        data.forcedHiddenItems().clear();
        data.forcedShownItems().clear();
        data.markDirty();
        recomputeRegistry();
        broadcast();
        return true;
    }

    public static boolean showAll() {
        if (data == null) {
            return false;
        }
        data.setHideAll(false);
        data.forcedHiddenItems().clear();
        data.forcedShownItems().clear();
        data.markDirty();
        recomputeRegistry();
        broadcast();
        return true;
    }

    /** Hides all recipes producing the item and removes the item from the JEI list. */
    public static int hideItem(ResourceLocation itemId) {
        if (data == null) {
            return 0;
        }
        int count = recipesProducing(itemId).size();
        data.forcedShownItems().remove(itemId);
        data.forcedHiddenItems().add(itemId);
        data.markDirty();
        recomputeRegistry();
        broadcast();
        return count;
    }

    /** Unlocks all recipes producing the item and restores the item to the JEI list. */
    public static int showItem(ResourceLocation itemId) {
        if (data == null) {
            return 0;
        }
        int count = recipesProducing(itemId).size();
        data.forcedHiddenItems().remove(itemId);
        data.forcedShownItems().add(itemId);
        data.markDirty();
        recomputeRegistry();
        broadcast();
        return count;
    }

    // --- Sync ---------------------------------------------------------------------------------

    private static ClientBoundRecipeHidePacket snapshot() {
        boolean all = data != null && data.hideAll();
        // recipeIds: exemptions when hideAll (recipes producing shown items), else the hidden set
        // (recipes producing hidden items). Client uses these with the same predicate.
        Set<ResourceLocation> recipeSet = data == null ? Set.of()
                : recipesProducingAny(all ? data.forcedShownItems() : data.forcedHiddenItems());
        List<ResourceLocation> recipeIds = new ArrayList<>(recipeSet);
        List<ResourceLocation> shownItemIds = data == null ? List.of() : new ArrayList<>(data.forcedShownItems());
        List<ResourceLocation> hiddenItemIds = data == null ? List.of() : new ArrayList<>(data.forcedHiddenItems());
        return new ClientBoundRecipeHidePacket(all, recipeIds, shownItemIds, hiddenItemIds);
    }

    public static void broadcast() {
        MinecraftServer current = server != null ? server : ServerLifecycleHooks.getCurrentServer();
        if (current == null) {
            return;
        }
        ClientBoundRecipeHidePacket packet = snapshot();
        for (ServerPlayer player : current.getPlayerList().getPlayers()) {
            ICECoreNetwork.sendToPlayer(packet, player);
        }
    }

    public static void sendTo(ServerPlayer player) {
        if (data == null) {
            return;
        }
        ICECoreNetwork.sendToPlayer(snapshot(), player);
    }

    /** Toggles the executing player's client-side exempt mode (visual only). */
    public static void setExempt(ServerPlayer player, boolean exempt) {
        ICECoreNetwork.sendToPlayer(new com.yinfires.icecore.network.ClientBoundRecipeExemptPacket(exempt), player);
    }
}
