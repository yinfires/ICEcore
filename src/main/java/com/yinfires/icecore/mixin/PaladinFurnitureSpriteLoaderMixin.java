package com.yinfires.icecore.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(SpriteResourceLoader.class)
public abstract class PaladinFurnitureSpriteLoaderMixin {
    private static final String PALETTED_SPRITE_SUPPLIER =
            "net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations$PalettedSpriteSupplier";

    @ModifyVariable(
            method = "m_260886_(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/List;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;"
            ),
            ordinal = 0
    )
    private Map<ResourceLocation, SpriteSource.SpriteSupplier> icecore$memoizePalettedSpritesForPaladinFurniture(
            Map<ResourceLocation, SpriteSource.SpriteSupplier> suppliers
    ) {
        if (!ModList.get().isLoaded("pfm")) {
            return suppliers;
        }

        suppliers.replaceAll((id, supplier) -> supplier.getClass().getName().equals(PALETTED_SPRITE_SUPPLIER)
                ? new MemoizedSpriteSupplier(supplier)
                : supplier);
        return suppliers;
    }

    private static final class MemoizedSpriteSupplier implements SpriteSource.SpriteSupplier {
        private final SpriteSource.SpriteSupplier delegate;
        private boolean resolved;
        private SpriteContents contents;

        private MemoizedSpriteSupplier(SpriteSource.SpriteSupplier delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized SpriteContents get() {
            if (!resolved) {
                contents = delegate.get();
                resolved = true;
            }
            return contents;
        }

        @Override
        public synchronized void discard() {
            if (!resolved) {
                delegate.discard();
                resolved = true;
            }
        }
    }
}
