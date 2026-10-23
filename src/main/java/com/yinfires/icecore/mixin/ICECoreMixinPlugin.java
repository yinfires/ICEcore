package com.yinfires.icecore.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Prevents optional-mod mixins from loading when their target mod is absent. */
public final class ICECoreMixinPlugin implements IMixinConfigPlugin {
    private static boolean verifiedSortClass(String target) {
        try(var input=ICECoreMixinPlugin.class.getClassLoader().getResourceAsStream(target.replace('.','/')+".class")) {
            return input!=null && java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()))
                .equals("155209dc3a68097f9bcb33b4da6a530c6c26330a0f8d994642b79a29b1a83ffa");
        } catch(java.io.IOException|java.security.NoSuchAlgorithmException incompatible) { return false; }
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("WorkstationJuicerServerSoundMixin"))
            return net.minecraftforge.fml.loading.FMLLoader.getDist() == net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER
                    && present(targetClassName);
        if (mixinClassName.endsWith("EmbeddiumExactSortMixin")) return verifiedSortClass(targetClassName);
        if (mixinClassName.contains("WoodModernTooltip") || mixinClassName.contains("WoodJade") || mixinClassName.contains("ShelfJadeAccessor")) return present(targetClassName);
        if (mixinClassName.contains("RackDisplay")) return present(targetClassName)
                && !present("dev.xkmc.youkaishomecoming.util.BatchedItemRenderer");
        if (mixinClassName.contains("Spoiled")) return present(targetClassName);
        if (mixinClassName.contains("SmartKeyPromptsCombo")) return present(targetClassName);
        if (mixinClassName.contains("YuushyaJoinMessage") || mixinClassName.contains("SupplementariesPlaceableTooltip")) return present(targetClassName);
        if (mixinClassName.contains("Workstation")) return present(targetClassName);
        if (mixinClassName.contains("ItemDescriptionTooltip") || mixinClassName.contains("TeapotContentsTooltip")) return present(targetClassName);
        if (mixinClassName.contains("PonderItemTooltip") || mixinClassName.contains("JeiItemModNameTooltip")) return present(targetClassName);
        if (mixinClassName.contains("FoodTooltip")) return present(targetClassName);
        if (mixinClassName.contains("CookeryStockpot")) return present(targetClassName);
        if (mixinClassName.contains("GrillingOilPress") || mixinClassName.contains("GrillingCanolaOnly")) return present(targetClassName);
        if (mixinClassName.contains("GrillingSkewer")) return present(targetClassName);
        if (mixinClassName.contains("GrillingJade")) return present(targetClassName) && present("snownee.jade.api.ITooltip");
        if (mixinClassName.contains("GeckoLib")) return present(targetClassName);
        if (mixinClassName.contains("SeedBagCompatDewRandomTick")) return present("cool.bot.dewdropfarmland.Config");
        if (mixinClassName.contains("SeedBagCompat")) return present(targetClassName);
        if (mixinClassName.contains("DewDropIrrigationSprinkler"))
            return present("cool.bot.dewdropfarmland.Config") && present(targetClassName);
        if (mixinClassName.contains("DewDropIrrigation")) return present(targetClassName);
        if (mixinClassName.contains("Tavern")) return present("com.github.ysbbbbbb.kaleidoscopetavern.KaleidoscopeTavern");
        if (mixinClassName.contains("CozyCafe")) return present("io.github.chakyl.cozycafe.CozyCafe");
        if (mixinClassName.contains("SDMShop")) return present("net.sixik.sdmshoprework.SDMShopRework");
        if (mixinClassName.contains("Jei")) return present("mezz.jei.library.recipes.RecipeManagerInternal");
        return true;
    }
    private static boolean present(String name) {
        String resourceName = name.replace('.', '/') + ".class";
        return ICECoreMixinPlugin.class.getClassLoader().getResource(resourceName) != null;
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (mixinClassName.endsWith("WorkstationJuicerServerSoundMixin")) {
            com.yinfires.icecore.compat.fragrantorchard.JuicerServerClassPatch.apply(targetClass);
        }
    }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
