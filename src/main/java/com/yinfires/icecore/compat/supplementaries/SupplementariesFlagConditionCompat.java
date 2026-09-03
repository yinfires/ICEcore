package com.yinfires.icecore.compat.supplementaries;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.yinfires.icecore.ICECore;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Best-effort compatibility guard for the {@code supplementaries:flag} recipe condition.
 *
 * <p>Supplementaries registers that condition through Moonlight's {@code RegHelper}, but its
 * dynamic (Moonlight-generated) recipes — e.g. {@code supplementaries:recipes/sign_post_oak.json}
 * — are parsed on an async resource-generation thread during world load and call Forge's
 * {@link CraftingHelper#getCondition}. When the serializer is not present in {@code CraftingHelper}'s
 * condition map at that instant, parsing throws {@code Unknown condition type: supplementaries:flag}
 * and world entry crashes (verified in the 2026-09-03 crash reports; the failing frame is
 * {@code CraftingHelper.getCondition} on Moonlight's {@code ServerDynamicResourcesGenerator} thread).
 *
 * <p>At {@link FMLLoadCompleteEvent} (after all mod setup, before world-load resource generation)
 * this reflectively inspects the private {@code CraftingHelper.conditions} map and, ONLY if
 * {@code supplementaries:flag} is absent, installs a fallback serializer whose condition always
 * returns {@code true} (sign-post-style features are enabled by default). If Supplementaries/Moonlight
 * already registered it, {@code putIfAbsent} leaves the real one untouched — so this can neither
 * double-register nor override upstream behaviour. Only active when {@code supplementaries} is loaded.
 */
public final class SupplementariesFlagConditionCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation FLAG_ID =
            ResourceLocation.fromNamespaceAndPath("supplementaries", "flag");

    private SupplementariesFlagConditionCompat() {
    }

    @Mod.EventBusSubscriber(modid = ICECore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLoadComplete(FMLLoadCompleteEvent event) {
            if (ModList.get().isLoaded("supplementaries")) {
                event.enqueueWork(SupplementariesFlagConditionCompat::installIfMissing);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void installIfMissing() {
        try {
            Field field = CraftingHelper.class.getDeclaredField("conditions");
            field.setAccessible(true);
            Map<ResourceLocation, IConditionSerializer<?>> conditions =
                    (Map<ResourceLocation, IConditionSerializer<?>>) field.get(null);
            if (conditions.containsKey(FLAG_ID)) {
                return; // Upstream already registered it; do nothing.
            }
            conditions.putIfAbsent(FLAG_ID, new FlagSerializer());
            LOGGER.info("ICEcore installed a fallback '{}' recipe condition (upstream had not registered it)", FLAG_ID);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            // Never let a compat guard break loading; if reflection fails, upstream behaviour is unchanged.
            LOGGER.warn("ICEcore could not install the fallback supplementaries:flag condition", ex);
        }
    }

    /** Condition that ignores its flag and is always satisfied. */
    private record FlagCondition(String flag) implements ICondition {
        @Override
        public ResourceLocation getID() {
            return FLAG_ID;
        }

        @Override
        public boolean test(IContext context) {
            return true;
        }
    }

    private static final class FlagSerializer implements IConditionSerializer<FlagCondition> {
        @Override
        public void write(JsonObject json, FlagCondition value) {
            json.addProperty("flag", value.flag());
        }

        @Override
        public FlagCondition read(JsonObject json) {
            return new FlagCondition(json.has("flag") ? json.get("flag").getAsString() : "");
        }

        @Override
        public ResourceLocation getID() {
            return FLAG_ID;
        }
    }
}
