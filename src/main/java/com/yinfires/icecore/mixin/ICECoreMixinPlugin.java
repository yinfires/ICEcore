package com.yinfires.icecore.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Prevents optional-mod mixins from loading when their target mod is absent. */
public final class ICECoreMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
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
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
