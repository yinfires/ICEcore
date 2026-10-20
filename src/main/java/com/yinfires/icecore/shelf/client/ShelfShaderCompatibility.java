package com.yinfires.icecore.shelf.client;

import java.lang.invoke.*;

/** Shader packs may attach per-item metadata outside VertexConsumer. Keep their native path. */
final class ShelfShaderCompatibility {
    // Keep ItemRenderer.renderModelLists visible to the accelerator's own model cache,
    // compute transform and Iris grouping. Never bulk-write its custom consumers.
    private static final boolean ACCELERATOR=ShelfShaderCompatibility.class.getClassLoader().getResource("com/github/argon4w/acceleratedrendering/AcceleratedRenderingModEntry.class")!=null;
    static boolean acceleratedRenderingPresent() { return ACCELERATOR; }
    private static boolean failed;
    private static final MethodHandle ACTIVE=resolve();
    private static MethodHandle resolve() {
        try {
            Class<?> api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            var lookup=MethodHandles.publicLookup();
            Object instance=lookup.findStatic(api,"getInstance",MethodType.methodType(api)).invoke();
            return lookup.findVirtual(api,"isShaderPackInUse",MethodType.methodType(boolean.class)).bindTo(instance);
        } catch(ClassNotFoundException absent) { return null; }
        catch(Throwable incompatible) { failed=true;return null; }
    }
    static boolean nativeRequired() {
        if(failed) return true;
        if(ACTIVE==null) return false;
        try { return (boolean)ACTIVE.invokeExact(); }
        catch(Throwable incompatible) { failed=true;return true; }
    }
    private ShelfShaderCompatibility() {}
}
