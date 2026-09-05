package com.yinfires.icecore.recipehide;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dual-sided, statically held view of the "recipes forbidden from use" set. The authoritative server
 * fills it from {@link RecipeHideService}; each client fills it from the sync packet. It is read on
 * the hot crafting path via {@code RecipeManagerMixin}, so all filter helpers return the original
 * instance unchanged when nothing is hidden (zero allocation).
 *
 * <p>Three modes: {@code NONE} (nothing hidden), {@code ALL} (everything hidden except {@code ids}
 * which are exemptions), {@code PARTIAL} ({@code ids} are the hidden recipes). Modes let hide-all be
 * expressed without enumerating every recipe id.
 *
 * <p><b>JEI build bypass</b>: JEI (re)builds its runtime by enumerating recipes through
 * {@code getAllRecipesFor} → {@code byType} — the very method this registry filters. In single player
 * the registry is shared with the integrated server and is already active before JEI builds, so JEI
 * would permanently register only the unfiltered subset; recipes it never registered can't be shown
 * later, no matter what the runtime applier does. While JEI is building (bracketed by the ICEcore JEI
 * plugin), non-server threads therefore see the unfiltered view. The server thread is never bypassed,
 * so the crafting ban stays enforced throughout.
 */
public final class RecipeHideRegistry {
    public enum Mode { NONE, ALL, PARTIAL }

    private static volatile Mode mode = Mode.NONE;
    private static volatile Set<ResourceLocation> ids = Set.of();
    private static volatile boolean jeiBuildBypass;
    private static volatile Thread serverThread;

    private RecipeHideRegistry() {
    }

    /** Replaces the active state atomically. {@code ids} is exemptions for ALL, hidden set for PARTIAL. */
    public static void update(Mode newMode, Set<ResourceLocation> recipeIds) {
        ids = recipeIds.isEmpty() ? Set.of() : Set.copyOf(recipeIds);
        mode = newMode;
    }

    /** Clears all state; used on server stop so a later single-player world starts clean. */
    public static void clear() {
        mode = Mode.NONE;
        ids = Set.of();
    }

    /** The authoritative server's thread; filtering on it is never bypassed. Null when no server. */
    public static void setServerThread(Thread thread) {
        serverThread = thread;
    }

    /** Bracket set by the JEI plugin while JEI builds its runtime; see class javadoc. */
    public static void setJeiBuildBypass(boolean bypass) {
        jeiBuildBypass = bypass;
    }

    private static boolean bypassed() {
        return jeiBuildBypass && Thread.currentThread() != serverThread;
    }

    private static boolean isHidden(ResourceLocation id) {
        Mode m = mode;
        Set<ResourceLocation> set = ids;
        return switch (m) {
            case NONE -> false;
            case ALL -> !set.contains(id);
            case PARTIAL -> set.contains(id);
        };
    }

    /** Filters {@code RecipeManager.byType}. Empty/NONE state returns the original map (no copy). */
    public static <T> Map<ResourceLocation, T> filterMap(Map<ResourceLocation, T> original) {
        Mode m = mode;
        Set<ResourceLocation> set = ids;
        if (m == Mode.NONE || bypassed()) {
            return original;
        }
        if (m == Mode.ALL && set.isEmpty()) {
            return Map.of();
        }
        boolean any = false;
        for (ResourceLocation key : original.keySet()) {
            if (m == Mode.ALL ? !set.contains(key) : set.contains(key)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return original;
        }
        Map<ResourceLocation, T> out = new LinkedHashMap<>(original.size());
        original.forEach((key, value) -> {
            boolean hidden = m == Mode.ALL ? !set.contains(key) : set.contains(key);
            if (!hidden) {
                out.put(key, value);
            }
        });
        return out;
    }

    /** Filters {@code RecipeManager.byKey}. */
    public static Optional<? extends Recipe<?>> filterOptional(Optional<? extends Recipe<?>> original) {
        if (mode == Mode.NONE || original.isEmpty() || bypassed()) {
            return original;
        }
        return isHidden(original.get().getId()) ? Optional.empty() : original;
    }
}
