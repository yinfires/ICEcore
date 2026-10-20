package com.yinfires.icecore.shelf.client;

import com.mojang.blaze3d.vertex.*;
import java.lang.invoke.*;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Optional Embeddium API; never unwrap or bypass a consumer's interception. */
final class ShelfBulkWriter {
    private static final Bridge BRIDGE=resolve();
    private static final Object RAW=new Object();
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
    // Resolve once per submission. Never retain a consumer across getBuffer/layer switches.
    static Object resolveWriter(VertexConsumer consumer) {
        if(BRIDGE!=null) {
            try { Object writer=(Object)BRIDGE.of.invokeExact(consumer);if(writer!=null) return writer; }
            catch(Throwable failure) { throw new IllegalStateException("Embeddium vertex writer lookup failed",failure); }
        }
        return consumer.getClass()==BufferBuilder.class?RAW:null;
    }
    static void writeResolved(VertexConsumer consumer,Object writer,ByteBuffer data,int count) {
        if(writer==RAW) { ((BufferBuilder)consumer).putBulkData(data);return; }
        try(MemoryStack stack=MemoryStack.stackPush()) {
            BRIDGE.push.invokeExact(writer,stack,MemoryUtil.memAddress(data),count,BRIDGE.format);
        } catch(Throwable failure) {
            // A failed push may have consumed vertices. Retrying would duplicate geometry.
            throw new IllegalStateException("Embeddium bulk vertex submission failed",failure);
        }
    }
    private ShelfBulkWriter() {}
}
