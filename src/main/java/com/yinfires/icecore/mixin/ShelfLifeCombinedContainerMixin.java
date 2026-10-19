package com.yinfires.icecore.mixin;

import com.yinfires.icecore.spoilage.PreservedContainer;
import net.minecraft.world.Container;
import net.blay09.mods.balm.api.container.CombinedContainer;
import org.spongepowered.asm.mixin.*;

@Mixin(value = CombinedContainer.class, remap = false)
public abstract class ShelfLifeCombinedContainerMixin implements PreservedContainer {
    @Shadow @Final private Container[] containers;
    @Override public void icecore$preserve(boolean preserve) {
        for (Container container : containers)
            if (container instanceof PreservedContainer access) access.icecore$preserve(preserve);
    }
    @Override public boolean icecore$preserved() {
        for (Container container : containers)
            if (!(container instanceof PreservedContainer access) || !access.icecore$preserved()) return false;
        return containers.length > 0;
    }
}
