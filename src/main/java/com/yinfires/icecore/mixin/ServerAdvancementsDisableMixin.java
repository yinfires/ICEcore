package com.yinfires.icecore.mixin;

import com.google.gson.JsonElement;
import com.yinfires.icecore.config.ICECoreConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementsDisableMixin {
    @Inject(method = "m_5787_(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void icecore$disableAdvancements(Map<ResourceLocation, JsonElement> definitions,
                                            ResourceManager resources, ProfilerFiller profiler,
                                            CallbackInfo ci) {
        if (ICECoreConfig.disableAllAdvancements()) {
            // prepare() supplies a mutable HashMap. Let vanilla rebuild its list and reload players.
            definitions.clear();
        }
    }
}
