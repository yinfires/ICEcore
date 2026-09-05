package com.yinfires.icecore.recipehide;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persistent authoritative state for {@link RecipeHideService}: a global hide-all baseline plus
 * per-item overrides. Relative to the baseline, an item's producing recipes are hidden when it is in
 * {@code forcedHiddenItems}, shown when it is in {@code forcedShownItems}, otherwise follow
 * {@code hideAll}. The effective forbidden-recipe set is not stored; it is recomputed from these on
 * load and after datapack reloads so newly added recipes stay covered.
 */
public final class RecipeHideSavedData extends SavedData {
    public static final String FILE_ID = "icecore_recipe_hide";

    private boolean hideAll;
    private final Set<ResourceLocation> forcedHiddenItems = new LinkedHashSet<>();
    private final Set<ResourceLocation> forcedShownItems = new LinkedHashSet<>();

    public static RecipeHideSavedData load(CompoundTag tag) {
        RecipeHideSavedData data = new RecipeHideSavedData();
        data.hideAll = tag.getBoolean("HideAll");
        readIds(tag, "ForcedHiddenItems", data.forcedHiddenItems);
        readIds(tag, "ForcedShownItems", data.forcedShownItems);
        return data;
    }

    private static void readIds(CompoundTag tag, String key, Set<ResourceLocation> target) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                target.add(id);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("HideAll", hideAll);
        tag.put("ForcedHiddenItems", writeIds(forcedHiddenItems));
        tag.put("ForcedShownItems", writeIds(forcedShownItems));
        return tag;
    }

    private static ListTag writeIds(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    boolean hideAll() {
        return hideAll;
    }

    void setHideAll(boolean value) {
        if (this.hideAll != value) {
            this.hideAll = value;
            setDirty();
        }
    }

    Set<ResourceLocation> forcedHiddenItems() {
        return forcedHiddenItems;
    }

    Set<ResourceLocation> forcedShownItems() {
        return forcedShownItems;
    }

    void markDirty() {
        setDirty();
    }
}
