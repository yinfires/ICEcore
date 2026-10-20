package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.*;
import java.lang.invoke.*;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Optional Embeddium API; never unwrap or bypass a consumer's interception. */
final class ShelfBulkWriter {
    private static final Bridge BRIDGE=resolve();
    private record Bridge(MethodHandle of,MethodHandle push,Object format) {}
    private static Bridge resolve() {
        try {
            var lookup=MethodHandles.publicLookup();
            Class<?> writer=Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter");
            Class<?> registry=Class.forName("net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry");
            Class<?> description=Class.forName("net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription");
            Object instance=lookup.findStatic(registry,"instance",MethodType.methodType(registry)).invoke();
            Object format=lookup.findVirtual(registry,"get",MethodType.methodType(description,VertexFormat.class)).invoke(instance,DefaultVertexFormat.NEW_ENTITY);
            MethodHandle of=lookup.findStatic(writer,"tryOf",MethodType.methodType(writer,VertexConsumer.class))
                    .asType(MethodType.methodType(Object.class,VertexConsumer.class));
            MethodHandle push=lookup.findVirtual(writer,"push",MethodType.methodType(void.class,MemoryStack.class,long.class,int.class,description))
                    .asType(MethodType.methodType(void.class,Object.class,MemoryStack.class,long.class,int.class,Object.class));
            return new Bridge(of,push,format);
        } catch(ClassNotFoundException absent) { return null; }
        catch(Throwable incompatible) { return null; }
    }
    static boolean write(VertexConsumer consumer,ByteBuffer data,int count) {
        if(BRIDGE!=null) {
            final Object writer;
            try { writer=(Object)BRIDGE.of.invokeExact(consumer); }
            catch(Throwable incompatible) { throw new IllegalStateException("Embeddium vertex writer lookup failed",incompatible); }
            if(writer!=null) {
                try(MemoryStack stack=MemoryStack.stackPush()) {
                    BRIDGE.push.invokeExact(writer,stack,MemoryUtil.memAddress(data),count,BRIDGE.format);
                    return true;
                } catch(Throwable failure) {
                    // push may already have consumed vertices: retrying would duplicate geometry.
                    throw new IllegalStateException("Embeddium bulk vertex submission failed",failure);
                }
            }
        }
        if(consumer.getClass()==BufferBuilder.class) {
            ((BufferBuilder)consumer).putBulkData(data);return true;
        }
        ShelfRenderStats.reason(ShelfRenderStats.Reason.BULK_WRAPPER);
        return false;
    }
    static boolean supported(VertexConsumer consumer) {
        ShelfRenderStats.type("consumer",consumer);
        if(consumer.getClass()==BufferBuilder.class) return true;
        if(BRIDGE==null) return false;
        try { return (Object)BRIDGE.of.invokeExact(consumer)!=null; }
        catch(Throwable incompatible) { return false; }
    }
    private ShelfBulkWriter() {}
}
